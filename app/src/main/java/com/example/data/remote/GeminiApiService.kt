package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ProxyChatTurnDto(
    val role: String,
    val text: String
)

@JsonClass(generateAdapter = true)
data class ProxyChatRequest(
    val model: String,
    val systemInstruction: String,
    val messages: List<ProxyChatTurnDto>
)

@JsonClass(generateAdapter = true)
data class ProxyChatResponse(
    val reply: String? = null,
    val modelUsed: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ProxyImageRequest(
    val model: String,
    val prompt: String,
    val imageSize: String,
    val aspectRatio: String
)

@JsonClass(generateAdapter = true)
data class ProxyImageResponse(
    val imageBase64: String? = null,
    val mimeType: String? = null,
    val captionText: String? = null,
    val error: String? = null
)

/**
 * Keyless Retrofit client communicating exclusively with the secure HTTPS backend proxy.
 * Zero API keys or third-party credentials are stored in or transmitted from the Android APK.
 */
interface GeminiApiService {
    @POST("api/chat")
    suspend fun chatWithAssistant(
        @Body request: ProxyChatRequest
    ): ProxyChatResponse

    @POST("api/generate-image")
    suspend fun generateStudioImage(
        @Body request: ProxyImageRequest
    ): ProxyImageResponse

    companion object {
        fun create(baseUrl: String): GeminiApiService {
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder().build()

            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GeminiApiService::class.java)
        }
    }
}
