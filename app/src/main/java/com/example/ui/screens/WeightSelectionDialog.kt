package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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

/**
 * Customer weight picker. Returns the selected quantity in grams.
 * Price preview is calculated from the product's per-kilogram rate.
 */
@Composable
fun WeightSelectionDialog(
    item: ProduceItem,
    onDismiss: () -> Unit,
    onConfirm: (weightGrams: Int) -> Unit
) {
    var selectedWeight by remember(item.id) { mutableIntStateOf(250) }
    var customWeightText by remember(item.id) { mutableStateOf("") }
    val customWeight = customWeightText.toIntOrNull()
    val isCustom = selectedWeight == 0
    val valid = if (isCustom) customWeight?.let(WeightPricing::isValidCustomWeight) == true else true
    val grams = if (isCustom) customWeight ?: 0 else selectedWeight
    val amount = if (valid && grams > 0) WeightPricing.priceForGrams(item.priceRupees, grams) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("वजन निवडा — ${item.marathiName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("दर: ₹${item.priceRupees} / किलो")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WeightPricing.standardOptions) { option ->
                        FilterChip(
                            selected = selectedWeight == (option.grams ?: 0),
                            onClick = { selectedWeight = option.grams ?: 0 },
                            label = { Text(option.label) }
                        )
                    }
                }
                if (isCustom) {
                    OutlinedTextField(
                        value = customWeightText,
                        onValueChange = { value -> customWeightText = value.filter(Char::isDigit).take(6) },
                        label = { Text("वजन ग्रॅममध्ये लिहा") },
                        placeholder = { Text("उदा. ३५०") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = customWeightText.isNotEmpty() && !valid,
                        supportingText = { Text(if (valid) "१ ते १,००,००० ग्रॅम" else "योग्य वजन लिहा") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = if (valid) "या वजनाची किंमत: ₹${"%.2f".format(amount)}" else "कृपया योग्य वजन भरा",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(grams) },
                enabled = valid && grams > 0
            ) { Text("पिशवीत टाका") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}
