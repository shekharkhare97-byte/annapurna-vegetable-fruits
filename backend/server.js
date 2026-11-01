"use strict";

const http = require("node:http");
const { timingSafeEqual } = require("node:crypto");

const PORT = Number(process.env.PORT || 8080);
const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
const ALLOWED_CHAT_MODELS = new Set(["gemini-3.1-flash-lite", "gemini-3.8-flash", "gemini-4-argon"]);
const ALLOWED_IMAGE_SIZES = new Set(["1K", "2K", "4K"]);
const ALLOWED_ASPECT_RATIOS = new Set(["1:1", "4:3", "16:9", "3:4", "9:16"]);
const IMAGE_MODEL = "gemini-3-pro-image-preview";
const rateBuckets = new Map();
const WINDOW_MS = 60_000;
const MAX_REQ_PER_WINDOW = 60;

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "no-store",
  });
  res.end(JSON.stringify(payload));
}

function checkRateLimit(ip) {
  const now = Date.now();
  const entry = rateBuckets.get(ip) || { count: 0, resetAt: now + WINDOW_MS };
  if (now > entry.resetAt) { entry.count = 0; entry.resetAt = now + WINDOW_MS; }
  entry.count += 1;
  rateBuckets.set(ip, entry);
  return entry.count <= MAX_REQ_PER_WINDOW;
}

function readJsonBody(req, maxBytes = 262_144) {
  return new Promise((resolve, reject) => {
    let total = 0;
    const chunks = [];
    req.on("data", (chunk) => {
      total += chunk.length;
      if (total > maxBytes) { reject(new Error("Payload too large")); req.destroy(); return; }
      chunks.push(chunk);
    });
    req.on("end", () => {
      try { resolve(chunks.length ? JSON.parse(Buffer.concat(chunks).toString("utf8")) : {}); }
      catch { reject(new Error("Invalid JSON body")); }
    });
    req.on("error", reject);
  });
}

function ownerAuthorized(req) {
  const expected = (process.env.ANNAPURNA_OWNER_TOKEN || "").trim();
  const supplied = (req.headers.authorization || "").replace(/^Bearer\s+/i, "").trim();
  if (!expected || !supplied) return false;
  const a = Buffer.from(expected);
  const b = Buffer.from(supplied);
  return a.length === b.length && timingSafeEqual(a, b);
}

function fsValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number" && Number.isInteger(value)) return { integerValue: String(value) };
  return { stringValue: String(value) };
}

function decodeFsValue(value) {
  if (!value) return null;
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("timestampValue" in value) return value.timestampValue;
  if ("nullValue" in value) return null;
  return null;
}

function decodeDocument(doc) {
  const result = { id: doc.name.split("/").pop() };
  for (const [key, value] of Object.entries(doc.fields || {})) result[key] = decodeFsValue(value);
  return result;
}

