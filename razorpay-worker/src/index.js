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
  const payment = await paymentResponse.json().catch(() => ({}));
  if (!paymentResponse.ok) return { ok: false, status: 502, error: "Could not confirm payment with Razorpay" };
  if (payment.order_id !== orderId || payment.amount !== amount || payment.currency !== "INR" ||
      payment.status !== "captured" || payment.captured !== true) {
    return { ok: false, status: 400, error: "Payment is not captured for this order and amount" };
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
  const clientTotal = Number(order.totalAmountRupees);
  const deliveryCharge = Number(order.deliveryChargeRupees ?? 0);
  const deliveryTip = Number(order.deliveryTipRupees ?? 0);
  const distance = Number(order.deliveryDistanceKm ?? 0);
  if (typeof orderId !== "string" || !/^[A-Za-z0-9_-]{10,80}$/.test(orderId) ||
      typeof token !== "string" || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(token) ||
      typeof order.customerName !== "string" || order.customerName.trim().length < 2 ||
      typeof mobile !== "string" || !/^\d{10}$/.test(mobile.trim()) ||
      typeof order.deliveryAddress !== "string" || order.deliveryAddress.trim().length < 8 ||
      !Array.isArray(order.items) || order.items.length < 1 || order.items.length > 50 ||
      !Number.isFinite(clientTotal) || Math.round(clientTotal * 100) !== verified.amount ||
      !Number.isSafeInteger(deliveryCharge) || deliveryCharge < 0 || deliveryCharge > 2000 ||
      !Number.isSafeInteger(deliveryTip) || deliveryTip < 0 || deliveryTip > 5000 ||
      !Number.isFinite(distance) || distance < 0 || distance > 100 ||
      order.paymentStatus !== "paid" || order.paymentMethod === "COD" ||
      order.transactionId !== verified.paymentId) {
    return { ok: false, status: 400, error: "Order details do not match the verified payment" };
  }
  if (tracking.status !== "new" || tracking.orderId !== orderId || tracking.createdAtMillis !== order.createdAtMillis ||
      !Number.isSafeInteger(Number(order.createdAtMillis)) || Number(order.createdAtMillis) <= 0) {
    return { ok: false, status: 400, error: "Tracking details are invalid" };
  }

  const { token: accessToken, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const validated = await loadVerifiedItems(base, accessToken, order.items);
  if (!validated.ok) return validated;
  const serverTotal = Math.round((validated.itemsTotalRupees + deliveryCharge + deliveryTip) * 100) / 100;
  if (Math.round(serverTotal * 100) !== verified.amount) {
    return { ok: false, status: 400, error: "Paid amount does not match current product prices and stock" };
  }

  const now = new Date().toISOString();
  const serverOrder = {
    ...order,
    customerName: order.customerName.trim(),
    mobileNumber: mobile.trim(),
    deliveryAddress: order.deliveryAddress.trim(),
    items: validated.items,
    itemsTotalRupees: validated.itemsTotalRupees,
    itemsSummaryMarathi: validated.summary + "\n• डिलिव्हरी चार्ज = ₹" + deliveryCharge +
      (deliveryTip ? "\n• डिलिव्हरी पार्टनर टिप = ₹" + deliveryTip : ""),
    totalItemsCount: validated.items.reduce((sum, item) => sum + item.quantity, 0),
    deliveryChargeRupees: deliveryCharge,
    deliveryTipRupees: deliveryTip,
    deliveryDistanceKm: distance,
    totalAmountRupees: serverTotal,
    status: "new",
    paymentStatus: "paid",
    transactionId: verified.paymentId,
    stockReservedByServer: true,
    stockReservationVersion: 1,
  };
  const orderDocFields = firestoreFields(serverOrder);
  orderDocFields.createdAt = { timestampValue: now };
  const trackingFields = firestoreFields({
    status: "new",
    createdAtMillis: Number(order.createdAtMillis),
    orderId,
  });
  const commitResponse = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({
      writes: [
        { update: { name: base + "/orders/" + orderId, fields: orderDocFields }, currentDocument: { exists: false } },
        { update: { name: base + "/orderTracking/" + token, fields: trackingFields }, currentDocument: { exists: false } },
        ...stockUpdateWrites(validated.stockUpdates),
      ],
    }),
  });
  const result = await commitResponse.json().catch(() => ({}));
  if (!commitResponse.ok) {
    if (result.error?.status === "FAILED_PRECONDITION") {
      return { ok: false, status: 409, error: "Product stock changed during checkout; please retry your order" };
    }
    if (commitResponse.status === 409 || result.error?.status === "ALREADY_EXISTS") {
      return { ok: false, status: 409, error: "This order was already submitted" };
    }
    return { ok: false, status: 502, error: "Firebase could not save the paid order" };
  }
  return { ok: true };
}


