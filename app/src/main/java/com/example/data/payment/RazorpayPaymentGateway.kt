package com.example.data.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Public client for the Annapurna Cloudflare payment Worker.
 * Razorpay secret keys are never stored in the Android application.
 */
object RazorpayPaymentGateway {
    private const val BASE_URL = "https://annapurna-razorpay.shekharkhare97.workers.dev"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    data class CreatedOrder(
        val orderId: String,
        val amountPaise: Long,
        val currency: String,
        val keyId: String
    )

    suspend fun createOrder(amountPaise: Long): CreatedOrder = withContext(Dispatchers.IO) {
        require(amountPaise > 0) { "Payment amount must be greater than zero." }
        val payload = JSONObject().put("amount", amountPaise)
        val request = Request.Builder()
            .url("$BASE_URL/create-order")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            val data = runCatching { JSONObject(body) }.getOrElse {
                throw IOException("Payment server returned an invalid response.")
            }
            if (!it.isSuccessful) {
                throw IOException(data.optString("error").ifBlank { "Unable to create payment order." })
            }
            val orderId = data.optString("order_id")
            val keyId = data.optString("key_id")
            if (orderId.isBlank() || keyId.isBlank()) {
                throw IOException("Payment server response is incomplete.")
            }
            CreatedOrder(
                orderId = orderId,
                amountPaise = data.optLong("amount", amountPaise),
                currency = data.optString("currency", "INR"),
                keyId = keyId
            )
        }
    }

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        amountPaise: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("order_id", orderId)
            .put("razorpay_payment_id", paymentId)
            .put("razorpay_signature", signature)
            .put("amount", amountPaise)
        val request = Request.Builder()
            .url("$BASE_URL/verify-payment")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            val data = runCatching { JSONObject(body) }.getOrElse {
                throw IOException("Payment verification returned an invalid response.")
            }
            if (!it.isSuccessful) {
                throw IOException(data.optString("error").ifBlank { "Payment verification failed." })
            }
            data.optBoolean("verified", false)
        }
    }
    /** Sends COD orders to the trusted Worker, which reloads current product prices and validates stock. */
    suspend fun submitCodOrder(
        order: Map<String, Any>,
        tracking: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("order", JSONObject(order))
            .put("tracking", JSONObject(tracking))
        val request = Request.Builder()
            .url("$BASE_URL/submit-cod-order")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            val data = runCatching { JSONObject(body) }.getOrElse {
                throw IOException("Order server returned an invalid response.")
            }
            if (!it.isSuccessful || !data.optBoolean("saved", false)) {
                throw IOException(data.optString("error").ifBlank { "COD order could not be saved." })
            }
            data.optBoolean("saved", false)
        }
    }

    /** Verifies a captured Razorpay payment and atomically saves its order + public tracking record. */
    suspend fun submitPaidOrder(
        orderId: String,
        paymentId: String,
        signature: String,
        amountPaise: Long,
        order: Map<String, Any>,
        tracking: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("order_id", orderId)
            .put("razorpay_payment_id", paymentId)
            .put("razorpay_signature", signature)
            .put("amount", amountPaise)
            .put("order", JSONObject(order))
            .put("tracking", JSONObject(tracking))
        val request = Request.Builder()
            .url("$BASE_URL/submit-paid-order")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            val data = runCatching { JSONObject(body) }.getOrElse {
                throw IOException("Order server returned an invalid response.")
            }
            if (!it.isSuccessful || !data.optBoolean("saved", false)) {
                throw IOException(data.optString("error").ifBlank { "Verified payment order could not be saved." })
            }
            data.optBoolean("verified", false)
        }
    }

}
