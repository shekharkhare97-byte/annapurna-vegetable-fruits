package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.ChatModelTier
import com.example.data.repository.ChatTurn
import com.example.ui.viewmodel.ChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarathiChatScreen(
    chatState: ChatUiState,
    startAction: String? = null,
    onStartActionConsumed: () -> Unit = {},
    onSelectModelTier: (ChatModelTier) -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?, String?, String?) -> Unit,
    onResetChat: () -> Unit,
    onBackToBazaar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToBazaar)

    val context = LocalContext.current
    val language = context.resources.configuration.locales[0]?.language ?: "mr"
    fun tr(mr: String, hi: String, en: String) = when (language) { "hi" -> hi; "en" -> en; else -> mr }
    val chatSuggestions = listOf(
        tr("भरली वांगी बनवण्याची सोपी पाककृती सांगा", "भरवाँ बैंगन बनाने की आसान रेसिपी बताइए", "Tell me an easy recipe for stuffed eggplant"),
        tr("माझ्या पिशवीतील भाज्यांपासून कोणता पदार्थ बनेल?", "मेरे पास मौजूद सब्ज़ियों से कौन-सी डिश बनेगी?", "What can I cook with the vegetables I have?"),
        tr("पालेभाज्या जास्त दिवस ताज्या कशा ठेवाव्यात?", "पत्तेदार सब्ज़ियों को अधिक दिनों तक ताज़ा कैसे रखें?", "How can I keep leafy greens fresh longer?"),
        tr("रोगप्रतिकारशक्ती वाढवण्यासाठी कोणती फळे खावीत?", "रोग प्रतिरोधक क्षमता के लिए कौन-से फल खाएँ?", "Which fruits support immunity?")
    )
    val speechLanguage = context.resources.configuration.locales[0].toLanguageTag()
    var listening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException(tr("फोटो वाचता आला नाही.", "फ़ोटो पढ़ा नहीं जा सका।", "Could not read the photo."))
                if (bytes.size > 4 * 1024 * 1024) {
                    speechError = tr("कृपया 4 MB पेक्षा लहान फोटो निवडा.", "कृपया 4 MB से छोटी फ़ोटो चुनें।", "Please choose a photo smaller than 4 MB.")
                } else {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val prompt = chatState.inputText.ifBlank { tr("या फोटोतील वस्तू/भाजी/फळ ओळखा आणि माहिती सांगा.", "इस फ़ोटो में मौजूद वस्तु/सब्ज़ी/फल पहचानकर जानकारी दें।", "Identify the item, vegetable or fruit in this photo and describe it.") }
                    onSendMessage(prompt, Base64.encodeToString(bytes, Base64.NO_WRAP), mime)
                }
            } catch (e: Exception) {
                speechError = e.localizedMessage ?: tr("फोटो पाठवता आला नाही.", "फ़ोटो भेजा नहीं जा सका।", "Could not send the photo.")
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguage)
            }
            try {
                recognizer?.startListening(speechIntent)
                listening = true
            } catch (_: Exception) {
                speechError = tr("माइक सुरू करता आला नाही. पुन्हा प्रयत्न करा.", "माइक शुरू नहीं हो सका। फिर कोशिश करें।", "Could not start the microphone. Please try again.")
            }
        } else {
            speechError = tr("बोलून प्रश्न विचारण्यासाठी Microphone परवानगी द्या.", "बोलकर सवाल पूछने के लिए माइक्रोफ़ोन की अनुमति दें।", "Allow microphone access to ask a question by voice.")
        }
    }

    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) {
                    listening = false
                    speechError = tr("आवाज समजला नाही. पुन्हा बोला किंवा टाइप करा.", "आवाज़ समझ नहीं आई। फिर बोलें या टाइप करें।", "Speech was not understood. Speak again or type your question.")
                }
                override fun onResults(results: android.os.Bundle?) {
                    listening = false
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!words.isNullOrBlank()) onInputChange(
                        listOf(chatState.inputText, words).filter { it.isNotBlank() }.joinToString(" ")
                    )
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            recognizer = sr
        } else {
            speechError = tr("या मोबाईलमध्ये Speech Recognition उपलब्ध नाही.", "इस फ़ोन में स्पीच रिकग्निशन उपलब्ध नहीं है।", "Speech recognition is unavailable on this phone.")
        }
        onDispose {
            recognizer?.destroy()
            recognizer = null
            listening = false
        }
    }

    // Home-screen shortcuts open the exact input tool instead of leaving the
    // customer to find the microphone or photo button inside the chat.
    LaunchedEffect(startAction) {
        when (startAction) {
            "photo" -> {
                speechError = null
                photoPicker.launch("image/*")
                onStartActionConsumed()
            }
            "voice" -> {
                speechError = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguage)
                    }
                    try {
                        recognizer?.startListening(speechIntent)
                        listening = true
                    } catch (_: Exception) {
                        speechError = tr("माइक सुरू करता आला नाही. पुन्हा प्रयत्न करा.", "माइक शुरू नहीं हो सका। फिर कोशिश करें।", "Could not start the microphone. Please try again.")
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                onStartActionConsumed()
            }
        }
    }

    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.forLanguageTag(speechLanguage)
            }
        }
        tts.value = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts.value = null
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size, chatState.isSending) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header + Model Selector Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.chat_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = chatState.selectedModelTier.marathiSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onResetChat,
                        modifier = Modifier.testTag("reset_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.chat_clear_desc)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val tiers = ChatModelTier.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tiers.forEachIndexed { index, tier ->
                        SegmentedButton(
                            selected = chatState.selectedModelTier == tier,
                            onClick = { onSelectModelTier(tier) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tiers.size
                            ),
                            modifier = Modifier.testTag("chat_model_${tier.name.lowercase()}")
                        ) {
                            Text(
                                text = tier.marathiTitle,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Multi-Turn Conversation Thread
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list")
        ) {
            items(chatState.messages, key = { it.id }) { turn ->
                ChatBubbleItem(turn = turn, onSpeak = { spokenText ->
                    tts.value?.let { engine ->
                        engine.language = Locale.forLanguageTag(speechLanguage)
                        engine.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, turn.id)
                    }
                })
            }

            if (chatState.isSending) {
                item(key = "typing_indicator") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.chat_typing_indicator),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (chatState.errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = chatState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Quick Marathi Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(chatSuggestions) { suggestion ->
                AssistChip(
                    onClick = { onSendMessage(suggestion, null, null) },
                    enabled = !chatState.isSending,
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        if (speechError != null) {
            Text(
                text = speechError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Message Composer Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = { photoPicker.launch("image/*") },
                enabled = !chatState.isSending
            ) { Text("📷") }
            OutlinedTextField(
                value = chatState.inputText,
                onValueChange = onInputChange,
                maxLines = 3,
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_input_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage(null, null, null) }
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilledIconButton(
                onClick = {
                    speechError = null
                    if (listening) {
                        recognizer?.stopListening()
                        listening = false
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguage)
                        }
                        try {
                            recognizer?.startListening(speechIntent)
                            listening = true
                        } catch (_: Exception) {
                            speechError = tr("माइक सुरू करता आला नाही. पुन्हा प्रयत्न करा.", "माइक शुरू नहीं हो सका। फिर कोशिश करें।", "Could not start the microphone. Please try again.")
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !chatState.isSending,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (listening) tr("आवाज थांबवा", "आवाज़ रोकें", "Stop listening") else tr("बोलून प्रश्न विचारा", "बोलकर सवाल पूछें", "Ask by voice"),
                    tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            FilledIconButton(
                onClick = { onSendMessage(null, null, null) },
                enabled = !chatState.isSending && chatState.inputText.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send_message)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(turn: ChatTurn, onSpeak: (String) -> Unit) {
    val alignment = if (turn.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (turn.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val contentColor = if (turn.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleShape = if (turn.isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(
        contentAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = bubbleShape,
            shadowElevation = if (turn.isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = turn.text,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!turn.isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSpeak(turn.text) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = when (androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language) { "hi" -> "जवाब सुनें"; "en" -> "Listen to answer"; else -> "उत्तर ऐका" },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (turn.modelUsed != null) {
                            Text(
                                text = turn.modelUsed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
