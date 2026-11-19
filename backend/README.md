# Annapurna Secure Backend Proxy

All sensitive AI credentials have been removed from the Android APK and moved exclusively to this server-side proxy.

## Endpoints
- `GET /api/health` — Liveness check
- `POST /api/chat` — Proxies multi-turn Marathi chat (`gemini-3.1-flash-lite`, `gemini-3.8-flash`, `gemini-4-argon`)
- `POST /api/generate-image` — Proxies high-resolution (`1K`, `2K`, `4K`) image generation (`gemini-3-pro-image-preview`)

## Cloud Run Deployment (Using Secret Manager)
1. Store your Gemini API credential in Google Cloud Secret Manager as `annapurna-gemini-key`.
2. Deploy from `/backend`:
   ```bash
   gcloud run deploy annapurna-ai-proxy \
     --source . \
     --region asia-south1 \
     --allow-unauthenticated \
     --set-secrets="GEMINI_SERVER_API_KEY=annapurna-gemini-key:latest"
   ```
3. Copy the resulting `https://...a.run.app/` URL into `BackendConfig.BACKEND_BASE_URL` in the Android app.

## Free-only Customer App deployment (Cloudflare Workers)

The Customer App can use the free Gemini chat tier without Google Cloud Secret Manager or a Google Cloud billing account by deploying `cloudflare-worker.js` to Cloudflare Workers Free.

1. Create a Cloudflare account and open **Workers & Pages**.
2. Create a Worker named `annapurna-customer-ai`.
3. Add a secret named `GEMINI_SERVER_API_KEY` in Worker settings. Never put the key in Android code or commit it to GitHub.
4. Verify `/api/health` returns JSON with `"status":"ok"`.
5. Set the Worker URL in `BackendConfig.BACKEND_BASE_URL` in the Android app, then build a new APK.

For Cloudflare Git-connected builds, use `backend` as the root directory, leave the build command blank, and use `npx wrangler deploy` as the deploy command. The Worker configuration is in `wrangler.toml`.

This free-only Worker allows chat models `gemini-3.8-flash` and `gemini-3.1-flash-lite`. Gemini API Free Tier availability and rate limits are controlled by Google and may change. Image generation is deliberately disabled in this free-only Worker because image generation is not part of this free chat setup.

Cloudflare Workers Free has usage limits; check the current plan details before deployment.
