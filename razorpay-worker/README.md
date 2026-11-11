# Annapurna Razorpay backend

This Cloudflare Worker keeps the Razorpay Key Secret off the Android app.

## Endpoints
- `GET /health` — service health check.
- `POST /create-order` — creates an INR order. Send `{"amount": 12500, "receipt": "ann-order-123"}`; amount is in paise.
- `POST /verify-payment` — verifies the checkout signature and confirms with Razorpay that the payment belongs to the supplied order, matches the expected amount in paise, is INR, and is captured. Send `{"order_id":"order_...","razorpay_payment_id":"pay_...","razorpay_signature":"...","amount":12500}`.

## Deploy
1. Create a Cloudflare account and open **Workers & Pages**.
2. Deploy this folder as a Worker named `annapurna-razorpay` (or use Wrangler).
3. In Worker **Settings → Variables and Secrets**, add these as **Secret** values:
   - `RAZORPAY_KEY_ID` — the Razorpay Key ID (use Test keys first).
   - `RAZORPAY_KEY_SECRET` — the matching Key Secret. Never add this to GitHub or the Android app.
4. Deploy the Worker and test `/health`. Save the Worker URL for the Android app configuration.

## Important
- Use Razorpay Test Mode first, complete a full payment and signature-verification test, then configure Live keys.
- The app must only mark an online order as paid after `/verify-payment` returns `verified: true`.
- The app should still allow Cash on Delivery when the Worker is not configured.
- This Worker does not store order records. The app/backend must save verified payment state to the store's order database before fulfilment.
