package com.example.data.remote

/**
 * Public, non-secret configuration for the Annapurna Secure Backend Proxy.
 *
 * Sensitive Gemini credentials MUST NEVER be stored in the Android APK.
 * Deploy the included `/backend` service (e.g., to Google Cloud Run) with the
 * server-side secret configured in Cloud Secret Manager, then set [BACKEND_BASE_URL]
 * to your public `https://` service endpoint (ending with `/`).
 */
object BackendConfig {
    // Public HTTPS endpoint of your deployed `/backend` proxy service (e.g. "https://annapurna-proxy-xxx.a.run.app/")
    const val BACKEND_BASE_URL: String = ""
}
