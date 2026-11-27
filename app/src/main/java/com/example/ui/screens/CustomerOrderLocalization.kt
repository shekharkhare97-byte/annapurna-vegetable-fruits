package com.example.ui.screens

import com.example.data.local.OrderEntity
import android.util.Base64
import com.example.data.model.MealComboPack
import com.example.data.model.ProduceCatalog
import com.example.data.model.WeightPricing
import java.util.Locale

/**
 * Rebuilds saved order item names from the current multilingual product catalog.
 * Falls back to the original saved Marathi receipt when a historical product was deleted.
 */
fun localizedOrderItemsSummary(order: OrderEntity, language: String): String {
    if (order.serializedItems.isBlank()) return order.itemsSummaryMarathi
    val lines = order.serializedItems.split(",").mapNotNull { encoded ->
        val parts = encoded.split(":")
        if (parts.size < 2) return@mapNotNull null
        if (parts[0].startsWith("combo~")) {
            val quantity = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val combo = runCatching {
                val bytes = Base64.decode(parts[0].removePrefix("combo~"), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                MealComboPack.fromJson(String(bytes, Charsets.UTF_8))
            }.getOrNull() ?: return@mapNotNull null
            val ingredients = combo.ingredients.joinToString(", ") { "${it.displayName(language)} ${it.quantityGrams}g" }
            val price = String.format(Locale.US, "%.2f", combo.priceRupees * quantity).trimEnd('0').trimEnd('.')
            return@mapNotNull "• ${combo.displayName(language)} × $quantity — $ingredients = ₹$price"
        }
        val product = ProduceCatalog.findById(parts[0]) ?: return@mapNotNull null
        val quantity = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val grams = parts.getOrNull(2)?.toIntOrNull()?.takeIf { it > 0 } ?: 1000
        val quantityLabel = if (product.isPieceBased) {
            val unit = product.displayUnit().removePrefix("1 ").removePrefix("१ ")
            "$quantity $unit"
        } else if (language == "mr") {
            WeightPricing.formatWeightMarathi(grams)
        } else {
            val amount = if (grams % 1000 == 0) "${grams / 1000} kg" else "$grams g"
            amount
        }
        val lineTotal = if (product.isPieceBased) {
            product.priceRupees * quantity
        } else {
            WeightPricing.priceInPaiseForGrams(product.priceRupees, grams) * quantity / 100.0
        }
        val price = String.format(Locale.US, "%.2f", lineTotal).trimEnd('0').trimEnd('.')
        "• ${product.displayName()} ($quantityLabel) = ₹$price"
    }
    return if (lines.isEmpty()) order.itemsSummaryMarathi else lines.joinToString("\n")
}
