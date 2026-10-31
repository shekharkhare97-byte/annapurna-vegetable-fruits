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
