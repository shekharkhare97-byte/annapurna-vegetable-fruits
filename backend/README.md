# Annapurna Backend

This Node.js 20+ service provides the AI proxy and the shared shop API. Shop stock and customer orders are stored in **Google Cloud Firestore**, not on the Cloud Run container's temporary disk.

## Shop API

- `GET /api/shop/products` — public list of available product stock and selling prices.
- `POST /api/shop/orders` — customer submits an order. Required: `customerName`, `mobileNumber`, `deliveryAddress`, and `items`. Each item contains `productId`, `productName`, and positive `quantityGrams`.
- `GET /api/shop/orders` — owner-only list of incoming orders.
- `PUT /api/shop/products/:productId` — owner-only stock and price update. Body: `name`, `stockGrams`, `sellingPricePaisePerKg`.
- `PATCH /api/shop/orders/:orderId` — owner-only status update. Status: `ACCEPTED`, `REJECTED`, or `COMPLETED`.
- `GET /api/health` — health check.

Owner endpoints require `Authorization: Bearer <ANNAPURNA_OWNER_TOKEN>`. Customer order submission and product listing do not require the owner token. The service applies a per-IP request limit; configure additional abuse protection before public production use.

## Google Cloud Run + Firestore setup

1. Create or select a Google Cloud project and enable the Firestore API.
2. Create a Firestore database in Native mode.
3. Give the Cloud Run runtime service account the **Cloud Datastore User** role on that project.
4. Create a long random owner token and store it as the Cloud Run environment secret `ANNAPURNA_OWNER_TOKEN`. Keep it private; never commit it to Git or place it in the customer APK.
5. Store the Gemini API key in Secret Manager as `annapurna-gemini-key` if AI chat/image features are required.
6. Deploy from this `backend/` directory, setting `GOOGLE_CLOUD_PROJECT`, `ANNAPURNA_OWNER_TOKEN`, and (for AI features) `GEMINI_SERVER_API_KEY`.

Example deployment (replace the project and service account values with your own):

```bash
gcloud run deploy annapurna-backend \
  --source . \
  --region asia-south1 \
  --set-env-vars="GOOGLE_CLOUD_PROJECT=YOUR_PROJECT_ID" \
  --set-secrets="ANNAPURNA_OWNER_TOKEN=annapurna-owner-token:latest,GEMINI_SERVER_API_KEY=annapurna-gemini-key:latest"
```

The Cloud Run service account must have Firestore access. The example enables unauthenticated access because the customer app needs to submit orders and read products; owner-only endpoints still require the separate owner token. Keep that token private and do not put it in the customer APK. The mobile app must use the deployed HTTPS URL.

## AI proxy

- `POST /api/chat`
- `POST /api/generate-image`

Gemini credentials are read only from the server environment / Secret Manager and are never embedded in the Android APK.

## Important integration status

The shared server endpoints are being added first. The Android screens still need to be connected to these endpoints, and the backend must be deployed before customer orders can reach the shop owner's phone. A successful APK build alone does not activate cloud order synchronization.
