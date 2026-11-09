const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Content-Type": "application/json; charset=utf-8",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: corsHeaders });
}

async function hmacSha256Hex(secret, message) {
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(message));
  return [...new Uint8Array(signature)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function safeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "annapurna-razorpay" });
    }

    if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) {
      return json({ error: "Payment service is not configured" }, 503);
    }

    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

    if (url.pathname === "/create-order") {
      let body;
      try { body = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }
      const amount = Number(body.amount);
      if (!Number.isSafeInteger(amount) || amount < 100 || amount > 50000000) {
        return json({ error: "Amount must be between 100 and 50000000 paise" }, 400);
      }
      const receipt = typeof body.receipt === "string"
        ? body.receipt.slice(0, 40)
        : `ann-${Date.now()}`;
      const auth = btoa(`${env.RAZORPAY_KEY_ID}:${env.RAZORPAY_KEY_SECRET}`);
      const response = await fetch("https://api.razorpay.com/v1/orders", {
        method: "POST",
        headers: {
          "Authorization": `Basic ${auth}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ amount, currency: "INR", receipt }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        return json({ error: "Razorpay could not create the order", details: result.error?.description || "Please try again" }, 502);
      }
      return json({
        order_id: result.id,
        amount: result.amount,
        currency: result.currency,
        key_id: env.RAZORPAY_KEY_ID,
      });
    }

    if (url.pathname === "/verify-payment") {
      let body;
      try { body = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }
      const orderId = body.order_id;
      const paymentId = body.razorpay_payment_id;
      const signature = body.razorpay_signature;
      if (![orderId, paymentId, signature].every((v) => typeof v === "string" && v.length > 0 && v.length < 256)) {
        return json({ verified: false, error: "Missing payment verification fields" }, 400);
      }
      const expected = await hmacSha256Hex(
        env.RAZORPAY_KEY_SECRET,
        `${orderId}|${paymentId}`
      );
      if (!safeEqual(expected, signature.toLowerCase())) {
        return json({ verified: false, error: "Payment signature is invalid" }, 400);
      }
      return json({ verified: true, order_id: orderId, payment_id: paymentId });
    }

    return json({ error: "Not found" }, 404);
  },
};
