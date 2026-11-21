const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
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
  if (value && typeof value === "object" && typeof value.__firestoreTimestamp === "string") return { timestampValue: value.__firestoreTimestamp };
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


async function firestoreGetDocument(env, path) {
  const { token, projectId } = await getFirebaseAccessToken(env);
  const url = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents/" + path;
  const response = await fetch(url, { headers: { Authorization: "Bearer " + token } });
  if (response.status === 404) return { exists: false };
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error("Firestore document read failed");
  return { exists: true, data, updateTime: data.updateTime, token, projectId };
}

function fromFirestoreValue(value) {
  if (!value) return null;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return Number(value.doubleValue);
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  return null;
}

function firestoreDocumentData(document) {
  const result = {};
  for (const [key, value] of Object.entries(document?.fields || {})) result[key] = fromFirestoreValue(value);
  return result;
}

async function verifyFirebaseIdToken(idToken) {
  // Verify Firebase Auth ID tokens using Google's published Secure Token
  // signing keys. The OAuth tokeninfo endpoint is not a reliable verifier
  // for Firebase-issued ID tokens and may omit user_id.
  if (typeof idToken !== "string" || idToken.length < 100 || idToken.length > 5000) return null;
  const parts = idToken.split(".");
  if (parts.length !== 3) return null;
  try {
    const decode = (part) => {
      const normalized = part.replace(/-/g, "+").replace(/_/g, "/");
      const binary = atob(normalized + "=".repeat((4 - normalized.length % 4) % 4));
      const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes));
    };
    const header = decode(parts[0]);
    const claims = decode(parts[1]);
    const now = Math.floor(Date.now() / 1000);
    if (header.alg !== "RS256" || typeof header.kid !== "string" ||
        claims.aud !== "annapurna-vegetables-and-78cef" ||
        claims.iss !== "https://securetoken.google.com/annapurna-vegetables-and-78cef" ||
        typeof claims.sub !== "string" || !claims.sub ||
        claims.sub.length > 128 || !Number.isFinite(claims.exp) ||
        claims.exp <= now || !Number.isFinite(claims.iat) ||
        claims.iat > now + 300 || !Number.isFinite(claims.auth_time) ||
        claims.auth_time > now + 300) return null;

    const keysResponse = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com");
    if (!keysResponse.ok) return null;
    const keySet = await keysResponse.json();
    const jwk = (keySet.keys || []).find((key) => key.kid === header.kid && key.alg === "RS256");
    if (!jwk) return null;
    const publicKey = await crypto.subtle.importKey(
      "jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]
    );
    const signature = Uint8Array.from(
      atob(parts[2].replace(/-/g, "+").replace(/_/g, "/") +
        "=".repeat((4 - parts[2].length % 4) % 4)),
      (c) => c.charCodeAt(0)
    );
    const signedData = new TextEncoder().encode(parts[0] + "." + parts[1]);
    const valid = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", publicKey, signature, signedData);
    return valid ? claims.sub : null;
  } catch (error) {
    console.error("firebase-id-token-verification-failed", error?.message || "unknown");
    return null;
  }
}

