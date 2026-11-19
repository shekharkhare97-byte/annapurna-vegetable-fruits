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
      return json({ status: "ok", service: "annapurna-customer-ai-worker", mode: "free-chat-vision" });
    }

    if (request.method !== "POST" || url.pathname !== "/api/chat") {
      if (url.pathname === "/api/generate-image") {
        return json({ error: "AI image generation is not included in the free-only setup. Chat and photo understanding are available." }, 501);
      }
      return json({ error: "Not found" }, 404);
    }

    const apiKey = (env.GEMINI_SERVER_API_KEY || "").trim();
    if (!apiKey) return json({ error: "Server secret GEMINI_SERVER_API_KEY is not configured." }, 503);

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 6 * 1024 * 1024) return json({ error: "Payload too large. Please choose a smaller photo." }, 413);
      body = JSON.parse(raw || "{}");
    } catch {
      return json({ error: "Invalid JSON body." }, 400);
    }

    if (!Array.isArray(body.messages) || body.messages.length === 0) {
      return json({ error: "messages array is required." }, 400);
    }

    const allowedModels = ["gemini-3.8-flash", "gemini-3.1-flash-lite"];
    const requestedModel = allowedModels.includes(body.model) ? body.model : allowedModels[0];
    const modelOrder = [requestedModel, ...allowedModels.filter((item) => item !== requestedModel)];
    const systemInstruction = typeof body.systemInstruction === "string"
      ? body.systemInstruction.slice(0, 8000) : "";
    if (body.messages.some((m) => typeof m.imageBase64 === "string" && m.imageBase64.length > 5 * 1024 * 1024)) {\n      return json({ error: "Image too large. Please choose a smaller photo." }, 413);\n    }\n    const contents = body.messages.slice(-24).map((m) => {
      const parts = [{ text: String(m.text || "").slice(0, 4000) }];
      if (typeof m.imageBase64 === "string" && m.imageBase64.length > 0) {
        const cleanBase64 = m.imageBase64.replace(/^data:[^;]+;base64,/, "");
        if (cleanBase64.length > 5 * 1024 * 1024) throw new Error("Image too large.");
        const mimeType = ["image/jpeg", "image/png", "image/webp"].includes(m.mimeType)
          ? m.mimeType : "image/jpeg";
        parts.push({ inlineData: { mimeType, data: cleanBase64 } });
      }
      return { role: m.role === "model" ? "model" : "user", parts };
    });

    const requestBody = JSON.stringify({
      contents,
      ...(systemInstruction ? { systemInstruction: { parts: [{ text: systemInstruction }] } } : {}),
      generationConfig: { temperature: 0.7 }
    });

    let lastStatus = 502;
    let lastMessage = "Gemini is temporarily unavailable. Please try again shortly.";
    for (const model of modelOrder) {
      try {
        const upstream = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
            body: requestBody
          }
        );
        const data = await upstream.json().catch(() => ({}));
        if (upstream.ok) {
          const parts = data?.candidates?.[0]?.content?.parts || [];
          const reply = parts.map((part) => part.text || "").join("").trim();
          if (reply) return json({ reply, modelUsed: model });
          lastStatus = 502;
          lastMessage = "Gemini returned an empty response.";
        } else {
          lastStatus = upstream.status;
          lastMessage = data?.error?.message || `Gemini API error (${upstream.status})`;
        }
        if (![429, 500, 502, 503, 504].includes(lastStatus)) {
          return json({ error: lastMessage }, lastStatus);
        }
      } catch (error) {
        lastStatus = 400;
        lastMessage = error?.message === "Image too large." ? error.message : "Could not reach Gemini API. Please try again.";
        if (lastStatus === 400) return json({ error: lastMessage }, lastStatus);
      }
    }

    return json({
      error: lastMessage,
      message: "Both free AI model options are temporarily busy or unavailable. Please wait a little and try again."
    }, lastStatus);
  }
};
