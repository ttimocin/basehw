/**
 * Supabase Edge Function: notify_feedback
 *
 * Called by a **Database Webhook** on `public.feedback_messages` INSERT.
 * Sends one email per new row via **Resend** (https://resend.com).
 *
 * --- Required secrets (Dashboard → Edge Functions → Secrets) ---
 *   RESEND_API_KEY          Resend API key (re_...)
 *   FEEDBACK_NOTIFY_TO      Comma-separated inbox(es), e.g. you@domain.com
 *   FEEDBACK_WEBHOOK_SECRET Long random string; must match webhook header (see below)
 *
 * --- Optional ---
 *   FEEDBACK_NOTIFY_FROM    Default: "BaseHW <onboarding@resend.dev>"
 *
 * --- Deploy ---
 *   supabase functions deploy notify_feedback
 *
 * --- Database Webhook (Dashboard → Integrations → Database Webhooks) ---
 *   Table: feedback_messages | Event: INSERT
 *   URL:   https://<PROJECT_REF>.supabase.co/functions/v1/notify_feedback
 *   HTTP header: x-webhook-secret: <same value as FEEDBACK_WEBHOOK_SECRET>
 *   (Authorization: Bearer <secret> is also accepted.)
 */

import "jsr:@supabase/functions-js/edge-runtime.d.ts";

type FeedbackRecord = {
  id?: string;
  firebase_uid?: string;
  username?: string;
  subject?: string;
  message?: string;
  created_at?: string;
};

type InsertPayload = {
  type: "INSERT";
  table: string;
  schema: string;
  record: FeedbackRecord;
  old_record: null;
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function verifyWebhookSecret(req: Request): boolean {
  const expected = Deno.env.get("FEEDBACK_WEBHOOK_SECRET");
  if (!expected) {
    console.warn("notify_feedback: FEEDBACK_WEBHOOK_SECRET is not set; refusing request.");
    return false;
  }
  const header = req.headers.get("x-webhook-secret");
  if (header === expected) return true;
  const auth = req.headers.get("Authorization") ?? "";
  const m = auth.match(/^Bearer\s+(.+)$/i);
  if (m && m[1] === expected) return true;
  return false;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { status: 204 });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  if (!verifyWebhookSecret(req)) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  const resendKey = Deno.env.get("RESEND_API_KEY");
  const notifyTo = Deno.env.get("FEEDBACK_NOTIFY_TO")?.trim();
  const notifyFrom = Deno.env.get("FEEDBACK_NOTIFY_FROM")?.trim() ||
    "BaseHW <onboarding@resend.dev>";

  if (!resendKey || !notifyTo) {
    console.error("notify_feedback: RESEND_API_KEY or FEEDBACK_NOTIFY_TO missing.");
    return jsonResponse({ error: "Email not configured" }, 503);
  }

  let payload: InsertPayload | null = null;
  try {
    payload = (await req.json()) as InsertPayload;
  } catch {
    return jsonResponse({ error: "Invalid JSON" }, 400);
  }

  if (!payload || payload.type !== "INSERT" || payload.table !== "feedback_messages" || !payload.record) {
    return jsonResponse({ ok: true, skipped: true, reason: "not_feedback_insert" });
  }

  const r = payload.record;
  const subjectLine = `[BaseHW feedback] ${r.subject ?? "(no subject)"}`;
  const textBody = [
    `New row in feedback_messages`,
    ``,
    `id: ${r.id ?? ""}`,
    `created_at: ${r.created_at ?? ""}`,
    `firebase_uid: ${r.firebase_uid ?? ""}`,
    `username: ${r.username ?? ""}`,
    `subject: ${r.subject ?? ""}`,
    ``,
    `message:`,
    r.message ?? "",
  ].join("\n");

  const htmlBody = `
  <h2>New feedback_messages row</h2>
  <table style="border-collapse:collapse;font-family:sans-serif;font-size:14px">
    <tr><td style="padding:4px 8px;border:1px solid #ccc"><b>id</b></td><td style="padding:4px 8px;border:1px solid #ccc">${escapeHtml(String(r.id ?? ""))}</td></tr>
    <tr><td style="padding:4px 8px;border:1px solid #ccc"><b>created_at</b></td><td style="padding:4px 8px;border:1px solid #ccc">${escapeHtml(String(r.created_at ?? ""))}</td></tr>
    <tr><td style="padding:4px 8px;border:1px solid #ccc"><b>firebase_uid</b></td><td style="padding:4px 8px;border:1px solid #ccc">${escapeHtml(String(r.firebase_uid ?? ""))}</td></tr>
    <tr><td style="padding:4px 8px;border:1px solid #ccc"><b>username</b></td><td style="padding:4px 8px;border:1px solid #ccc">${escapeHtml(String(r.username ?? ""))}</td></tr>
    <tr><td style="padding:4px 8px;border:1px solid #ccc"><b>subject</b></td><td style="padding:4px 8px;border:1px solid #ccc">${escapeHtml(String(r.subject ?? ""))}</td></tr>
  </table>
  <h3>Message</h3>
  <pre style="white-space:pre-wrap;background:#f5f5f5;padding:12px;border-radius:8px">${escapeHtml(String(r.message ?? ""))}</pre>
  `;

  const resendRes = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${resendKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: notifyFrom,
      to: notifyTo.split(",").map((e) => e.trim()).filter(Boolean),
      subject: subjectLine,
      text: textBody,
      html: htmlBody,
    }),
  });

  const resendText = await resendRes.text();
  if (!resendRes.ok) {
    console.error("Resend error:", resendRes.status, resendText);
    return jsonResponse({ error: "Resend failed", detail: resendText }, 502);
  }

  let resendJson: unknown = resendText;
  try {
    resendJson = resendText ? JSON.parse(resendText) : null;
  } catch {
    resendJson = { raw: resendText };
  }

  return jsonResponse({ ok: true, resend: resendJson });
});
