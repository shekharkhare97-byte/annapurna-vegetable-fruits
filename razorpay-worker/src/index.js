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
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(message));
  return [...new Uint8Array(signature)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function safeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function base64url(value) {
  const bytes = typeof value === "string" ? new TextEncoder().encode(value) : new Uint8Array(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function pemToArrayBuffer(pem) {
  const encoded = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const binary = atob(encoded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

async function getFirebaseAccessToken(env) {
  if (!env.FIREBASE_SERVICE_ACCOUNT_JSON) throw new Error("Firebase service account secret is not configured");
  const account = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
  if (!account.client_email || !account.private_key || !account.project_id) throw new Error("Firebase service account secret is invalid");
  if (account.project_id !== "annapurna-vegetables-and-78cef") throw new Error("Firebase service account belongs to the wrong project");

  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64url(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = header + "." + claim;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const assertion = unsigned + "." + base64url(signature);
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const tokenData = await tokenResponse.json().catch(() => ({}));
  if (!tokenResponse.ok || !tokenData.access_token) throw new Error("Firebase authentication failed");
  return { token: tokenData.access_token, projectId: account.project_id };
}

function firestoreValue(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (Array.isArray(value)) return { arrayValue: { values: value.map(firestoreValue) } };
  if (typeof value === "object") return { mapValue: { fields: firestoreFields(value) } };
  throw new Error("Unsupported order field");
}

function firestoreFields(object) {
  const fields = {};
  for (const [key, value] of Object.entries(object)) {
    if (value !== undefined) fields[key] = firestoreValue(value);
  }
  return fields;
}

async function verifyCapturedPayment(env, body) {
  const orderId = body.order_id;
  const paymentId = body.razorpay_payment_id;
  const signature = body.razorpay_signature;
  if (![orderId, paymentId, signature].every((v) => typeof v === "string" && v.length > 0 && v.length < 256)) {
    return { ok: false, status: 400, error: "Missing payment verification fields" };
  }
  const amount = Number(body.amount);
  if (!Number.isSafeInteger(amount) || amount < 100 || amount > 50000000) {
    return { ok: false, status: 400, error: "A valid expected amount in paise is required" };
  }
  const expected = await hmacSha256Hex(env.RAZORPAY_KEY_SECRET, orderId + "|" + paymentId);
  if (!safeEqual(expected, signature.toLowerCase())) return { ok: false, status: 400, error: "Payment signature is invalid" };

  const auth = btoa(env.RAZORPAY_KEY_ID + ":" + env.RAZORPAY_KEY_SECRET);
  const paymentResponse = await fetch("https://api.razorpay.com/v1/payments/" + encodeURIComponent(paymentId), {
    method: "GET",
    headers: { Authorization: "Basic " + auth },
  });
  let payment = await paymentResponse.json().catch(() => ({}));
  if (!paymentResponse.ok) return { ok: false, status: 502, error: "Could not confirm payment with Razorpay" };

  // Never capture until the signed callback and exact order/amount/currency have been verified.
  if (payment.order_id !== orderId || payment.amount !== amount || payment.currency !== "INR") {
    return { ok: false, status: 400, error: "Payment does not match this order and amount" };
  }

  // Explicitly capture an authorized payment. This also covers accounts configured for
  // manual capture and delayed/late authorizations; already-captured payments are untouched.
  if (payment.status === "authorized" && payment.captured !== true) {
    const captureResponse = await fetch("https://api.razorpay.com/v1/payments/" + encodeURIComponent(paymentId) + "/capture", {
      method: "POST",
      headers: { Authorization: "Basic " + auth, "Content-Type": "application/json" },
      body: JSON.stringify({ amount, currency: "INR" }),
    });
    const captureResult = await captureResponse.json().catch(() => ({}));
    if (captureResponse.ok) {
      payment = captureResult;
    } else {
      // A concurrent auto-capture may win the race. Re-fetch before treating capture as failed.
      const retryResponse = await fetch("https://api.razorpay.com/v1/payments/" + encodeURIComponent(paymentId), {
        headers: { Authorization: "Basic " + auth },
      });
      const retryPayment = await retryResponse.json().catch(() => ({}));
      if (retryResponse.ok && retryPayment.status === "captured" && retryPayment.captured === true) {
        payment = retryPayment;
      } else {
        console.error("razorpay-capture-failed", {
          httpStatus: captureResponse.status,
          code: captureResult.error?.code || "unknown",
          description: captureResult.error?.description || "No capture error description returned",
        });
        return { ok: false, status: 502, error: "Razorpay authorized the payment but could not capture it. Please contact support; do not pay again." };
      }
    }
  }

  if (payment.status !== "captured" || payment.captured !== true) {
    return { ok: false, status: 409, error: "Payment is not captured yet. Please do not pay again; contact support to check the payment." };
  }
  return { ok: true, payment, orderId, paymentId, amount };
}

async function savePaidOrder(env, body, verified) {
  const order = body.order;
  const tracking = body.tracking;
  if (!order || typeof order !== "object" || !tracking || typeof tracking !== "object") {
    return { ok: false, status: 400, error: "Order and tracking data are required" };
  }
  const orderId = order.orderId;
  const token = order.trackingToken;
  const mobile = order.mobileNumber;
  const total = Number(order.totalAmountRupees);
  if (typeof orderId !== "string" || !/^[A-Za-z0-9_-]{10,80}$/.test(orderId) ||
      typeof token !== "string" || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(token) ||
      typeof order.customerName !== "string" || order.customerName.trim().length < 2 ||
      typeof mobile !== "string" || !/^\d{10}$/.test(mobile) ||
      typeof order.deliveryAddress !== "string" || order.deliveryAddress.trim().length < 8 ||
      !Array.isArray(order.items) || order.items.length < 1 || order.items.length > 100 ||
      !Number.isFinite(total) || Math.round(total * 100) !== verified.amount ||
      order.paymentStatus !== "paid" || order.paymentMethod === "COD" ||
      order.transactionId !== verified.paymentId) {
    return { ok: false, status: 400, error: "Order details do not match the verified payment" };
  }
  if (tracking.status !== "new" || tracking.orderId !== orderId || tracking.createdAtMillis !== order.createdAtMillis) {
    return { ok: false, status: 400, error: "Tracking details are invalid" };
  }

  const { token: accessToken, projectId } = await getFirebaseAccessToken(env);
  const now = new Date().toISOString();
  const orderFields = { ...order, createdAt: { __firestoreTimestamp: now } };
  delete orderFields.createdAt;
  const orderDocFields = firestoreFields(orderFields);
  orderDocFields.createdAt = { timestampValue: now };
  const trackingFields = firestoreFields(tracking);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  // Make a repeated submit safe after a network timeout: accept only the same paid transaction.
  const existingResponse = await fetch(base + "/orders/" + encodeURIComponent(orderId), {
    headers: { Authorization: "Bearer " + accessToken },
  });
  if (existingResponse.ok) {
    const existing = await existingResponse.json().catch(() => ({}));
    const fields = existing.fields || {};
    if (fields.paymentStatus?.stringValue === "paid" &&
        fields.transactionId?.stringValue === verified.paymentId) {
      return { ok: true, alreadySaved: true };
    }
    return { ok: false, status: 409, error: "An order with this ID already exists with different payment details" };
  }
  if (existingResponse.status !== 404) {
    console.error("paid-order-existing-check-failed", existingResponse.status);
    return { ok: false, status: 502, error: "Firebase could not check the paid order" };
  }
  const commitResponse = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({
      writes: [
        { update: { name: base + "/orders/" + orderId, fields: orderDocFields }, currentDocument: { exists: false } },
        { update: { name: base + "/orderTracking/" + token, fields: trackingFields }, currentDocument: { exists: false } },
      ],
    }),
  });
  const result = await commitResponse.json().catch(() => ({}));
  if (!commitResponse.ok) {
    if (commitResponse.status === 409 || result.error?.status === "ALREADY_EXISTS") {
      return { ok: false, status: 409, error: "This order was already submitted" };
    }
    console.error("paid-order-firestore-write-failed", {
      httpStatus: commitResponse.status,
      firestoreStatus: result.error?.status || "unknown",
      firestoreMessage: result.error?.message || "No Firestore error message returned",
    });
    return { ok: false, status: 502, error: "Firebase could not save the paid order (" + (result.error?.status || commitResponse.status) + ")" };
  }
  return { ok: true };
}


/**
 * One-time, read-only support check for the two payment IDs reported by the
 * store owner. It returns payment state and whether the matching Firestore
 * order/tracking documents exist; it never returns customer information.
 */
async function checkReportedPayments(env) {
  const paymentIds = [
    "pay_TzkM606OnSK9LH",
    "pay_TzhjzEgaeXhOzU",
  ];
  const auth = btoa(env.RAZORPAY_KEY_ID + ":" + env.RAZORPAY_KEY_SECRET);
  const { token, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const results = [];

  for (const paymentId of paymentIds) {
    const paymentResponse = await fetch("https://api.razorpay.com/v1/payments/" + encodeURIComponent(paymentId), {
      headers: { Authorization: "Basic " + auth },
    });
    const payment = await paymentResponse.json().catch(() => ({}));
    if (!paymentResponse.ok) {
      results.push({ paymentId, razorpayFound: false, razorpayHttpStatus: paymentResponse.status });
      continue;
    }

    const queryResponse = await fetch(base + ":runQuery", {
      method: "POST",
      headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" },
      body: JSON.stringify({
        structuredQuery: {
          from: [{ collectionId: "orders" }],
          where: {
            fieldFilter: {
              field: { fieldPath: "transactionId" },
              op: "EQUAL",
              value: { stringValue: paymentId },
            },
          },
          limit: 2,
        },
      }),
    });
    const queryResult = await queryResponse.json().catch(() => ({}));
    if (!queryResponse.ok) {
      results.push({
        paymentId,
        razorpayFound: true,
        paymentStatus: payment.status || "unknown",
        captured: payment.captured === true,
        amountPaise: payment.amount ?? null,
        currency: payment.currency ?? null,
        firestoreCheck: false,
        firestoreHttpStatus: queryResponse.status,
      });
      continue;
    }

    const orderDoc = Array.isArray(queryResult)
      ? queryResult.find((entry) => entry.document)?.document
      : null;
    let trackingSaved = false;
    let orderId = null;
    if (orderDoc) {
      const fields = orderDoc.fields || {};
      orderId = fields.orderId?.stringValue || orderDoc.name?.split("/").pop() || null;
      const trackingToken = fields.trackingToken?.stringValue;
      if (trackingToken) {
        const trackingResponse = await fetch(base + "/orderTracking/" + encodeURIComponent(trackingToken), {
          headers: { Authorization: "Bearer " + token },
        });
        trackingSaved = trackingResponse.ok;
      }
    }

    results.push({
      paymentId,
      razorpayFound: true,
      paymentStatus: payment.status || "unknown",
      captured: payment.captured === true,
      amountPaise: payment.amount ?? null,
      currency: payment.currency ?? null,
      razorpayOrderId: payment.order_id ?? null,
      firestoreCheck: true,
      orderSaved: !!orderDoc,
      orderId,
      trackingSaved,
    });
  }
  return results;
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "annapurna-razorpay" });
    }
    if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) return json({ error: "Payment service is not configured" }, 503);

    if (request.method === "GET" && url.pathname === "/support/payment-check") {
      try {
        const results = await checkReportedPayments(env);
        return json({ ok: true, readOnly: true, results });
      } catch (error) {
        console.error("support-payment-check-failed", error?.message || "unknown");
        return json({ ok: false, error: "Payment/Firebase diagnostic failed. Check Worker runtime secrets and Firebase service-account permissions." }, 502);
      }
    }

    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

    let body;
    try { body = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }

    if (url.pathname === "/create-order") {
      const amount = Number(body.amount);
      if (!Number.isSafeInteger(amount) || amount < 100 || amount > 50000000) return json({ error: "Amount must be between 100 and 50000000 paise" }, 400);
      const receipt = typeof body.receipt === "string" ? body.receipt.slice(0, 40) : "ann-" + Date.now();
      const auth = btoa(env.RAZORPAY_KEY_ID + ":" + env.RAZORPAY_KEY_SECRET);
      const response = await fetch("https://api.razorpay.com/v1/orders", {
        method: "POST",
        headers: { Authorization: "Basic " + auth, "Content-Type": "application/json" },
        body: JSON.stringify({
          amount,
          currency: "INR",
          receipt,
          payment: { capture: "automatic" },
        }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) return json({ error: "Razorpay could not create the order", details: result.error?.description || "Please try again" }, 502);
      return json({ order_id: result.id, amount: result.amount, currency: result.currency, key_id: env.RAZORPAY_KEY_ID });
    }

    if (url.pathname === "/verify-payment" || url.pathname === "/submit-paid-order") {
      const requestId = crypto.randomUUID();
      let stage = "request_received";
      console.log("payment-request-received", { requestId, route: url.pathname });
      try {
        stage = "razorpay_verification";
        const verified = await verifyCapturedPayment(env, body);
        if (!verified.ok) {
          console.warn("payment-request-rejected", { requestId, stage, status: verified.status, reason: verified.error });
          return json({ verified: false, error: verified.error, request_id: requestId }, verified.status);
        }
        if (url.pathname === "/verify-payment") {
          console.log("payment-verification-succeeded", { requestId });
          return json({ verified: true, order_id: verified.orderId, payment_id: verified.paymentId, amount: verified.amount, currency: "INR", status: "captured" });
        }
        stage = "firestore_order_save";
        const saved = await savePaidOrder(env, body, verified);
        if (!saved.ok) {
          console.warn("paid-order-save-rejected", { requestId, stage, status: saved.status, reason: saved.error });
          return json({ verified: false, error: saved.error, request_id: requestId }, saved.status);
        }
        console.log("paid-order-save-succeeded", { requestId, alreadySaved: saved.alreadySaved === true });
        return json({ verified: true, saved: true, order_id: body.order.orderId, payment_id: verified.paymentId, request_id: requestId });
      } catch (error) {
        console.error("payment-request-failed", {
          requestId,
          stage,
          errorName: error?.name || "Error",
          errorMessage: String(error?.message || "Payment processing failed").slice(0, 240),
        });
        return json({ verified: false, error: "Payment processing failed. Contact support before retrying.", request_id: requestId }, 500);
      }
    }
    return json({ error: "Not found" }, 404);
  },
};
