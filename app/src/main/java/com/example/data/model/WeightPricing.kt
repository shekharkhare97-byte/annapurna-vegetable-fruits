package com.example.data.model

/**
 * Shared weight choices for produce sold by kilogram.
 * Custom quantities are entered in grams.
 */
object WeightPricing {
    data class Option(val label: String, val grams: Int?)

    val standardOptions = listOf(
        Option("१२५ ग्रॅम", 125),
        Option("२५० ग्रॅम", 250),
        Option("५०० ग्रॅम", 500),
        Option("१ किलो", 1000),
        Option("कस्टम", null)
    )

    fun isValidCustomWeight(grams: Int): Boolean = grams in 1..100_000

    fun priceInPaiseForGrams(pricePerKgRupees: Double, grams: Int): Long {
        require(pricePerKgRupees >= 0.0) { "Price cannot be negative" }
        require(grams > 0) { "Weight must be positive" }
        val pricePaisePerKg = java.math.BigDecimal.valueOf(pricePerKgRupees)
            .movePointRight(2)
        return pricePaisePerKg.multiply(java.math.BigDecimal.valueOf(grams.toLong()))
            .divide(java.math.BigDecimal.valueOf(1000L), 0, java.math.RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun priceForGrams(pricePerKgRupees: Double, grams: Int): Double =
        priceInPaiseForGrams(pricePerKgRupees, grams) / 100.0

    fun formatWeightMarathi(grams: Int): String {
        require(grams > 0) { "Weight must be positive" }
        val marathiGrams = grams.toString().map { digit ->
            if (digit in '0'..'9') "०१२३४५६७८९"[digit - '0'] else digit
        }.joinToString("")
        return if (grams == 1000) "१ किलो" else "$marathiGrams ग्रॅम"
    }
}
