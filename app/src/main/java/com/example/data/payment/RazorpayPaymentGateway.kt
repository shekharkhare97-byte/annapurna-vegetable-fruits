package com.example.data.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        var lastError: IOException? = null
        // Razorpay may briefly report an authorization before capture completes.
        // Retry only that transient state; never retry an invalid signature or amount.
        repeat(5) { attempt ->
            val request = Request.Builder()
                .url("$BASE_URL/verify-payment")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val retryCapture = response.use {
                val body = it.body?.string().orEmpty()
                val data = runCatching { JSONObject(body) }.getOrElse {
                    throw IOException("Payment verification returned an invalid response.")
                }
                if (!it.isSuccessful) {
                    val message = data.optString("error").ifBlank { "Payment verification failed." }
                    if (message.contains("not captured", ignoreCase = true) && attempt < 4) {
                        lastError = IOException(message)
                        true
                    } else {
                        throw IOException(message)
                    }
                } else {
                    return@withContext data.optBoolean("verified", false)
                }
            }
            if (retryCapture) delay(1500)
        }
        throw lastError ?: IOException("Payment capture could not be confirmed. Please check the payment status before retrying.")
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

        var lastError: IOException? = null
        // The Worker makes duplicate submissions idempotent for the same captured payment.
        // Retry only transient network/server failures; never retry validation or payment errors.
        repeat(3) { attempt ->
            try {
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
                    if (!it.isSuccessful) {
                        val message = data.optString("error").ifBlank { "Verified payment order could not be saved." }
                        if ((it.code >= 500 || it.code == 408 || it.code == 429) && attempt < 2) {
                            lastError = IOException(message)
                        } else {
                            throw IOException(message)
                        }
                    } else {
                        if (!data.optBoolean("verified", false) || !data.optBoolean("saved", false)) {
                            throw IOException(data.optString("error").ifBlank { "Verified payment order could not be saved." })
                        }
                        return@withContext true
                    }
                }
            } catch (error: IOException) {
                if (attempt >= 2) throw error
                lastError = error
            }
            delay(1500L * (attempt + 1))
        }
        throw lastError ?: IOException("Order could not be saved. Please contact support before making another payment.")
    }

}
