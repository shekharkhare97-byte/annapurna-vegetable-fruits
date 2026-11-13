import test, { afterEach } from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, createHmac } from "node:crypto";
import worker from "../src/index.js";

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

function fixture() {
  const { privateKey } = generateKeyPairSync("rsa", {
    modulusLength: 2048,
    privateKeyEncoding: { type: "pkcs8", format: "pem" },
    publicKeyEncoding: { type: "spki", format: "pem" },
  });
  const env = {
    FIREBASE_SERVICE_ACCOUNT_JSON: JSON.stringify({
      project_id: "annapurna-vegetables-and-78cef",
      client_email: "worker-test@annapurna-vegetables-and-78cef.iam.gserviceaccount.com",
      private_key: privateKey,
    }),
  };
  const order = {
    orderId: "test-order-0001",
    trackingToken: "123e4567-e89b-42d3-a456-426614174000",
    customerName: "Test Customer",
    mobileNumber: "9876543210",
    deliveryAddress: "Test address, Nashik",
    items: [{ productId: "tomato-1", quantity: 1, weightGrams: 500 }],
    deliveryChargeRupees: 20,
    deliveryTipRupees: 0,
    deliveryDistanceKm: 2,
    createdAtMillis: 1794500000000,
  };
  const tracking = { status: "new", orderId: order.orderId, createdAtMillis: order.createdAtMillis };
  return { env, order, tracking };
}

function firestoreProduct(stock = 10) {
  return {
    updateTime: "2026-11-13T00:00:00.000000Z",
    fields: {
      isVisible: { booleanValue: true },
      stock: { integerValue: String(stock) },
      unit: { stringValue: "kg" },
      pricePerKg: { integerValue: "100" },
      name: { stringValue: "Tomato" },
      nameMr: { stringValue: "टोमॅटो" },
      nameHi: { stringValue: "टमाटर" },
      nameEn: { stringValue: "Tomato" },
    },
  };
}

async function postOrder(env, order, tracking) {
  return worker.fetch(new Request("https://worker.test/submit-cod-order", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ order, tracking }),
  }), env);
}

test("COD accepts a valid 10-digit mobile and saves a server-priced order", async () => {
  const { env, order, tracking } = fixture();
  let committed;
  globalThis.fetch = async (input, init = {}) => {
    const url = String(input);
    if (url.includes("oauth2.googleapis.com/token")) {
      return Response.json({ access_token: "test-token" });
    }
    if (url.includes("/products/tomato-1")) return Response.json(firestoreProduct());
    if (url.endsWith(":commit")) {
      committed = JSON.parse(init.body);
      return Response.json({ writeResults: [{}, {}] });
    }
    throw new Error("Unexpected URL: " + url);
  };

  const response = await postOrder(env, order, tracking);
  const result = await response.json();
  assert.equal(response.status, 200);
  assert.equal(result.saved, true);
  assert.equal(result.totalAmountRupees, 70);
  const saved = committed.writes[0].update.fields;
  assert.equal(saved.totalAmountRupees.integerValue, "70");
  assert.match(saved.itemsSummaryMarathi.stringValue, /टोमॅटो/);
  assert.ok(saved.itemsSummaryMarathi.stringValue.includes("\n"));
  assert.equal(committed.writes.length, 3);
  assert.equal(committed.writes[2].update.name, "https://firestore.googleapis.com/v1/projects/annapurna-vegetables-and-78cef/databases/(default)/documents/products/tomato-1");
  assert.equal(committed.writes[2].update.fields.stock.doubleValue, 9.5);
  assert.equal(committed.writes[2].currentDocument.updateTime, "2026-11-13T00:00:00.000000Z");
});

test("COD aggregates repeated product lines into one atomic stock reservation", async () => {
  const { env, order, tracking } = fixture();
  order.items = [
    { productId: "tomato-1", quantity: 1, weightGrams: 500 },
    { productId: "tomato-1", quantity: 1, weightGrams: 500 },
  ];
  let committed;
  let productReads = 0;
  globalThis.fetch = async (input, init = {}) => {
    const url = String(input);
    if (url.includes("oauth2.googleapis.com/token")) return Response.json({ access_token: "test-token" });
    if (url.includes("/products/tomato-1")) {
      productReads++;
      return Response.json(firestoreProduct(10));
    }
    if (url.endsWith(":commit")) {
      committed = JSON.parse(init.body);
      return Response.json({ writeResults: [{}, {}, {}] });
    }
    throw new Error("Unexpected URL: " + url);
  };
  const response = await postOrder(env, order, tracking);
  assert.equal(response.status, 200);
  assert.equal(productReads, 1);
  assert.equal(committed.writes.length, 3);
  const remainingStock = committed.writes[2].update.fields.stock;
  assert.equal(Number(remainingStock.integerValue ?? remainingStock.doubleValue), 9);
});

