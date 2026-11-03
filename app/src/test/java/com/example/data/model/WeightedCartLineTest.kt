package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedCartLineTest {
    @Test
    fun storesSelectedWeightAndCalculatedOrderAmount() {
        val line = WeightedCartLine("tomato", "टोमॅटो", 40, 250)
        assertEquals(250, line.weightGrams)
        assertEquals(1000L, line.lineTotalPaise)
        assertEquals("२५० ग्रॅम", line.formattedWeight)
    }

    @Test
    fun orderMapContainsWeightAndPaiseTotal() {
        val map = WeightedCartLine("apple", "सफरचंद", 200, 125).toOrderMap()
        assertEquals(125, map["weightGrams"])
        assertEquals(2500L, map["lineTotalPaise"])
        assertEquals(200, map["pricePerKgRupees"])
        assertTrue(map.containsKey("produceId"))
    }
}
