# Annapurna Razorpay backend

This Cloudflare Worker keeps the Razorpay Key Secret off the Android app.

## Endpoints
- `GET /health` — service health check.
- `POST /create-order` — creates an INR order. Send `{"amount": 12500, "receipt": "ann-order-123"}`; amount is in paise.
- `POST /verify-payment` — verifies the checkout signature and confirms with Razorpay that the payment belongs to the supplied order, matches the expected amount in paise, is INR, and is captured. Send `{"order_id":"order_...","razorpay_payment_id":"pay_...","razorpay_signature":"...","amount":12500}`.
- `POST /submit-cod-order` — validates customer details, reloads current product prices and stock from Firestore, and atomically writes the COD order, tracking record, and reserved stock.
- `POST /submit-paid-order` — re-verifies captured payment, validates catalog price and stock, and atomically writes the paid order, tracking record, and reserved stock.

## Deploy
1. Create a Cloudflare account and open **Workers & Pages**.
2. Deploy this folder as a Worker named `annapurna-razorpay` (or use Wrangler).
3. In Worker **Settings → Variables and Secrets**, add these as **Secret** values:
   - `RAZORPAY_KEY_ID` — the Razorpay Key ID (use Test keys first).
   - `RAZORPAY_KEY_SECRET` — the matching Key Secret. Never add this to GitHub or the Android app.
   - `FIREBASE_SERVICE_ACCOUNT_JSON` — the full Firebase service-account JSON contents for project `annapurna-vegetables-and-78cef`, stored as a Worker Secret. Never commit or share this file.
4. Deploy the Worker and test `/health`. Save the Worker URL for the Android app configuration.

### GitHub Actions deployment (manual)

A manual workflow is available at **Actions → Deploy Annapurna Razorpay Worker → Run workflow**. Before running it, add these repository Actions secrets under **Settings → Secrets and variables → Actions**:

- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN` (with permission to edit/deploy Workers)
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `FIREBASE_SERVICE_ACCOUNT_JSON` (the complete service-account JSON for project `annapurna-vegetables-and-78cef`)

The workflow is manual-only; it will not deploy on every code push. Never paste secret values into chat, source files, or commits.

## Important
- Use Razorpay Test Mode first, complete a full payment and signature-verification test, then configure Live keys.
- The app must only mark an online order as paid after `/verify-payment` returns `verified: true`.
- COD checkout requires this Worker to be deployed with `FIREBASE_SERVICE_ACCOUNT_JSON`; do not bypass the trusted stock and price validation with direct client writes.
- Both order submission endpoints reserve stock in the same Firestore commit as the order and tracking documents. Product update-time preconditions prevent two simultaneous checkouts from reserving the same units.
- Orders written by this Worker include `stockReservedByServer: true`; Admin must not deduct their stock again when accepting them. If Admin cancels an accepted order, it restores the reserved stock.
- The public Android client cannot create a `paid` order directly; public Firestore order creation is restricted to pending COD orders.
