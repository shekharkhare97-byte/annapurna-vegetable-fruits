package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.GeminiRepository
import com.example.data.repository.StudioAspectRatio
import com.example.data.repository.StudioImageSize
import com.example.ui.viewmodel.StudioUiState
import com.example.util.MarathiFormatters

private val studioMarathiPresets = listOf(
    "सणासाठी फुलांनी सजवलेली रत्नागिरी हापूस आंबे, डाळिंब आणि केळी यांची सुंदर परडी, स्टुडिओ लाईटिंग",
    "गरमागरम अस्सल महाराष्ट्रीयन भरली वांगी, ज्वारीची भाकरी आणि ताजा कांदा-लिंबू, केळीच्या पानावर",
    "शेतकऱ्याच्या मळ्यातील दवबिंदू असलेले लाल टोमॅटो, कोवळी भेंडी आणि हिरवागार पालक, बांबूची टोकरी",
    "ताज्या हापूस आंब्याचा घट्ट आमरस आणि फुगलेली गरमागरम पुरी, पारंपरिक पितळी वाटीत"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageStudioScreen(
    selectedSize: StudioImageSize,
    selectedRatio: StudioAspectRatio,
    prompt: String,
    studioState: StudioUiState,
    onSelectSize: (StudioImageSize) -> Unit,
    onSelectRatio: (StudioAspectRatio) -> Unit,
    onPromptChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onBackToBazaar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToBazaar)

    val context = LocalContext.current
    val isGenerating = studioState is StudioUiState.Generating

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("image_studio_list")
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${stringResource(R.string.studio_subtitle)} (${GeminiRepository.IMAGE_GENERATION_MODEL})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 1K / 2K / 4K Size Selector + Aspect Ratio Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.studio_size_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val sizes = StudioImageSize.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sizes.forEachIndexed { index, sizeOption ->
                            SegmentedButton(
                                selected = selectedSize == sizeOption,
                                onClick = { onSelectSize(sizeOption) },
                                enabled = !isGenerating,
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = sizes.size
                                ),
                                modifier = Modifier.testTag(
                                    "image_size_${sizeOption.apiValue.lowercase()}"
                                )
                            ) {
                                Text(
                                    text = sizeOption.marathiLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.studio_ratio_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioAspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { onSelectRatio(ratio) },
                                enabled = !isGenerating,
                                label = { Text(ratio.marathiLabel) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("aspect_ratio_${ratio.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        enabled = !isGenerating,
                        minLines = 3,
                        maxLines = 5,
                        label = { Text(stringResource(R.string.studio_prompt_label)) },
                        placeholder = { Text(stringResource(R.string.studio_prompt_placeholder)) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_prompt_input")
                    )

                    Text(
                        text = stringResource(R.string.studio_preset_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        studioMarathiPresets.forEachIndexed { idx, preset ->
                            AssistChip(
                                onClick = { onPromptChange(preset) },
                                enabled = !isGenerating,
                                label = {
                                    Text(
                                        text = "कल्पना ${MarathiFormatters.toMarathiNumber(idx + 1)}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("studio_preset_$idx")
                            )
                        }
                    }

                    Button(
                        onClick = onGenerateClick,
                        enabled = !isGenerating,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("generate_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.btn_generate_image)} (${selectedSize.apiValue})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Output Canvas Section
        item {
            when (studioState) {
                is StudioUiState.Idle -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "तुमचा तयार केलेला १K / २K / ४K फोटो येथे दिसेल",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "वरीलपैकी कोणतीही मराठी कल्पना निवडा किंवा स्वतःचे वर्णन लिहून 'उच्च दर्जाचा फोटो तयार करा' दाबा.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is StudioUiState.Generating -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(
                                    R.string.studio_generating_msg,
                                    studioState.size.apiValue
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                is StudioUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_error_card")
                    ) {
                        Text(
                            text = studioState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is StudioUiState.Success -> {
                    val generated = studioState.result
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_result_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    bitmap = generated.bitmap.asImageBitmap(),
                                    contentDescription = generated.prompt,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "${generated.imageSize.apiValue} • ${generated.aspectRatio.apiValue}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!generated.captionText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = generated.captionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    MarathiFormatters.shareGeneratedBitmap(
                                        context = context,
                                        bitmap = generated.bitmap,
                                        prompt = generated.prompt
                                    )
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("share_generated_image_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_share_image))
                            }
                        }
                    }
                }
            }
        }
    }
}
