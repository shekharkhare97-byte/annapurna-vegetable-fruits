package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WeightPricing
import com.example.data.model.ProduceItem
import java.math.RoundingMode

/**
 * Customer weight picker. Custom quantity is entered in kilograms and
 * converted to grams internally for existing cart/order calculations.
 */
@Composable
fun WeightSelectionDialog(
    item: ProduceItem,
    onDismiss: () -> Unit,
    onConfirm: (weightGrams: Int) -> Unit
) {
    var selectedWeight by remember(item.id) { mutableIntStateOf(if (item.isPieceBased) 1000 else 250) }
    var customWeightText by remember(item.id) { mutableStateOf("") }
    val customWeightGrams = customWeightText.toBigDecimalOrNull()
        ?.movePointRight(3)
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toInt()
    val isCustom = selectedWeight == 0
    val grams = if (isCustom) customWeightGrams ?: 0 else selectedWeight
    val valid = if (isCustom) customWeightGrams?.let(WeightPricing::isValidCustomWeight) == true else true
    val amount = if (item.isPieceBased) item.priceRupees else if (valid && grams > 0) WeightPricing.priceForGrams(item.priceRupees, grams) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                if (item.isPieceBased) "${item.unitMarathi.removePrefix("१ ")} निवडा — ${item.marathiName}" else "वजन निवडा — ${item.marathiName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (item.isPieceBased) "दर: ₹${com.example.util.MarathiFormatters.formatRupees(item.priceRupees)} / ${item.unitMarathi.removePrefix("१ ")}" else "दर: ₹${item.priceRupees} / किलो",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                if (!item.isPieceBased) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WeightPricing.standardOptions) { option ->
                        FilterChip(
                            selected = selectedWeight == (option.grams ?: 0),
                            onClick = { selectedWeight = option.grams ?: 0 },
                            label = { Text(option.label) }
                        )
                    }
                }
                if (!item.isPieceBased && isCustom) {
                    OutlinedTextField(
                        value = customWeightText,
                        onValueChange = { value ->
                            val normalized = value.replace(',', '.').filter { it.isDigit() || it == '.' }
                            val whole = normalized.substringBefore('.').take(3)
                            val fraction = normalized.substringAfter('.', "").replace(".", "").take(3)
                            customWeightText = if ('.' in normalized) "$whole.$fraction" else whole
                        },
                        label = { Text("वजन किलोमध्ये लिहा") },
                        placeholder = { Text("उदा. ०.३५० किंवा १.५") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = customWeightText.isNotEmpty() && !valid,
                        supportingText = { Text(if (valid) "०.००१ ते १०० किलो" else "योग्य वजन लिहा") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Surface(
                    color = if (valid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (valid) "या वजनाची किंमत" else "कृपया योग्य वजन भरा",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (valid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (valid) "₹${"%.2f".format(amount)}" else "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (item.isPieceBased) 1000 else grams) },
                enabled = item.isPieceBased || (valid && grams > 0),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(48.dp)
            ) { Text("पिशवीत टाका") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}
