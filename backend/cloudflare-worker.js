export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff"
    };

    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
    const json = (data, status = 200) => new Response(JSON.stringify(data), {
      status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
    });

    if (request.method === "GET" && url.pathname === "/api/health") {
      return json({ status: "ok", service: "annapurna-customer-ai-worker", mode: "free-chat" });
    }

    if (request.method !== "POST" || url.pathname !== "/api/chat") {
      if (url.pathname === "/api/generate-image") {
        return json({ error: "AI image generation is not included in the free-only setup. Chat and recipe help are available." }, 501);
      }
      return json({ error: "Not found" }, 404);
    }

    const apiKey = (env.GEMINI_SERVER_API_KEY || "").trim();
    if (!apiKey) return json({ error: "Server secret GEMINI_SERVER_API_KEY is not configured." }, 503);

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 256 * 1024) return json({ error: "Payload too large." }, 413);
      body = JSON.parse(raw || "{}");
    } catch {
      return json({ error: "Invalid JSON body." }, 400);
    }

    if (!Array.isArray(body.messages) || body.messages.length === 0) {
      return json({ error: "messages array is required." }, 400);
    }

    // Restrict requests to models listed on Google's Gemini API Free Tier.
    const allowedModels = new Set(["gemini-3.8-flash", "gemini-3.1-flash-lite"]);
    const model = allowedModels.has(body.model) ? body.model : "gemini-3.8-flash";
    const systemInstruction = typeof body.systemInstruction === "string"
      ? body.systemInstruction.slice(0, 8000) : "";
    const contents = body.messages.slice(-24).map((m) => ({
      role: m.role === "model" ? "model" : "user",
      parts: [{ text: String(m.text || "").slice(0, 4000) }]
    }));

    try {
      const upstream = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
          body: JSON.stringify({
            contents,
            ...(systemInstruction ? { systemInstruction: { parts: [{ text: systemInstruction }] } } : {}),
            generationConfig: { temperature: 0.7 }
          })
        }
      );
      const data = await upstream.json();
      if (!upstream.ok) {
        return json({ error: data?.error?.message || `Gemini API error (${upstream.status})` }, upstream.status);
      }
      const parts = data?.candidates?.[0]?.content?.parts || [];
      const reply = parts.map((part) => part.text || "").join("").trim();
      if (!reply) return json({ error: "Gemini returned an empty response." }, 502);
      return json({ reply, modelUsed: model });
    } catch {
      return json({ error: "Could not reach Gemini API. Please try again." }, 502);
    }
  }
};
