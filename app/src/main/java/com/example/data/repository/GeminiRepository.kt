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
        modelId = "gemini-3.8-flash",
        marathiTitle = "सखोल तज्ञ (Complex)",
        marathiSubtitle = "सविस्तर नियोजनासाठी • gemini-3.8-flash"
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
            तुम्ही 'अन्नपूर्णा भाजीपाला आणि फळे' (Annapurna Vegetable and Fruits) चे मैत्रीपूर्ण, कुशल भारतीय स्वयंपाक आणि ताज्या भाज्या-फळांचे AI सहाय्यक आहात.
            मुख्य नियम:
            १. ग्राहकाने ज्या भाषेत शेवटचा प्रश्न विचारला आहे त्याच भाषेत उत्तर द्या: मराठी (देवनागरी किंवा रोमन मराठी), हिंदी किंवा इंग्रजी. भाषा बदलून उत्तर देऊ नका.
            २. ग्राहकाने Cart मधील भाज्या/फळांपासून काय बनवता येईल असे विचारल्यास खाली दिलेली Cart यादी आधी तपासा. त्यातील उपलब्ध साहित्य वापरून २-३ योग्य पदार्थ सुचवा. कोणते अतिरिक्त साहित्य लागेल ते वेगळे सांगा.
            ३. कोणत्याही भाजी, फळ किंवा पदार्थाची रेसिपी विचारल्यास साहित्य व प्रमाण, किती जणांसाठी, तयारी/शिजवण्याचा अंदाजे वेळ आणि क्रमवार सोपी कृती द्या. शक्य असल्यास घरगुती महाराष्ट्रीयन पद्धत आणि पर्यायही सांगा.
            ४. एखादी विशिष्ट भाजी वापरून रेसिपी विचारल्यास त्या भाजीला मुख्य घटक ठेवा. ग्राहकाच्या Cart मध्ये त्या भाजीचे प्रमाण कमी/जास्त असल्यास त्यानुसार प्रमाण सुचवा.
            ५. ग्राहकाला Cart मधील सर्व साहित्य वापरणे बंधनकारक नाही. उपलब्ध साहित्यावर आधारित उपयोगी पर्याय द्या; Cart मध्ये नसलेले साहित्य असल्याचा दावा करू नका.
            ६. दुकानातील उपलब्ध उत्पादनांची यादी व दाखवलेले दर:
            $catalogLines
            ७. ग्राहकाच्या सध्याच्या Cart मधील वस्तू व प्रमाण:
            $cartSummary
            ८. दर बदलू शकतात; खात्री नसलेले आजचे दर किंवा stock बनवून सांगू नका. वैद्यकीय सल्ला देऊ नका; अन्न/अॅलर्जीबद्दल शंका असल्यास ग्राहकाला काळजी घेण्यास सांगा.
            ९. ग्राहकाने खरेदीची यादी किंवा बजेट विचारल्यास दुकानातील उपलब्ध यादीतील वस्तू, प्रमाण आणि दिलेले दर वापरून तक्त्यासारखी यादी व अंदाजे एकूण रक्कम द्या. दर उपलब्ध नसल्यास अंदाज बनवू नका; त्या वस्तूचा दर विचारावा.\n            १०. ग्राहकाने फोटो पाठवला असल्यास फोटोतील वस्तू ओळखण्याचा प्रयत्न करा; भाजी/फळाचे नाव, दिसणारी अवस्था आणि स्वयंपाक/साठवणुकीचे उपयोग सांगा. फोटोवरून खात्री नसेल तर स्पष्टपणे अनिश्चितता सांगा.\n            ११. उत्तर सोपे, नम्र, मुद्देसूद आणि प्रत्यक्ष स्वयंपाकात उपयोगी ठेवा. गरज नसताना प्रश्न विचारू नका.
        """.trimIndent()
    }

    suspend fun sendMultiTurnChat(
        history: List<ChatTurn>,
        selectedTier: ChatModelTier,
        cartSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val service = requireSecureBackendService().getOrElse {
            return@withContext Result.failure(it)
        }
        if (history.none { it.isUser }) {
            return@withContext Result.failure(
                IllegalArgumentException("कृपया आधी तुमचा प्रश्न लिहा.")
            )
        }

        val request = ProxyChatRequest(
            model = selectedTier.modelId,
            systemInstruction = buildStoreSystemInstruction(cartSummary),
            messages = history.map { turn ->
                ProxyChatTurnDto(
                    role = if (turn.isUser) "user" else "model",
                    text = turn.text
                )
            }
        )

        try {
            val response = withTimeout(90_000L) {
                service.chatWithAssistant(request)
            }
            if (!response.error.isNullOrBlank()) {
                Result.failure(IllegalStateException(response.error))
            } else {
                val reply = response.reply?.trim()
                if (reply.isNullOrBlank()) {
                    Result.failure(IllegalStateException("AI कडून रिकामे उत्तर आले. कृपया पुन्हा प्रयत्न करा."))
                } else {
                    Result.success(reply)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Cloudflare AI chat request timed out", e)
            Result.failure(IllegalStateException("विनंतीला वेळ लागला. कृपया पुन्हा प्रयत्न करा."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val detail = parseHttpErrorMessage(e)
            Log.e(TAG, "Cloudflare AI chat HTTP error: $detail", e)
            Result.failure(IllegalStateException(detail))
        } catch (e: Exception) {
            Log.e(TAG, "Cloudflare AI chat failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Annapurna AI शी संपर्क साधताना अडचण आली."))
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
