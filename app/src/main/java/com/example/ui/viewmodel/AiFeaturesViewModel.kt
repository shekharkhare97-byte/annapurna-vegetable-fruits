package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatModelTier
import com.example.data.repository.ChatTurn
import com.example.data.repository.GeminiRepository
import com.example.data.repository.GeneratedStudioImage
import com.example.data.repository.StudioAspectRatio
import com.example.data.repository.StudioImageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface StudioUiState {
    data object Idle : StudioUiState
    data class Generating(val size: StudioImageSize) : StudioUiState
    data class Success(val result: GeneratedStudioImage) : StudioUiState
    data class Error(val message: String) : StudioUiState
}

data class ChatUiState(
    val selectedModelTier: ChatModelTier = ChatModelTier.GENERAL,
    val messages: List<ChatTurn> = listOf(
        ChatTurn(
            id = "welcome_greeting",
            isUser = false,
            text = "नमस्कार! 🙏 'अन्नपूर्णा भाजीपाला आणि फळे' मध्ये तुमचे सहर्ष स्वागत आहे!\n\nमी तुमचा AI मराठी सहाय्यक आहे. तुम्ही मला आजच्या ताज्या भाज्यांचे दर, चविष्ट महाराष्ट्रीयन पाककृती (उदा. भरली वांगी, पावभाजी, आमरस), किंवा फळे-भाज्या ताज्या ठेवण्याच्या टिप्सबद्दल काहीही विचारू शकता!"
        )
    ),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class AiFeaturesViewModel(
    private val geminiRepository: GeminiRepository = GeminiRepository()
) : ViewModel() {

    // 1. Multi-Turn Marathi Chatbot State
    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    // 2. High-Quality Image Studio State (gemini-3-pro-image-preview)
    private val _selectedImageSize = MutableStateFlow(StudioImageSize.SIZE_1K)
    val selectedImageSize: StateFlow<StudioImageSize> = _selectedImageSize.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(StudioAspectRatio.SQUARE)
    val selectedAspectRatio: StateFlow<StudioAspectRatio> = _selectedAspectRatio.asStateFlow()

    private val _studioPrompt = MutableStateFlow("")
    val studioPrompt: StateFlow<String> = _studioPrompt.asStateFlow()

    private val _studioState = MutableStateFlow<StudioUiState>(StudioUiState.Idle)
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    fun selectModelTier(tier: ChatModelTier) {
        _chatState.update { it.copy(selectedModelTier = tier, errorMessage = null) }
    }

    fun updateChatInput(text: String) {
        _chatState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun resetConversation() {
        _chatState.update {
            ChatUiState(selectedModelTier = it.selectedModelTier)
        }
    }

    fun sendChatMessage(promptOverride: String? = null, cartSummary: String) {
        val currentState = _chatState.value
        if (currentState.isSending) return
        val messageText = (promptOverride ?: currentState.inputText).trim()
        if (messageText.isEmpty()) return

        val userTurn = ChatTurn(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = messageText
        )

        val updatedMessages = currentState.messages + userTurn
        _chatState.update {
            it.copy(
                messages = updatedMessages,
                inputText = if (promptOverride == null) "" else it.inputText,
                isSending = true,
                errorMessage = null
            )
        }

        // Build valid alternating multi-turn history starting from the first user turn
        val apiHistory = buildAlternatingUserFirstHistory(updatedMessages)
        val tier = currentState.selectedModelTier

        viewModelScope.launch {
            val result = geminiRepository.sendMultiTurnChat(
                history = apiHistory,
                selectedTier = tier,
                cartSummary = cartSummary
            )
            result.fold(
                onSuccess = { reply ->
                    val modelTurn = ChatTurn(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = reply,
                        modelUsed = tier.modelId
                    )
                    _chatState.update { state ->
                        state.copy(
                            messages = state.messages + modelTurn,
                            isSending = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _chatState.update { state ->
                        state.copy(
                            isSending = false,
                            errorMessage = error.message ?: "उत्तर मिळवताना त्रुटी आली."
                        )
                    }
                }
            )
        }
    }

    private fun buildAlternatingUserFirstHistory(allMessages: List<ChatTurn>): List<ChatTurn> {
        val firstUserIdx = allMessages.indexOfFirst { it.isUser }
        if (firstUserIdx == -1) return emptyList()
        val sliced = allMessages.subList(firstUserIdx, allMessages.size)
        val normalized = mutableListOf<ChatTurn>()
        for (turn in sliced) {
            val last = normalized.lastOrNull()
            if (last != null && last.isUser == turn.isUser) {
                // Merge consecutive same-role turns (e.g. if a previous request errored before model replied)
                normalized[normalized.lastIndex] = last.copy(text = "${last.text}\n${turn.text}")
            } else {
                normalized.add(turn)
            }
        }
        return normalized
    }

    // Studio Image Generation Actions
    fun selectStudioImageSize(size: StudioImageSize) {
        _selectedImageSize.value = size
    }

    fun selectStudioAspectRatio(ratio: StudioAspectRatio) {
        _selectedAspectRatio.value = ratio
    }

    fun updateStudioPrompt(prompt: String) {
        _studioPrompt.value = prompt
    }

    fun generateHighQualityImage() {
        val currentPrompt = _studioPrompt.value.trim()
        if (currentPrompt.isEmpty()) {
            _studioState.value = StudioUiState.Error("कृपया फोटो तयार करण्यासाठी मराठी किंवा इंग्रजीत वर्णन लिहा.")
            return
        }
        if (_studioState.value is StudioUiState.Generating) return

        val size = _selectedImageSize.value
        val ratio = _selectedAspectRatio.value

        viewModelScope.launch {
            _studioState.value = StudioUiState.Generating(size)
            val result = geminiRepository.generateStudioImage(
                prompt = currentPrompt,
                imageSize = size,
                aspectRatio = ratio
            )
            result.fold(
                onSuccess = { generated ->
                    _studioState.value = StudioUiState.Success(generated)
                },
                onFailure = { err ->
                    _studioState.value = StudioUiState.Error(
                        err.message ?: "उच्च दर्जाचा फोटो तयार करता आला नाही."
                    )
                }
            )
        }
    }
}