async function updateCustomerLiveLocation(env, body) {
  const trackingToken = body?.trackingToken;
  const latitude = body?.latitude;
  const longitude = body?.longitude;
  if (typeof trackingToken !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(trackingToken) ||
      typeof latitude !== "number" || !Number.isFinite(latitude) || latitude < -90 || latitude > 90 ||
      typeof longitude !== "number" || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    return json({ ok: false, error: "Valid tracking token and GPS coordinates are required" }, 400);
  }

  const { token: accessToken, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const queryResponse = await fetch(base + ":runQuery", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "orders" }],
        where: { fieldFilter: {
          field: { fieldPath: "trackingToken" },
          op: "EQUAL",
          value: { stringValue: trackingToken },
        }},
        limit: 1,
      },
    }),
  });
  const queryResult = await queryResponse.json().catch(() => ({}));
  if (!queryResponse.ok) throw new Error("Customer order lookup failed");
  const orderDocument = Array.isArray(queryResult)
    ? queryResult.find((entry) => entry.document)?.document
    : null;
  if (!orderDocument) return json({ ok: false, error: "Order not found" }, 404);

  const orderId = orderDocument.name.split("/").pop();
  const order = firestoreDocumentData(orderDocument);
  if (order.trackingToken !== trackingToken ||
      !["new", "accepted", "preparing", "ready_for_pickup", "picked_up", "out_for_delivery"].includes(order.status) ||
      ["delivered", "cancelled"].includes(order.deliveryState)) {
    return json({ ok: false, error: "Live location sharing is closed for this order" }, 409);
  }

  const previousUpdate = order.customerLocationUpdatedAt
    ? Date.parse(order.customerLocationUpdatedAt)
    : 0;
  if (Number.isFinite(previousUpdate) && Date.now() - previousUpdate < 2500) {
    return json({ ok: true, throttled: true });
  }

  const now = new Date().toISOString();
  const commit = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({
      writes: [{
        update: {
          name: "projects/" + projectId + "/databases/(default)/documents/orders/" + encodeURIComponent(orderId),
          fields: firestoreFields({
            customerLatitude: latitude,
            customerLongitude: longitude,
            customerLocationSharing: true,
            customerLocationUpdatedAt: { __firestoreTimestamp: now },
          }),
        },
        updateMask: {
          fieldPaths: [
            "customerLatitude",
            "customerLongitude",
            "customerLocationSharing",
            "customerLocationUpdatedAt",
          ],
        },
        currentDocument: { updateTime: orderDocument.updateTime },
      }],
    }),
  });
  if (!commit.ok) {
    const failure = await commit.json().catch(() => ({}));
    if (failure.error?.status === "ABORTED" || failure.error?.status === "FAILED_PRECONDITION") {
      return json({ ok: true, retry: true });
    }
    console.error("customer-live-location-save-failed", {
      status: commit.status,
      firestoreStatus: failure.error?.status || "unknown",
    });
    return json({ ok: false, error: "Could not update customer location" }, 409);
  }
  return json({ ok: true, updated: true });
}

