import test, { afterEach } from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
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