function readFirestoreScalar(field) {
  if (!field || typeof field !== "object") return null;
  if ("stringValue" in field) return field.stringValue;
  if ("integerValue" in field) return Number(field.integerValue);
  if ("doubleValue" in field) return Number(field.doubleValue);
  if ("booleanValue" in field) return field.booleanValue;
  return null;
}


async function loadVerifiedItems(base, accessToken, rawItems) {
  if (!Array.isArray(rawItems) || rawItems.length < 1 || rawItems.length > 50) {
    return { ok: false, status: 400, error: "Order items are invalid" };
  }
  const items = [];
  const products = new Map();
  const stockNeededByProduct = new Map();
  let itemsTotalRupees = 0;
  for (const raw of rawItems) {
    if (!raw || typeof raw !== "object") return { ok: false, status: 400, error: "An order item is invalid" };
    const productId = typeof raw.productId === "string" ? raw.productId : "";
    const quantity = Number(raw.quantity);
    const weightGrams = Number(raw.weightGrams ?? 1000);
    if (!/^[A-Za-z0-9_-]{1,150}$/.test(productId) ||
        !Number.isSafeInteger(quantity) || quantity < 1 || quantity > 100 ||
        !Number.isSafeInteger(weightGrams) || weightGrams < 125 || weightGrams > 10000) {
      return { ok: false, status: 400, error: "Product quantity or weight is invalid" };
    }
    let productDoc = products.get(productId);
    if (!productDoc) {
      const productResponse = await fetch(base + "/products/" + encodeURIComponent(productId), {
        headers: { Authorization: "Bearer " + accessToken },
      });
      productDoc = await productResponse.json().catch(() => ({}));
      if (!productResponse.ok) return { ok: false, status: 409, error: "A selected product is no longer available" };
      if (typeof productDoc.updateTime !== "string" || !productDoc.updateTime) {
        return { ok: false, status: 409, error: "Product stock changed; please retry your order" };
      }
      products.set(productId, productDoc);
    }
    const fields = productDoc.fields || {};
    const value = (key) => readFirestoreScalar(fields[key]);
    const stock = Number(value("stock") ?? 0);
    if (value("isVisible") !== true || stock <= 0) {
      return { ok: false, status: 409, error: "A selected product is out of stock" };
    }
    const rawUnit = String(value("unit") ?? "kg").trim().toLowerCase();
    const isBunch = rawUnit === "bunch" || rawUnit === "bunches" || rawUnit.includes("जुडी") || rawUnit.includes("जोडी");
    const isPiece = isBunch || ["piece", "pieces", "nos", "pcs", "नग"].includes(rawUnit);
    const price = Number(value(isPiece ? "pricePerPiece" : "pricePerKg") ?? value("price"));
    if (!Number.isFinite(price) || price < 0 || price > 100000) {
      return { ok: false, status: 409, error: "A product price is not configured correctly" };
    }
    const stockNeeded = isPiece ? quantity : (weightGrams * quantity) / 1000;
    stockNeededByProduct.set(productId, (stockNeededByProduct.get(productId) || 0) + stockNeeded);
    const lineTotal = isPiece ? price * quantity : price * weightGrams * quantity / 1000;
    const productName = String(value("nameMr") ?? value("marathiName") ?? value("name") ?? "प्रॉडक्ट");
    items.push({
      productId,
      name: String(value("name") ?? productName),
      nameMr: productName,
      nameHi: String(value("nameHi") ?? value("hindiName") ?? ""),
      nameEn: String(value("nameEn") ?? value("englishName") ?? ""),
      weightGrams,
      quantity,
      pricePerKgRupees: isPiece ? 0 : price,
      pricePerPieceRupees: isPiece ? price : 0,
      unit: rawUnit,
      isPieceBased: isPiece,
      lineTotalRupees: Math.round(lineTotal * 100) / 100,
    });
    itemsTotalRupees += Math.round(lineTotal * 100) / 100;
  }
  const stockUpdates = [];
  for (const [productId, stockNeeded] of stockNeededByProduct) {
    const doc = products.get(productId);
    const stockField = doc.fields.stock;
    const stock = Number(readFirestoreScalar(stockField) ?? 0);
    if (stockNeeded > stock + 0.000001) {
      const productName = readFirestoreScalar(doc.fields.name) || "a selected product";
      return { ok: false, status: 409, error: "Insufficient stock for " + productName };
    }
    const remaining = Math.max(0, stock - stockNeeded);
    const stockValue = stockField && "integerValue" in stockField && Number.isInteger(remaining)
      ? { integerValue: String(remaining) }
      : { doubleValue: remaining };
    stockUpdates.push({
      name: base + "/products/" + productId,
      updateTime: doc.updateTime,
      stockValue,
    });
  }
  itemsTotalRupees = Math.round(itemsTotalRupees * 100) / 100;
  const summary = items.map((item) =>
    "• " + item.nameMr + " (" + (item.isPieceBased ? item.quantity + " " + item.unit : item.weightGrams * item.quantity + " g") + ") = ₹" + item.lineTotalRupees.toFixed(2)
  ).join("\n");
  return { ok: true, items, itemsTotalRupees, summary, stockUpdates };
}

