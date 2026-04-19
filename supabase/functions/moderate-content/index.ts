// Supabase Edge Function: Content moderation via Groq API
// API key is stored as a Supabase secret, never exposed to the client.
// No auth required - moderation is a public utility, the API key is what we protect.
// Rate limiting is IP-based to prevent abuse.

import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_MODEL = "llama-3.3-70b-versatile";

const rateLimitMap = new Map<string, { count: number; resetAt: number }>();
const MAX_REQUESTS_PER_MINUTE = 10;

function checkRateLimit(ip: string): boolean {
  const now = Date.now();
  const entry = rateLimitMap.get(ip);
  if (!entry || now > entry.resetAt) {
    rateLimitMap.set(ip, { count: 1, resetAt: now + 60_000 });
    return true;
  }
  if (entry.count >= MAX_REQUESTS_PER_MINUTE) {
    return false;
  }
  entry.count++;
  return true;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "authorization, content-type, apikey",
      },
    });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
  }

  try {
    const clientIp = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";
    if (!checkRateLimit(clientIp)) {
      return new Response(
        JSON.stringify({ error: "Rate limit exceeded. Max 10 requests per minute." }),
        { status: 429 }
      );
    }

    const body = await req.json();
    const text: string = body.text;
    const lang: string = body.lang || "tr";

    const langNames: Record<string, string> = {
      "tr": "Turkish",
      "en": "English",
      "de": "German",
      "fr": "French",
      "es": "Spanish",
      "pt": "Portuguese",
      "ru": "Russian",
      "uk": "Ukrainian",
      "ar": "Arabic"
    };
    const targetLangName = langNames[lang] || "Turkish";

    if (!text || text.trim().length === 0) {
      return new Response(
        JSON.stringify({ is_safe: true }),
        { headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" } }
      );
    }

    const groqApiKey = Deno.env.get("GROQ_API_KEY");
    if (!groqApiKey) {
      return new Response(JSON.stringify({ error: "GROQ_API_KEY not configured", is_safe: true }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const groqResponse = await fetch(GROQ_API_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${groqApiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: GROQ_MODEL,
        messages: [
          {
            role: "system",
            content: `You are a content moderator. Analyze the text for profanity, hate speech, or offensive content.
                     Respond ONLY as a JSON object in this format: {"is_safe": true} or {"is_safe": false, "reason": "..."}.
                     If is_safe is false, the "reason" MUST be written in ${targetLangName}.`,
          },
          { role: "user", content: text },
        ],
        response_format: { type: "json_object" },
        temperature: 0,
      }),
    });

    if (!groqResponse.ok) {
      const errorBody = await groqResponse.text();
      console.error(`Groq API error: ${groqResponse.status} - ${errorBody}`);
      // Fallback: allow content if moderation service is down
      return new Response(JSON.stringify({ is_safe: true, reason: "Moderation unavailable" }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const groqData = await groqResponse.json();
    const aiContent = groqData.choices?.[0]?.message?.content;

    if (!aiContent) {
      return new Response(JSON.stringify({ is_safe: true, reason: "Empty AI response" }), {
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const result = JSON.parse(aiContent);

    return new Response(JSON.stringify(result), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  } catch (err) {
    console.error("Edge function error:", err);
    // Fallback: allow content if edge function has an error
    return new Response(JSON.stringify({ is_safe: true, reason: "Moderation error" }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  }
});