async function verifyPickupQr(env, request, body, attempt = 0) {
  const authHeader = request.headers.get("Authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
  const partnerUid = await verifyFirebaseIdToken(idToken);
  if (!partnerUid) return json({ ok: false, error: "Delivery partner authentication required" }, 401);

  const orderId = body?.orderId;
  const scannedToken = body?.token;
  if (typeof orderId !== "string" || !/^[A-Za-z0-9_-]{10,80}$/.test(orderId) ||
      typeof scannedToken !== "string" || scannedToken.length < 40 || scannedToken.length > 160) {
    return json({ ok: false, error: "Invalid pickup QR" }, 400);
  }

  const qrPath = "pickupQrTokens/" + encodeURIComponent(orderId);
  const orderPath = "orders/" + encodeURIComponent(orderId);
  const openPath = "deliveryOpenOrders/" + encodeURIComponent(orderId);
  const qrDoc = await firestoreGetDocument(env, qrPath);
  const orderDoc = await firestoreGetDocument(env, orderPath);
  const openDoc = await firestoreGetDocument(env, openPath);
  if (!qrDoc.exists || !orderDoc.exists || !openDoc.exists) {
    return json({ ok: false, error: "Pickup QR or assigned order was not found" }, 404);
  }

  const qr = firestoreDocumentData(qrDoc.data);
  const order = firestoreDocumentData(orderDoc.data);
  const openOrder = firestoreDocumentData(openDoc.data);

  // A retry after a successful scan is safe, but the same QR can never pick up
  // another order or be redeemed by a different partner.
  if (qr.used === true && qr.usedByUid === partnerUid &&
      order.deliveryPartnerUid === partnerUid &&
      ["picked_up", "out_for_delivery"].includes(order.deliveryState)) {
    return json({ ok: true, pickedUp: true, alreadyVerified: true });
  }

  const partnerDoc = await firestoreGetDocument(env, "deliveryPartners/" + encodeURIComponent(partnerUid));
  if (!partnerDoc.exists) return json({ ok: false, error: "Delivery Partner account not found" }, 403);
  const partner = firestoreDocumentData(partnerDoc.data);
  if (partner.registrationComplete !== true || partner.registrationStatus !== "approved" ||
      partner.accountStatus !== "active" || partner.isBlocked === true) {
    return json({ ok: false, error: "Delivery Partner account is not approved and active" }, 403);
  }

  if (qr.orderId !== orderId || qr.partnerUid !== partnerUid ||
      qr.token !== scannedToken || qr.used === true ||
      !qr.expiresAt || Date.parse(qr.expiresAt) <= Date.now()) {
    return json({ ok: false, error: "Pickup QR is invalid, expired, or already used" }, 403);
  }
  if (order.deliveryPartnerUid !== partnerUid ||
      order.deliveryState !== "claimed_pending_shop_qr" ||
      openOrder.state !== "claimed_pending_shop_qr" ||
      openOrder.claimedByUid !== partnerUid) {
    return json({ ok: false, error: "This order is not assigned to your account for shop pickup" }, 403);
  }

  const { token: accessToken, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const now = new Date().toISOString();
  const writes = [
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/" + qrPath,
        fields: firestoreFields({
          used: true,
          usedByUid: partnerUid,
          usedAt: { __firestoreTimestamp: now },
        }),
      },
      updateMask: { fieldPaths: ["used", "usedByUid", "usedAt"] },
      currentDocument: { updateTime: qrDoc.updateTime },
    },
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/" + orderPath,
        fields: firestoreFields({
          deliveryState: "picked_up",
          deliveryPickedUpAt: { __firestoreTimestamp: now },
          updatedAt: { __firestoreTimestamp: now },
        }),
      },
      updateMask: { fieldPaths: ["deliveryState", "deliveryPickedUpAt", "updatedAt"] },
      currentDocument: { updateTime: orderDoc.updateTime },
    },
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/" + openPath,
        fields: firestoreFields({
          state: "picked_up",
          pickedUpAt: { __firestoreTimestamp: now },
        }),
      },
      updateMask: { fieldPaths: ["state", "pickedUpAt"] },
      currentDocument: { updateTime: openDoc.updateTime },
    },
  ];

  const trackingToken = typeof order.trackingToken === "string" ? order.trackingToken : "";
  if (trackingToken) {
    const trackingPath = "orderTracking/" + encodeURIComponent(trackingToken);
    const trackingDoc = await firestoreGetDocument(env, trackingPath);
    if (trackingDoc.exists) {
      writes.push({
        update: {
          name: "projects/" + projectId + "/databases/(default)/documents/" + trackingPath,
          fields: firestoreFields({
            status: "picked_up",
            updatedAt: { __firestoreTimestamp: now },
          }),
        },
        updateMask: { fieldPaths: ["status", "updatedAt"] },
        currentDocument: { updateTime: trackingDoc.updateTime },
      });
    }
  }

  const commit = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
    body: JSON.stringify({ writes }),
  });
  if (!commit.ok) {
    const failure = await commit.json().catch(() => ({}));
    const firestoreStatus = failure.error?.status || "unknown";
    console.error("pickup-qr-commit-failed", {
      status: commit.status,
      firestoreStatus,
      attempt,
    });
    // Firestore may reject a valid atomic pickup when another app updates the
    // order at nearly the same time. Re-read all documents and retry safely.
    if (attempt < 2 && ["ABORTED", "FAILED_PRECONDITION"].includes(firestoreStatus)) {
      return verifyPickupQr(env, request, body, attempt + 1);
    }
    const firestoreMessage = typeof failure.error?.message === "string"
      ? failure.error.message.replace(/projects\/[^/\s]+/g, "projects/[redacted]").slice(0, 300)
      : "No Firestore detail returned";
    console.error("pickup-qr-commit-detail", { firestoreStatus, firestoreMessage });
    return json({
      ok: false,
      error: "QR verification could not be saved. Please scan again.",
      code: firestoreStatus,
      detail: firestoreMessage,
    }, 409);
  }
  return json({ ok: true, pickedUp: true });
}

