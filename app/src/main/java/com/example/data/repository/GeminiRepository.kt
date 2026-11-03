package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.data.model.ProduceCatalog
import com.example.data.remote.BackendConfig
import com.example.data.remote.GeminiApiService
import com.example.data.remote.ProxyChatRequest
import com.example.data.remote.ProxyChatTurnDto
import com.example.data.remote.ProxyImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import retrofit2.HttpException

enum class ChatModelTier(
    val modelId: String,
    val marathiTitle: String,
    val marathiSubtitle: String
) {
    FAST(
        modelId = "gemini-3.1-flash-lite",
        marathiTitle = "झटपट (Fast)",
        marathiSubtitle = "त्वरीत उत्तरांसाठी • gemini-3.1-flash-lite"
    ),
    GENERAL(
        modelId = "gemini-3.8-flash",
        marathiTitle = "सर्वसाधारण (General)",
        marathiSubtitle = "दैनंदिन मदतीसाठी • gemini-3.8-flash"
    ),
    COMPLEX(
        modelId = "gemini-4-argon",
        marathiTitle = "सखोल तज्ञ (Complex)",
        marathiSubtitle = "सविस्तर नियोजनासाठी • gemini-4-argon"
    )
}

enum class StudioImageSize(val apiValue: String, val marathiLabel: String) {
    SIZE_1K("1K", "1K (मानक HD)"),
    SIZE_2K("2K", "2K (अति-स्पष्ट)"),
    SIZE_4K("4K", "4K (अल्ट्रा स्टुडिओ)")
}

enum class StudioAspectRatio(val apiValue: String, val marathiLabel: String) {
    SQUARE("1:1", "१:१ चौकोनी"),
    FOUR_THREE("4:3", "४:३ कार्ड"),
    WIDE("16:9", "१६:९ बॅनर")
}

data class ChatTurn(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelUsed: String? = null
)

data class GeneratedStudioImage(
    val bitmap: Bitmap,
    val captionText: String?,
    val imageSize: StudioImageSize,
    val aspectRatio: StudioAspectRatio,
    val prompt: String
)

