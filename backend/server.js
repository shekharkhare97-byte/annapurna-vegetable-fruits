"use strict";

const http = require("node:http");

const PORT = Number(process.env.PORT || 8080);
const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

const ALLOWED_CHAT_MODELS = new Set([
  "gemini-3.1-flash-lite",
  "gemini-3.8-flash",
  "gemini-4-argon",
]);

const ALLOWED_IMAGE_SIZES = new Set(["1K", "2K", "4K"]);
const ALLOWED_ASPECT_RATIOS = new Set(["1:1", "4:3", "16:9", "3:4", "9:16"]);
const IMAGE_MODEL = "gemini-3-pro-image-preview";

// Simple per-IP sliding window rate limiter (30 requests / minute)
const rateBuckets = new Map();
const WINDOW_MS = 60_000;
const MAX_REQ_PER_WINDOW = 30;

function checkRateLimit(ip) {
  const now = Date.now();
  const entry = rateBuckets.get(ip) || { count: 0, resetAt: now + WINDOW_MS };
  if (now > entry.resetAt) {
    entry.count = 0;
    entry.resetAt = now + WINDOW_MS;
  }
  entry.count += 1;
  rateBuckets.set(ip, entry);
  return entry.count <= MAX_REQ_PER_WINDOW;
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "no-store",
  });
  res.end(body);
}

function readJsonBody(req, maxBytes = 262_144) {
  return new Promise((resolve, reject) => {
    let total = 0;
    const chunks = [];
    req.on("data", (chunk) => {
      total += chunk.length;
      if (total > maxBytes) {
        req.destroy();
        reject(new Error("Payload too large"));
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => {
      try {
        const raw = Buffer.concat(chunks).toString("utf8");
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

async function handleChat(req, res, serverApiKey) {
  const body = await readJsonBody(req);
  const model = ALLOWED_CHAT_MODELS.has(body.model) ? body.model : "gemini-3.8-flash";
  const systemInstruction = typeof body.systemInstruction === "string" ? body.systemInstruction.slice(0, 8000) : "";
  const rawMessages = Array.isArray(body.messages) ? body.messages : [];

  if (rawMessages.length === 0) {
    return sendJson(res, 400, { error: "messages array is required" });
  }

  const contents = rawMessages.slice(-24).map((m) => ({
    role: m.role === "model" ? "model" : "user",
    parts: [{ text: String(m.text || "").slice(0, 4000) }],
  }));

  const upstreamPayload = {
    contents,
    generationConfig: { temperature: 0.7 },
  };
  if (systemInstruction) {
    upstreamPayload.systemInstruction = {
      parts: [{ text: systemInstruction }],
    };
  }

  const upstreamRes = await fetch(`${GEMINI_BASE_URL}/${encodeURIComponent(model)}:generateContent`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": serverApiKey,
    },
    body: JSON.stringify(upstreamPayload),
  });

  const data = await upstreamRes.json();
  if (!upstreamRes.ok) {
    const msg = data?.error?.message || `Upstream error (${upstreamRes.status})`;
    return sendJson(res, upstreamRes.status, { error: msg });
  }

  const parts = data?.candidates?.[0]?.content?.parts || [];
  const reply = parts
    .map((p) => p.text || "")
    .join("")
    .trim();

  return sendJson(res, 200, { reply, modelUsed: model });
}

async function handleImageGeneration(req, res, serverApiKey) {
  const body = await readJsonBody(req);
  const prompt = typeof body.prompt === "string" ? body.prompt.trim().slice(0, 2000) : "";
  if (!prompt) {
    return sendJson(res, 400, { error: "prompt is required" });
  }

  const imageSize = ALLOWED_IMAGE_SIZES.has(body.imageSize) ? body.imageSize : "1K";
  const aspectRatio = ALLOWED_ASPECT_RATIOS.has(body.aspectRatio) ? body.aspectRatio : "1:1";

  const upstreamPayload = {
    contents: [
      {
        role: "user",
        parts: [{ text: prompt }],
      },
    ],
    generationConfig: {
      responseModalities: ["TEXT", "IMAGE"],
      imageConfig: {
        aspectRatio,
        imageSize,
      },
    },
  };

  const upstreamRes = await fetch(`${GEMINI_BASE_URL}/${IMAGE_MODEL}:generateContent`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": serverApiKey,
    },
    body: JSON.stringify(upstreamPayload),
  });

  const data = await upstreamRes.json();
  if (!upstreamRes.ok) {
    const msg = data?.error?.message || `Upstream error (${upstreamRes.status})`;
    return sendJson(res, upstreamRes.status, { error: msg });
  }

  const parts = data?.candidates?.[0]?.content?.parts || [];
  let imageBase64 = null;
  let mimeType = "image/png";
  const captions = [];

  for (const part of parts) {
    if (part.text) captions.push(part.text);
    const inline = part.inlineData || part.inline_data;
    if (inline?.data && !imageBase64) {
      imageBase64 = inline.data;
      mimeType = inline.mimeType || inline.mime_type || "image/png";
    }
  }

  if (!imageBase64) {
    return sendJson(res, 502, {
      error: captions.join("\n").trim() || "No image returned by upstream model",
    });
  }

  return sendJson(res, 200, {
    imageBase64,
    mimeType,
    captionText: captions.join("\n").trim() || null,
  });
}

const server = http.createServer(async (req, res) => {
  const clientIp =
    (req.headers["x-forwarded-for"] || "").toString().split(",")[0].trim() ||
    req.socket.remoteAddress ||
    "unknown";

  if (!checkRateLimit(clientIp)) {
    return sendJson(res, 429, { error: "Too many requests. Please wait a minute and try again." });
  }

  if (req.method === "GET" && req.url === "/api/health") {
    return sendJson(res, 200, { status: "ok", service: "annapurna-secure-ai-backend" });
  }

  // Server-side secret read strictly from server environment / Secret Manager
  const serverApiKey = (process.env.GEMINI_SERVER_API_KEY || "").trim();
  if (!serverApiKey) {
    return sendJson(res, 503, {
      error: "Server secret GEMINI_SERVER_API_KEY is not configured on the backend.",
    });
  }

  try {
    if (req.method === "POST" && req.url === "/api/chat") {
      return await handleChat(req, res, serverApiKey);
    }
    if (req.method === "POST" && req.url === "/api/generate-image") {
      return await handleImageGeneration(req, res, serverApiKey);
    }
    return sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    return sendJson(res, 500, { error: err.message || "Internal server error" });
  }
});

server.listen(PORT, () => {
  console.log(`Annapurna secure backend listening on port ${PORT}`);
});
