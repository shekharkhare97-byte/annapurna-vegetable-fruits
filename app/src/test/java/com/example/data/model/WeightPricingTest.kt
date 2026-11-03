package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightPricingTest {
    @Test
    fun standardWeightChoicesAreAvailable() {
        assertEquals(listOf(125, 250, 500, 1000, null), WeightPricing.standardOptions.map { it.grams })
    }

    @Test
    fun priceIsCalculatedFromPerKilogramRate() {
        assertEquals(10.0, WeightPricing.priceForGrams(40, 250), 0.0)
        assertEquals(20.0, WeightPricing.priceForGrams(40, 500), 0.0)
        assertEquals(40.0, WeightPricing.priceForGrams(40, 1000), 0.0)
    }

    @Test
    fun fractionalRupeeAmountsAreRoundedToNearestPaise() {
        assertEquals(313L, WeightPricing.priceInPaiseForGrams(25, 125))
    }

    @Test
    fun customWeightRangeIsValidated() {
        assertTrue(WeightPricing.isValidCustomWeight(1))
        assertTrue(WeightPricing.isValidCustomWeight(100000))
        assertFalse(WeightPricing.isValidCustomWeight(0))
        assertFalse(WeightPricing.isValidCustomWeight(100001))
    }

    @Test
    fun weightIsFormattedForReceipt() {
        assertEquals("१ किलो", WeightPricing.formatWeightMarathi(1000))
        assertEquals("२५० ग्रॅम", WeightPricing.formatWeightMarathi(250))
    }
}