async function firestoreRequest(path, options = {}) {
  const project = (process.env.GOOGLE_CLOUD_PROJECT || process.env.GCLOUD_PROJECT || "").trim();
  if (!project) throw new Error("GOOGLE_CLOUD_PROJECT is not configured");
  const tokenResponse = await fetch("http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token", {
    headers: { "Metadata-Flavor": "Google" },
  });
  if (!tokenResponse.ok) throw new Error("Could not obtain Cloud Run service identity token");
  const tokenData = await tokenResponse.json();
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${encodeURIComponent(project)}/databases/(default)/documents/${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${tokenData.access_token}`, "Content-Type": "application/json", ...(options.headers || {}) },
  });
  const raw = await response.text();
  let data = {};
  try { data = raw ? JSON.parse(raw) : {}; } catch { data = {}; }
  if (!response.ok) {
    const message = data?.error?.message || `Firestore request failed (${response.status})`;
    const error = new Error(message);
    error.statusCode = response.status;
    throw error;
  }
  return data;
}

async function listShopProducts() {
  const data = await firestoreRequest("shopProducts?pageSize=500");
  return (data.documents || []).map(decodeDocument);
}

async function listShopOrders() {
  const data = await firestoreRequest("shopOrders?pageSize=100&orderBy=createdAtMillis%20desc");
  return (data.documents || []).map(decodeDocument);
}

async function saveShopProduct(productId, body) {
  const fields = {
    productId: fsValue(productId),
    name: fsValue(String(body.name || "").slice(0, 100)),
    stockGrams: fsValue(Math.max(0, Math.floor(Number(body.stockGrams) || 0))),
    sellingPricePaisePerKg: fsValue(Math.max(0, Math.floor(Number(body.sellingPricePaisePerKg) || 0))),
    updatedAtMillis: fsValue(Date.now()),
  };
  return firestoreRequest(`shopProducts/${encodeURIComponent(productId)}`, {
    method: "PATCH", body: JSON.stringify({ fields }),
  });
}

async function createShopOrder(body) {
  const name = String(body.customerName || "").trim().slice(0, 100);
  const mobile = String(body.mobileNumber || "").trim().slice(0, 20);
  const address = String(body.deliveryAddress || "").trim().slice(0, 500);
  if (!name || !mobile || !address || !Array.isArray(body.items) || body.items.length < 1 || body.items.length > 50) {
    const error = new Error("Customer name, mobile, address and order items are required");
    error.statusCode = 400;
    throw error;
  }
  const items = body.items.map((item) => ({
    productId: String(item.productId || "").slice(0, 80),
    productName: String(item.productName || "").slice(0, 100),
    quantityGrams: Math.max(0, Math.floor(Number(item.quantityGrams) || 0)),
    unitPricePaisePerKg: Math.max(0, Math.floor(Number(item.unitPricePaisePerKg) || 0)),
  }));
  if (items.some((item) => !item.productId || item.quantityGrams <= 0)) {
    const error = new Error("Each order item needs a product and positive quantity in grams");
    error.statusCode = 400;
    throw error;
  }
  const totalAmountPaise = Math.max(0, Math.floor(Number(body.totalAmountPaise) || 0));
  const fields = {
    customerName: fsValue(name), mobileNumber: fsValue(mobile), deliveryAddress: fsValue(address),
    itemsJson: fsValue(JSON.stringify(items)), totalAmountPaise: fsValue(totalAmountPaise),
    status: fsValue("NEW"), createdAtMillis: fsValue(Date.now()),
  };
  const saved = await firestoreRequest("shopOrders", { method: "POST", body: JSON.stringify({ fields }) });
  return decodeDocument(saved);
}

async function updateShopOrder(orderId, body) {
  const status = String(body.status || "");
  if (!new Set(["ACCEPTED", "REJECTED", "COMPLETED"]).has(status)) {
    const error = new Error("Order status must be ACCEPTED, REJECTED or COMPLETED");
    error.statusCode = 400;
    throw error;
  }
  const fields = { status: fsValue(status), updatedAtMillis: fsValue(Date.now()) };
  const saved = await firestoreRequest(`shopOrders/${encodeURIComponent(orderId)}?updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAtMillis`, {
    method: "PATCH", body: JSON.stringify({ fields }),
  });
  return decodeDocument(saved);
}

async function handleChat(req, res, serverApiKey) {
  const body = await readJsonBody(req);
  const model = ALLOWED_CHAT_MODELS.has(body.model) ? body.model : "gemini-3.8-flash";
  const systemInstruction = typeof body.systemInstruction === "string" ? body.systemInstruction.slice(0, 8000) : "";
  const rawMessages = Array.isArray(body.messages) ? body.messages : [];
  if (!rawMessages.length) return sendJson(res, 400, { error: "messages array is required" });
  const contents = rawMessages.slice(-24).map((m) => ({ role: m.role === "model" ? "model" : "user", parts: [{ text: String(m.text || "").slice(0, 4000) }] }));
  const upstreamPayload = { contents, generationConfig: { temperature: 0.7 } };
  if (systemInstruction) upstreamPayload.systemInstruction = { parts: [{ text: systemInstruction }] };
  const upstreamRes = await fetch(`${GEMINI_BASE_URL}/${encodeURIComponent(model)}:generateContent`, {
    method: "POST", headers: { "Content-Type": "application/json", "x-goog-api-key": serverApiKey }, body: JSON.stringify(upstreamPayload),
  });
  const data = await upstreamRes.json();
  if (!upstreamRes.ok) return sendJson(res, upstreamRes.status, { error: data?.error?.message || `Upstream error (${upstreamRes.status})` });
  const parts = data?.candidates?.[0]?.content?.parts || [];
  return sendJson(res, 200, { reply: parts.map((p) => p.text || "").join("").trim(), modelUsed: model });
}

async function handleImageGeneration(req, res, serverApiKey) {
  const body = await readJsonBody(req);
  const prompt = typeof body.prompt === "string" ? body.prompt.trim().slice(0, 2000) : "";
  if (!prompt) return sendJson(res, 400, { error: "prompt is required" });
  const imageSize = ALLOWED_IMAGE_SIZES.has(body.imageSize) ? body.imageSize : "1K";
  const aspectRatio = ALLOWED_ASPECT_RATIOS.has(body.aspectRatio) ? body.aspectRatio : "1:1";
  const upstreamPayload = { contents: [{ role: "user", parts: [{ text: prompt }] }], generationConfig: { responseModalities: ["TEXT", "IMAGE"], imageConfig: { aspectRatio, imageSize } } };
  const upstreamRes = await fetch(`${GEMINI_BASE_URL}/${IMAGE_MODEL}:generateContent`, {
    method: "POST", headers: { "Content-Type": "application/json", "x-goog-api-key": serverApiKey }, body: JSON.stringify(upstreamPayload),
  });
  const data = await upstreamRes.json();
  if (!upstreamRes.ok) return sendJson(res, upstreamRes.status, { error: data?.error?.message || `Upstream error (${upstreamRes.status})` });
  const parts = data?.candidates?.[0]?.content?.parts || [];
  let imageBase64 = null;
  let mimeType = "image/png";
  const captions = [];
  for (const part of parts) {
    if (part.text) captions.push(part.text);
    const inline = part.inlineData || part.inline_data;
    if (inline?.data && !imageBase64) { imageBase64 = inline.data; mimeType = inline.mimeType || inline.mime_type || "image/png"; }
  }
  if (!imageBase64) return sendJson(res, 502, { error: captions.join("\n").trim() || "No image returned by upstream model" });
  return sendJson(res, 200, { imageBase64, mimeType, captionText: captions.join("\n").trim() || null });
}

const server = http.createServer(async (req, res) => {
  const ip = (req.headers["x-forwarded-for"] || "").toString().split(",")[0].trim() || req.socket.remoteAddress || "unknown";
  if (!checkRateLimit(ip)) return sendJson(res, 429, { error: "Too many requests. Please wait a minute and try again." });
  if (req.method === "GET" && req.url === "/api/health") return sendJson(res, 200, { status: "ok", service: "annapurna-secure-backend" });

  try {
    if (req.method === "GET" && req.url === "/api/shop/products") {
      return sendJson(res, 200, { products: await listShopProducts() });
    }
    if (req.method === "POST" && req.url === "/api/shop/orders") {
      return sendJson(res, 201, { order: await createShopOrder(await readJsonBody(req)) });
    }
    if (req.method === "GET" && req.url === "/api/shop/orders") {
      if (!ownerAuthorized(req)) return sendJson(res, 401, { error: "Owner authorization required" });
      return sendJson(res, 200, { orders: await listShopOrders() });
    }
    const productMatch = req.url.match(/^\/api\/shop\/products\/([A-Za-z0-9_-]{1,80})$/);
    if (req.method === "PUT" && productMatch) {
      if (!ownerAuthorized(req)) return sendJson(res, 401, { error: "Owner authorization required" });
      return sendJson(res, 200, { product: await saveShopProduct(productMatch[1], await readJsonBody(req)) });
    }
    const orderMatch = req.url.match(/^\/api\/shop\/orders\/([A-Za-z0-9_-]{1,120})$/);
    if (req.method === "PATCH" && orderMatch) {
      if (!ownerAuthorized(req)) return sendJson(res, 401, { error: "Owner authorization required" });
      return sendJson(res, 200, { order: await updateShopOrder(orderMatch[1], await readJsonBody(req)) });
    }

    const serverApiKey = (process.env.GEMINI_SERVER_API_KEY || "").trim();
    if (!serverApiKey) return sendJson(res, 503, { error: "Server secret GEMINI_SERVER_API_KEY is not configured." });
    if (req.method === "POST" && req.url === "/api/chat") return await handleChat(req, res, serverApiKey);
    if (req.method === "POST" && req.url === "/api/generate-image") return await handleImageGeneration(req, res, serverApiKey);
    return sendJson(res, 404, { error: "Not found" });
  } catch (err) {
    return sendJson(res, err.statusCode || 500, { error: err.message || "Internal server error" });
  }
});

server.listen(PORT, () => console.log(`Annapurna backend listening on port ${PORT}`));
