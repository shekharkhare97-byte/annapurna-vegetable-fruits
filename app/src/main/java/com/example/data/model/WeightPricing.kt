package com.example.data.model

/**
 * Customer weight choices for products priced per kilogram.
 * Custom quantities are entered in grams; prices retain paise precision.
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

    /** Returns the exact rupee amount; UI can format it to two decimal places. */
    fun priceForGrams(pricePerKgRupees: Int, grams: Int): Double {
        require(pricePerKgRupees >= 0) { "Price cannot be negative" }
        require(isValidCustomWeight(grams)) { "Weight must be between 1 and 100000 grams" }
        return pricePerKgRupees.toDouble() * grams / 1000.0
    }
}