test("COD reports a retry when Firestore detects a concurrent stock update", async () => {
  const { env, order, tracking } = fixture();
  globalThis.fetch = async (input) => {
    const url = String(input);
    if (url.includes("oauth2.googleapis.com/token")) return Response.json({ access_token: "test-token" });
    if (url.includes("/products/tomato-1")) return Response.json(firestoreProduct(10));
    if (url.endsWith(":commit")) {
      return Response.json({ error: { status: "FAILED_PRECONDITION" } }, { status: 409 });
    }
    throw new Error("Unexpected URL: " + url);
  };
  const response = await postOrder(env, order, tracking);
  const result = await response.json();
  assert.equal(response.status, 409);
  assert.match(result.error, /stock changed/i);
});

test("COD rejects an invalid mobile before contacting Firebase", async () => {
  const { env, order, tracking } = fixture();
  order.mobileNumber = "12345";
  globalThis.fetch = async () => { throw new Error("Firebase must not be called"); };
  const response = await postOrder(env, order, tracking);
  assert.equal(response.status, 400);
  assert.equal((await response.json()).saved, false);
});

test("COD rejects an out-of-stock product and does not commit an order", async () => {
  const { env, order, tracking } = fixture();
  globalThis.fetch = async (input) => {
    const url = String(input);
    if (url.includes("oauth2.googleapis.com/token")) return Response.json({ access_token: "test-token" });
    if (url.includes("/products/tomato-1")) return Response.json(firestoreProduct(0));
    throw new Error("Order must not be committed");
  };
  const response = await postOrder(env, order, tracking);
  assert.equal(response.status, 409);
  assert.match((await response.json()).error, /out of stock/i);
});

function paidFixture(clientTotal = 70) {
  const { env, order, tracking } = fixture();
  env.RAZORPAY_KEY_ID = "rzp_test_key";
  env.RAZORPAY_KEY_SECRET = "test-secret";
  order.orderId = "test-paid-order-0002";
  order.trackingToken = "223e4567-e89b-42d3-a456-426614174001";
  order.totalAmountRupees = clientTotal;
  order.paymentMethod = "Razorpay";
  order.paymentStatus = "paid";
  order.transactionId = "pay_test_0002";
  tracking.orderId = order.orderId;
  tracking.createdAtMillis = order.createdAtMillis;
  const razorpayOrderId = "order_test_0002";
  const amount = Math.round(clientTotal * 100);
  const signature = createHmac("sha256", env.RAZORPAY_KEY_SECRET)
    .update(razorpayOrderId + "|" + order.transactionId)
    .digest("hex");
  return {
    env,
    order,
    tracking,
    body: {
      order,
      tracking,
      order_id: razorpayOrderId,
      razorpay_payment_id: order.transactionId,
      razorpay_signature: signature,
      amount,
    },
  };
}

async function postPaidOrder(env, body) {
  return worker.fetch(new Request("https://worker.test/submit-paid-order", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  }), env);
}

test("paid order is saved only when current catalog prices match the captured payment", async () => {
  const { env, order, tracking, body } = paidFixture();
  let committed;
  globalThis.fetch = async (input, init = {}) => {
    const url = String(input);
    if (url.includes("/v1/payments/")) {
      return Response.json({
        order_id: body.order_id,
        amount: body.amount,
        currency: "INR",
        status: "captured",
        captured: true,
      });
    }
    if (url.includes("oauth2.googleapis.com/token")) return Response.json({ access_token: "test-token" });
    if (url.includes("/products/tomato-1")) return Response.json(firestoreProduct());
    if (url.endsWith(":commit")) {
      committed = JSON.parse(init.body);
      return Response.json({ writeResults: [{}, {}] });
    }
    throw new Error("Unexpected URL: " + url);
  };
  const response = await postPaidOrder(env, body);
  const result = await response.json();
  assert.equal(response.status, 200);
  assert.equal(result.saved, true);
  assert.equal(committed.writes[0].update.fields.totalAmountRupees.integerValue, "70");
  assert.equal(committed.writes[0].update.fields.items.arrayValue.values[0].mapValue.fields.lineTotalRupees.integerValue, "50");
  assert.equal(committed.writes.length, 3);
  assert.equal(committed.writes[2].update.fields.stock.doubleValue, 9.5);
  assert.equal(committed.writes[2].currentDocument.updateTime, "2026-11-13T00:00:00.000000Z");
});

test("paid order is rejected when the client total differs from current catalog pricing", async () => {
  const { env, body } = paidFixture(60);
  let committed = false;
  globalThis.fetch = async (input, init = {}) => {
    const url = String(input);
    if (url.includes("/v1/payments/")) {
      return Response.json({
        order_id: body.order_id,
        amount: body.amount,
        currency: "INR",
        status: "captured",
        captured: true,
      });
    }
    if (url.includes("oauth2.googleapis.com/token")) return Response.json({ access_token: "test-token" });
    if (url.includes("/products/tomato-1")) return Response.json(firestoreProduct());
    if (url.endsWith(":commit")) {
      committed = true;
      return Response.json({ writeResults: [{}, {}] });
    }
    throw new Error("Unexpected URL: " + url);
  };
  const response = await postPaidOrder(env, body);
  assert.equal(response.status, 400);
  assert.match((await response.json()).error, /current product prices/i);
  assert.equal(committed, false);
});