async function completeDelivery(env, request, body) {
  const authHeader = request.headers.get("Authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
  const partnerUid = await verifyFirebaseIdToken(idToken);
  if (!partnerUid) return json({ ok: false, error: "Delivery partner authentication required" }, 401);
  const orderId = body?.orderId;
  if (typeof orderId !== "string" || !/^[A-Za-z0-9_-]{10,80}$/.test(orderId)) {
    return json({ ok: false, error: "Valid order ID is required" }, 400);
  }
  const orderDoc = await firestoreGetDocument(env, "orders/" + encodeURIComponent(orderId));
  if (!orderDoc.exists) return json({ ok: false, error: "Order not found" }, 404);
  const order = firestoreDocumentData(orderDoc.data);
  if (order.deliveryPartnerUid !== partnerUid ||
      !["picked_up", "out_for_delivery"].includes(order.deliveryState)) {
    return json({ ok: false, error: "This order is not assigned to your account" }, 403);
  }
  if (typeof order.trackingToken !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(order.trackingToken)) {
    return json({ ok: false, error: "Order tracking record is invalid" }, 409);
  }

  // Delivery can be completed only after the assigned partner has redeemed
  // the one-time Shop Pickup QR through this trusted Worker endpoint.
  const pickupQrDoc = await firestoreGetDocument(env, "pickupQrTokens/" + encodeURIComponent(orderId));
  if (!pickupQrDoc.exists) return json({ ok: false, error: "Shop pickup QR has not been verified" }, 409);
  const pickupQr = firestoreDocumentData(pickupQrDoc.data);
  if (pickupQr.used !== true || pickupQr.usedByUid !== partnerUid || pickupQr.partnerUid !== partnerUid) {
    return json({ ok: false, error: "Shop pickup QR verification is required before delivery completion" }, 403);
  }

  const { token, projectId } = await getFirebaseAccessToken(env);
  const base = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/(default)/documents";
  const trackingPath = "orderTracking/" + encodeURIComponent(order.trackingToken);
  const trackingDoc = await firestoreGetDocument(env, trackingPath);
  if (!trackingDoc.exists) return json({ ok: false, error: "Tracking record not found" }, 409);

  // Award a fixed ₹50 milestone bonus after every 10th successful delivery.
  // The partner counter is updated in the same atomic Firestore commit as the order,
  // with update-time preconditions to prevent duplicate or concurrent awards.
  const partnerDoc = await firestoreGetDocument(env, "deliveryPartners/" + encodeURIComponent(partnerUid));
  if (!partnerDoc.exists) return json({ ok: false, error: "Delivery partner profile not found" }, 409);
  const partner = firestoreDocumentData(partnerDoc.data);
  const completedCount = Number(partner.completedDeliveryCount || 0) + 1;
  const milestoneBonus = completedCount % 10 === 0 ? 50 : 0;
  const totalMilestoneBonus = Number(partner.deliveryMilestoneBonusRupees || 0) + milestoneBonus;
  const milestoneCount = Number(partner.deliveryMilestoneBonusCount || 0) + (milestoneBonus ? 1 : 0);
  const now = new Date().toISOString();
  const writes = [
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/orders/" + orderId,
        fields: firestoreFields({
          deliveryState: "delivered",
          status: "completed",
          deliveryDeliveredAt: { __firestoreTimestamp: now },
          deliveryLocationSharing: false,
          customerLocationSharing: false,
          deliveryMilestoneBonusRupees: milestoneBonus,
        }),
      },
      updateMask: { fieldPaths: ["deliveryState", "status", "deliveryDeliveredAt", "deliveryLocationSharing", "customerLocationSharing", "deliveryMilestoneBonusRupees"] },
      currentDocument: { updateTime: orderDoc.updateTime },
    },
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/" + trackingPath,
        fields: firestoreFields({
          status: "completed",
          deliveryLocationSharing: false,
          updatedAt: { __firestoreTimestamp: now },
        }),
      },
      updateMask: { fieldPaths: ["status", "deliveryLocationSharing", "updatedAt"] },
      currentDocument: { updateTime: trackingDoc.updateTime },
    },
    {
      update: {
        name: "projects/" + projectId + "/databases/(default)/documents/deliveryPartners/" + encodeURIComponent(partnerUid),
        fields: firestoreFields({
          completedDeliveryCount: completedCount,
          deliveryMilestoneBonusRupees: totalMilestoneBonus,
          deliveryMilestoneBonusCount: milestoneCount,
          deliveryMilestoneBonusUpdatedAt: { __firestoreTimestamp: now },
        }),
      },
      updateMask: { fieldPaths: ["completedDeliveryCount", "deliveryMilestoneBonusRupees", "deliveryMilestoneBonusCount", "deliveryMilestoneBonusUpdatedAt"] },
      currentDocument: { updateTime: partnerDoc.updateTime },
    },
  ];
  const commit = await fetch(base + ":commit", {
    method: "POST",
    headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" },
    body: JSON.stringify({ writes }),
  });
  if (!commit.ok) {
    const commitError = await commit.json().catch(() => ({}));
    const firestoreError = commitError && commitError.error ? commitError.error : {};
    // Keep detailed Firestore diagnostics in Worker logs only; never expose project
    // details or backend messages to the delivery partner.
    console.error("delivery-completion-commit-failed", JSON.stringify({
      httpStatus: commit.status,
      firestoreStatus: firestoreError.status || null,
      firestoreCode: firestoreError.code || null,
      firestoreMessage: typeof firestoreError.message === "string"
        ? firestoreError.message.slice(0, 500)
        : null,
    }));
    const safeCode = typeof firestoreError.status === "string" &&
      /^[A-Z_]{3,40}$/.test(firestoreError.status)
      ? firestoreError.status
      : "UNKNOWN";
    const safeMessage = typeof firestoreError.message === "string"
      ? firestoreError.message.replace(/projects\/[^/\s]+/g, "projects/[redacted]").slice(0, 300)
      : "No Firestore detail returned";
    return json({
      ok: false,
      error: "Could not complete delivery (Firestore: " + safeCode + "). " + safeMessage,
    }, 409);
  }
  return json({ ok: true, delivered: true });
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
  // Payment fields are written by this trusted Worker, never trusted from the Android payload.
  // This gives the Admin order screen an authoritative captured amount and Razorpay references.
  const orderFields = {
    ...order,
    paymentMethod: "RAZORPAY",
    paymentGateway: "razorpay",
    paymentStatus: "paid",
    transactionId: verified.paymentId,
    paidAmountPaise: verified.amount,
    razorpayPaymentId: verified.paymentId,
    razorpayOrderId: verified.orderId,
    paymentCapturedAt: { __firestoreTimestamp: now },
  };
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
        { update: { name: "projects/" + projectId + "/databases/(default)/documents/orders/" + orderId, fields: orderDocFields }, currentDocument: { exists: false } },
        { update: { name: "projects/" + projectId + "/databases/(default)/documents/orderTracking/" + token, fields: trackingFields }, currentDocument: { exists: false } },
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
    // Support diagnostics expose payment/deployment metadata. Require a private
    // bearer token so these read-only endpoints are not publicly enumerable.
    const supportToken = env.SUPPORT_DIAGNOSTIC_TOKEN;
    const suppliedSupportToken = (request.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
    const supportAuthorized = Boolean(supportToken) && safeEqual(suppliedSupportToken, supportToken);

    // Read-only diagnostic: validates Razorpay credentials without creating a payment/order.
    if (request.method === "GET" && url.pathname === "/support/config-check") {
      if (!supportAuthorized) return json({ ok: false, error: "Unauthorized" }, 401);
      if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) {
        return json({ ok: false, configured: false, error: "Razorpay credentials are missing" }, 503);
      }
      const keyId = String(env.RAZORPAY_KEY_ID);
      const mode = keyId.startsWith("rzp_test_") ? "test" : keyId.startsWith("rzp_live_") ? "live" : "unknown";
      try {
        const auth = btoa(keyId + ":" + env.RAZORPAY_KEY_SECRET);
        const check = await fetch("https://api.razorpay.com/v1/orders?count=1", {
          headers: { Authorization: "Basic " + auth },
        });
        const payload = await check.json().catch(() => ({}));
        return json({
          ok: check.ok,
          configured: true,
          mode,
          keyPrefix: keyId.slice(0, 12),
          credentialsAccepted: check.ok,
          error: check.ok ? null : (payload.error?.description || "Razorpay rejected the configured credentials"),
        }, check.ok ? 200 : 502);
      } catch (error) {
        return json({ ok: false, configured: true, mode, keyPrefix: keyId.slice(0, 12), error: "Could not reach Razorpay API" }, 502);
      }
    }
    if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) return json({ error: "Payment service is not configured" }, 503);

    if (request.method === "GET" && url.pathname === "/support/payment-check") {
      if (!supportAuthorized) return json({ ok: false, error: "Unauthorized" }, 401);
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

    if (url.pathname === "/customer-live-location") {
      try { return await updateCustomerLiveLocation(env, body); }
      catch (error) {
        console.error("customer-live-location-failed", error?.message || "unknown");
        return json({ ok: false, error: "Could not update customer location. Please retry." }, 502);
      }
    }
    if (url.pathname === "/verify-pickup-qr") {
      try { return await verifyPickupQr(env, request, body); }
      catch (error) {
        console.error("pickup-qr-verification-failed", error?.message || "unknown");
        return json({ ok: false, error: "Could not verify pickup QR. Please retry." }, 502);
      }
    }
    if (url.pathname === "/complete-delivery") {
      try { return await completeDelivery(env, request, body); }
      catch (error) {
        console.error("delivery-completion-failed", error?.message || "unknown");
        return json({ ok: false, error: "Could not complete delivery. Please retry." }, 502);
      }
    }

    if (url.pathname === "/create-order") {
      const amount = Number(body.amount);
      if (!Number.isSafeInteger(amount) || amount < 100 || amount > 50000000) return json({ error: "Amount must be between 100 and 50000000 paise" }, 400);
      const receipt = typeof body.receipt === "string" ? body.receipt.slice(0, 40) : "ann-" + Date.now();
      const auth = btoa(env.RAZORPAY_KEY_ID + ":" + env.RAZORPAY_KEY_SECRET);
      try {
        const response = await fetch("https://api.razorpay.com/v1/orders", {
          method: "POST",
          headers: { Authorization: "Basic " + auth, "Content-Type": "application/json" },
          body: JSON.stringify({ amount, currency: "INR", receipt }),
        });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) {
          console.error("razorpay-create-order-rejected", { status: response.status, code: result.error?.code, description: result.error?.description });
          return json({ error: "Razorpay could not create the order", details: result.error?.description || "Please try again", code: result.error?.code || null }, 502);
        }
        return json({ order_id: result.id, amount: result.amount, currency: result.currency, key_id: env.RAZORPAY_KEY_ID });
      } catch (error) {
        console.error("razorpay-create-order-network-error", String(error?.message || error));
        return json({ error: "Could not connect to Razorpay. Please try again.", details: "Razorpay API network request failed." }, 502);
      }
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