function stockUpdateWrites(stockUpdates) {
  return stockUpdates.map((item) => ({
    update: { name: item.name, fields: { stock: item.stockValue } },
    updateMask: { fieldPaths: ["stock"] },
    currentDocument: { updateTime: item.updateTime },
  }));
}

async function submitCodOrder(env, body) {
  const order = body.order;
  const tracking = body.tracking;
  if (!order || typeof order !== "object" || !tracking || typeof tracking !== "object") {
    return { ok: false, status: 400, error: "Order and tracking data are required" };
  }
  const orderId = order.orderId;
  const token = order.trackingToken;
  const mobile = typeof order.mobileNumber === "string" ? order.mobileNumber.trim() : "";
  const name = typeof order.customerName === "string" ? order.customerName.trim() : "";
  const address = typeof order.deliveryAddress === "string" ? order.deliveryAddress.trim() : "";
  if (typeof orderId !== "string" || !/^[A-Za-z0-9_-]{10,80}$/.test(orderId) ||
      typeof token !== "string" || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(token) ||
      name.length < 2 || name.length > 100 || !/^\d{10}$/.test(mobile) ||
      address.length < 8 || address.length > 500 ||
      !Array.isArray(order.items) || order.items.length < 1 || order.items.length > 50 ||
      tracking.status !== "new" || tracking.orderId !== orderId ||
      !Number.isSafeInteger(Number(order.createdAtMillis)) || Number(order.createdAtMillis) <= 0) {
    return { ok: false, status: 400, error: "Customer or order details are invalid" };
  }

  const deliveryCharge = Number(order.deliveryChargeRupees ?? 0);
  const deliveryTip = Number(order.deliveryTipRupees ?? 0);
  const distance = Number(order.deliveryDistanceKm ?? 0);
  if (!Number.isSafeInteger(deliveryCharge) || deliveryCharge < 0 || deliveryCharge > 2000 ||
      !Number.isSafeInteger(deliveryTip) || deliveryTip < 0 || deliveryTip > 5000 ||
      !Number.isFinite(distance) || distance < 0 || distance > 100) {
    return { ok: false, status: 400, error: "Delivery charge, tip or distance is invalid" };
  }

  const { token: accessToken, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const validated = await loadVerifiedItems(base, accessToken, order.items);
  if (!validated.ok) return validated;
  const verifiedItems = validated.items;
  const itemsTotal = validated.itemsTotalRupees;
  const summary = validated.summary;
  const total = Math.round((itemsTotal + deliveryCharge + deliveryTip) * 100) / 100;
  const serverOrder = {
    orderId, trackingToken: token, customerName: name, mobileNumber: mobile, deliveryAddress: address,
    items: verifiedItems, itemsSummaryMarathi: summary + "\n• डिलिव्हरी चार्ज = ₹" + deliveryCharge + (deliveryTip ? "\n• डिलिव्हरी पार्टनर टिप = ₹" + deliveryTip : ""),
    totalItemsCount: verifiedItems.reduce((sum, item) => sum + item.quantity, 0),
    itemsTotalRupees: itemsTotal, deliveryChargeRupees: deliveryCharge, deliveryTipRupees: deliveryTip,
    deliveryDistanceKm: distance, totalAmountRupees: total, status: "new", paymentMethod: "COD",
    transactionId: "", paymentStatus: "pending", paymentGateway: "cash_on_delivery",
    stockReservedByServer: true, stockReservationVersion: 1,
    createdAtMillis: Number(order.createdAtMillis),
  };
  if (Number.isFinite(Number(order.latitude)) && Number.isFinite(Number(order.longitude))) {
    serverOrder.latitude = Number(order.latitude);
    serverOrder.longitude = Number(order.longitude);
    serverOrder.locationSource = "customer_address_geocoded";
  }
  const now = new Date().toISOString();
  const orderDocFields = firestoreFields(serverOrder);
  orderDocFields.createdAt = { timestampValue: now };
  const trackingFields = firestoreFields({ status: "new", createdAtMillis: serverOrder.createdAtMillis, orderId });
  const commitResponse = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({ writes: [
      { update: { name: base + "/orders/" + orderId, fields: orderDocFields }, currentDocument: { exists: false } },
      { update: { name: base + "/orderTracking/" + token, fields: trackingFields }, currentDocument: { exists: false } },
      ...stockUpdateWrites(validated.stockUpdates),
    ] }),
  });
  const result = await commitResponse.json().catch(() => ({}));
  if (!commitResponse.ok) {
    if (commitResponse.status === 409 || result.error?.status === "ALREADY_EXISTS") {
      return { ok: false, status: 409, error: "This order was already submitted" };
    }
    return { ok: false, status: 502, error: "Firebase could not save the COD order" };
  }
  return { ok: true, totalAmountRupees: total };
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "annapurna-razorpay" });
    }
    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

    let body;
    try { body = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }

    if (url.pathname === "/submit-cod-order") {
      try {
        const saved = await submitCodOrder(env, body);
        if (!saved.ok) return json({ saved: false, error: saved.error }, saved.status);
        return json({ saved: true, totalAmountRupees: saved.totalAmountRupees });
      } catch (error) {
        return json({ saved: false, error: error?.message || "COD order processing failed" }, 500);
      }
    }

    if (url.pathname === "/create-order") {
      if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) return json({ error: "Payment service is not configured" }, 503);
      const amount = Number(body.amount);
      if (!Number.isSafeInteger(amount) || amount < 100 || amount > 50000000) return json({ error: "Amount must be between 100 and 50000000 paise" }, 400);
      const receipt = typeof body.receipt === "string" ? body.receipt.slice(0, 40) : "ann-" + Date.now();
      const auth = btoa(env.RAZORPAY_KEY_ID + ":" + env.RAZORPAY_KEY_SECRET);
      const response = await fetch("https://api.razorpay.com/v1/orders", {
        method: "POST",
        headers: { Authorization: "Basic " + auth, "Content-Type": "application/json" },
        body: JSON.stringify({ amount, currency: "INR", receipt }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) return json({ error: "Razorpay could not create the order", details: result.error?.description || "Please try again" }, 502);
      return json({ order_id: result.id, amount: result.amount, currency: result.currency, key_id: env.RAZORPAY_KEY_ID });
    }

    if (url.pathname === "/verify-payment" || url.pathname === "/submit-paid-order") {
      if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) return json({ error: "Payment service is not configured" }, 503);
      try {
        const verified = await verifyCapturedPayment(env, body);
        if (!verified.ok) return json({ verified: false, error: verified.error }, verified.status);
        if (url.pathname === "/verify-payment") {
          return json({ verified: true, order_id: verified.orderId, payment_id: verified.paymentId, amount: verified.amount, currency: "INR", status: "captured" });
        }
        const saved = await savePaidOrder(env, body, verified);
        if (!saved.ok) return json({ verified: false, error: saved.error }, saved.status);
        return json({ verified: true, saved: true, order_id: body.order.orderId, payment_id: verified.paymentId });
      } catch (error) {
        return json({ verified: false, error: error?.message || "Payment processing failed" }, 500);
      }
    }
    return json({ error: "Not found" }, 404);
  },
};