class GeminiRepository(
    private val backendBaseUrl: String = BackendConfig.BACKEND_BASE_URL
) {
    companion object {
        private const val TAG = "GeminiRepository"
        const val IMAGE_GENERATION_MODEL = "gemini-3-pro-image-preview"
    }

    private val apiService: GeminiApiService? by lazy {
        val trimmed = backendBaseUrl.trim()
        if (trimmed.startsWith("https://")) {
            GeminiApiService.create(trimmed)
        } else {
            null
        }
    }

    private fun requireSecureBackendService(): Result<GeminiApiService> {
        val trimmed = backendBaseUrl.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "सुरक्षित बॅकएंड सर्व्हर URL अजून कॉन्फिगर केलेली नाही. सर्व API Keys सुरक्षेसाठी अँड्रॉइड APK मधून काढून सुरक्षित बॅकएंडवर (/backend) हलवल्या आहेत. कृपया बॅकएंड सर्व्हर डिप्लॉय करून त्याची HTTPS URL 'BackendConfig.BACKEND_BASE_URL' मध्ये जोडा."
                )
            )
        }
        if (!trimmed.startsWith("https://")) {
            return Result.failure(
                IllegalStateException("सुरक्षेसाठी फक्त 'https://' सुरक्षित बॅकएंड URL च स्वीकारली जाते.")
            )
        }
        val service = apiService ?: return Result.failure(
            IllegalStateException("बॅकएंड सेवा सुरू करता आली नाही.")
        )
        return Result.success(service)
    }

    private fun buildStoreSystemInstruction(cartSummary: String): String {
        val catalogLines = ProduceCatalog.items.joinToString(separator = "\n") { item ->
            "- ${item.marathiName}: ₹${item.priceRupees} प्रति ${item.unitMarathi} (${item.descriptionMarathi})"
        }
        return """
            तुम्ही 'अन्नपूर्णा भाजीपाला आणि फळे' (Annapurna Vegetable and Fruits) या अस्सल महाराष्ट्रीयन ताज्या भाजीपाला व फळांच्या दुकानाचे मनमिळाऊ, तज्ञ मराठी सहाय्यक आहात.
            तुमची भूमिका आणि नियम:
            १. नेहमी गोड, आदरयुक्त आणि सोप्या मराठी भाषेत संवाद साधा (ग्राहकाने इंग्रजीत विचारले तरी मराठीत किंवा आवश्यक असल्यास द्विभाषिक मदत करा).
            २. पाककृती विचारल्यास प्रथम थेट उत्तर द्या; अनावश्यक प्रश्न विचारू नका. एखाद्या भाजीचे किंवा फळाचे नाव दिल्यास त्यापासून बनणारे २-४ पदार्थ सुचवा आणि निवडलेल्या पदार्थासाठी लागणारे साहित्य, प्रमाण व क्रमवार कृती सोप्या मराठीत द्या. शक्य असल्यास महाराष्ट्रीयन घरगुती पद्धत सांगा. ग्राहकाने "यापासून काय बनवता येईल?" असे विचारल्यास उपलब्ध भाजी/फळ आणि खरेदी पिशवीतील वस्तू लक्षात घेऊन लगेच पाककृती सुचवा.
            ३. ग्राहकांना आजच्या ताज्या भाज्या-फळांचे दर सांगणे, भाजीपाला जास्त दिवस ताजा ठेवण्याच्या घरगुती टिप्स देणे आणि सामान्य अन्नविषयक माहिती देणे हेही तुमचे काम आहे. आरोग्यविषयक प्रश्नांना वैद्यकीय निदान न देता सावध, सामान्य माहिती द्या.
            ४. दुकानातील आजची उपलब्ध यादी व दर:
            $catalogLines
            ५. ग्राहकाच्या खरेदी पिशवीतील सद्यस्थिती:
            $cartSummary
            ६. उत्तरे सुटसुटीत, मुद्देसूद आणि वाचायला सोपी ठेवा. पाककृतीच्या उत्तरात साहित्य व कृती वेगवेगळी मांडून लगेच उपयोगी उत्तर द्या.
        """.trimIndent()
    }

    suspend fun sendMultiTurnChat(
        history: List<ChatTurn>,
        selectedTier: ChatModelTier,
        cartSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val service = requireSecureBackendService().getOrElse { return@withContext Result.failure(it) }

        val messages = history.map { turn ->
            ProxyChatTurnDto(
                role = if (turn.isUser) "user" else "model",
                text = turn.text
            )
        }

        val request = ProxyChatRequest(
            model = selectedTier.modelId,
            systemInstruction = buildStoreSystemInstruction(cartSummary),
            messages = messages
        )

        try {
            val response = withTimeout(60_000L) {
                service.chatWithAssistant(request)
            }
            if (!response.error.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException(response.error))
            }
            val replyText = response.reply?.trim()
            if (!replyText.isNullOrBlank()) {
                Result.success(replyText)
            } else {
                Result.failure(IllegalStateException("बॅकएंडकडून रिकामे उत्तर आले. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Backend chat request timed out", e)
            Result.failure(IllegalStateException("विनंतीला ६० सेकंदांपेक्षा जास्त वेळ लागला. कृपया पुन्हा प्रयत्न करा."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val detail = parseHttpErrorMessage(e)
            Log.e(TAG, "Backend chat HTTP error: $detail", e)
            Result.failure(IllegalStateException(detail))
        } catch (e: Exception) {
            Log.e(TAG, "Backend chat failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "सुरक्षित बॅकएंडशी संपर्क साधताना अडचण आली."))
        }
    }

    suspend fun generateStudioImage(
        prompt: String,
        imageSize: StudioImageSize,
        aspectRatio: StudioAspectRatio
    ): Result<GeneratedStudioImage> = withContext(Dispatchers.IO) {
        val service = requireSecureBackendService().getOrElse { return@withContext Result.failure(it) }

        val request = ProxyImageRequest(
            model = IMAGE_GENERATION_MODEL,
            prompt = prompt,
            imageSize = imageSize.apiValue,
            aspectRatio = aspectRatio.apiValue
        )

        try {
            val response = withTimeout(90_000L) {
                service.generateStudioImage(request)
            }
            if (!response.error.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException(response.error))
            }
            val base64Data = response.imageBase64
            if (base64Data.isNullOrBlank()) {
                val fallbackMsg = response.captionText
                    ?: "बॅकएंडकडून फोटो डेटा प्राप्त झाला नाही. कृपया वेगळे वर्णन देऊन प्रयत्न करा."
                return@withContext Result.failure(IllegalStateException(fallbackMsg))
            }

            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ?: return@withContext Result.failure(
                    IllegalStateException("प्राप्त फोटो डीकोड करता आला नाही.")
                )

            Result.success(
                GeneratedStudioImage(
                    bitmap = bitmap,
                    captionText = response.captionText,
                    imageSize = imageSize,
                    aspectRatio = aspectRatio,
                    prompt = prompt
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Backend image generation timed out", e)
            Result.failure(
                IllegalStateException("फोटो तयार करण्यास वेळ संपला (९० सेकंद). कृपया 1K किंवा 2K निवडून पुन्हा प्रयत्न करा.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val detail = parseHttpErrorMessage(e)
            Log.e(TAG, "Backend image generation HTTP error: $detail", e)
            Result.failure(IllegalStateException(detail))
        } catch (e: Exception) {
            Log.e(TAG, "Backend image generation failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "फोटो तयार करताना त्रुटी आली."))
        }
    }

    private fun parseHttpErrorMessage(e: HttpException): String {
        return try {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isNotBlank()) {
                val json = JSONObject(raw)
                val apiMsg = json.optString("error").ifBlank {
                    json.optJSONObject("error")?.optString("message").orEmpty()
                }
                if (apiMsg.isNotBlank()) {
                    "बॅकएंड त्रुटी (${e.code()}): $apiMsg"
                } else {
                    "सर्व्हर त्रुटी (${e.code()})"
                }
            } else {
                "सर्व्हर त्रुटी (${e.code()}): ${e.message()}"
            }
        } catch (_: Exception) {
            "सर्व्हर त्रुटी (${e.code()}): ${e.message()}"
        }
    }
}
