package com.example.data.model

/**
 * A cart/order line with the exact weight and price selected by the customer.
 * Store money as paise so totals do not lose precision.
 */
data class WeightedCartLine(
    val produceId: String,
    val produceName: String,
    val pricePerKgRupees: Int,
    val weightGrams: Int
) {
    init {
        require(produceId.isNotBlank()) { "Produce id is required" }
        require(produceName.isNotBlank()) { "Produce name is required" }
        require(pricePerKgRupees >= 0) { "Price cannot be negative" }
        require(WeightPricing.isValidCustomWeight(weightGrams)) { "Weight must be between 1 and 100000 grams" }
    }

    val lineTotalPaise: Long
        get() = WeightPricing.priceInPaiseForGrams(pricePerKgRupees, weightGrams)

    val formattedWeight: String
        get() = WeightPricing.formatWeightMarathi(weightGrams)

    /** Firestore-friendly fields for an order item document. */
    fun toOrderMap(): Map<String, Any> = mapOf(
        "produceId" to produceId,
        "produceName" to produceName,
        "pricePerKgRupees" to pricePerKgRupees,
        "weightGrams" to weightGrams,
        "lineTotalPaise" to lineTotalPaise
    )
}
