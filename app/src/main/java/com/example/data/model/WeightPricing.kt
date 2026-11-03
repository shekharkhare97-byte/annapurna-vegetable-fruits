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

    fun priceInPaiseForGrams(pricePerKgRupees: Int, grams: Int): Long {
        require(pricePerKgRupees >= 0) { "Price cannot be negative" }
        require(grams > 0) { "Weight must be positive" }
        return (pricePerKgRupees.toLong() * 100L * grams + 500L) / 1000L
    }

    fun priceForGrams(pricePerKgRupees: Int, grams: Int): Double =
        priceInPaiseForGrams(pricePerKgRupees, grams) / 100.0

    fun formatWeightMarathi(grams: Int): String {
        require(grams > 0) { "Weight must be positive" }
        return if (grams == 1000) "१ किलो" else "${grams} ग्रॅम"
    }
}
