// Supabase Edge Function: Vehicle image analysis via OpenAI API
// API key is stored as a Supabase secret, never exposed to the client.
// No auth required - analysis is a utility, the API key is what we protect.
// Rate limiting is IP-based to prevent abuse.

import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
const OPENAI_MODEL = "gpt-4o-mini";

const rateLimitMap = new Map<string, { count: number; resetAt: number }>();
const MAX_REQUESTS_PER_MINUTE = 5;

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

const VEHICLE_PROMPT = `Sen bir profesyonel diecast (Hot Wheels, Matchbox, MiniGT vb.) koleksiyon uzmanisin.
Gorseldeki araci ve paketi analiz et. Sunlari tespit et:
1. Marka (Hot Wheels, Matchbox, Majorette, MiniGT vb.)
2. Model tam adi (Orn: '1994 Toyota Supra')
3. Serisi (Orn: 'HW Then and Now' veya 'Mainline')
4. Varsa paket uzerindeki kodu (Orn: 'HCT04')
5. Paket Kondisyonu: Su degerlerden BIRINI sec: 'LOOSE' (kutusuz/acik), 'MINT' (kusursuz kutu), 'NEAR_MINT' (cok hafif yipranmis kutu), 'DAMAGED' (hasarli/yirtik/ezik kutu)
6. Kondisyon notu (Orn: 'Sol alt kosede hafif bukulme var')
7. Tahmini piyasa degeri: eBay uzerindeki guncel 'Sold/Completed' (Satilmis) ilanlarini baz alarak, aracin ve kutunun kondisyonuna gore dolar (USD) cinsinden sayisal bir deger ver. (Orn: 14.50)

Yaniti SADECE su JSON formatinda ver, baska metin ekleme:
{
  "brand": "...",
  "model": "...",
  "series": "...",
  "year": "...",
  "condition": "LOOSE | MINT | NEAR_MINT | DAMAGED",
  "conditionNote": "...",
  "estimatedValue": 15.5
}`;

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
        JSON.stringify({ error: "Rate limit exceeded. Max 5 requests per minute." }),
        { status: 429 }
      );
    }

    const body = await req.json();
    const base64Image: string = body.base64_image;

    if (!base64Image || base64Image.trim().length === 0) {
      return new Response(JSON.stringify({ error: "Missing base64_image" }), { status: 400 });
    }

    const openaiApiKey = Deno.env.get("OPENAI_API_KEY");
    if (!openaiApiKey) {
      return new Response(JSON.stringify({ error: "OPENAI_API_KEY not configured" }), {
        status: 500,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const openaiResponse = await fetch(OPENAI_API_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openaiApiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: OPENAI_MODEL,
        messages: [
          {
            role: "user",
            content: [
              { type: "text", text: VEHICLE_PROMPT },
              {
                type: "image_url",
                image_url: { url: `data:image/jpeg;base64,${base64Image}` },
              },
            ],
          },
        ],
        max_tokens: 1000,
        response_format: { type: "json_object" },
      }),
    });

    if (!openaiResponse.ok) {
      const errorBody = await openaiResponse.text();
      console.error(`OpenAI API error: ${openaiResponse.status} - ${errorBody}`);
      return new Response(JSON.stringify({ error: "Vision service error" }), {
        status: 502,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const openaiData = await openaiResponse.json();
    const aiContent = openaiData.choices?.[0]?.message?.content;

    if (!aiContent) {
      return new Response(JSON.stringify({ error: "Empty AI response" }), {
        status: 502,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    const result = JSON.parse(aiContent);

    return new Response(JSON.stringify(result), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  } catch (err) {
    console.error("Edge function error:", err);
    return new Response(JSON.stringify({ error: "Internal server error" }), {
      status: 500,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    });
  }
});