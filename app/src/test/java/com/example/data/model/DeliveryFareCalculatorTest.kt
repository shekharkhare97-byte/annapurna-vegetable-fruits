package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryFareCalculatorTest {
    @Test
    fun chargesTenRupeesForOneKilometre() {
        assertEquals(10, DeliveryFareCalculator.fromDistanceKm(1.0).chargeRupees)
    }

    @Test
    fun chargesProportionallyForFractionalKilometres() {
        assertEquals(2, DeliveryFareCalculator.fromDistanceKm(0.2).chargeRupees)
        assertEquals(42, DeliveryFareCalculator.fromDistanceKm(4.2).chargeRupees)
    }

    @Test
    fun roundsOnlyFractionalRupeesUpToTheNextRupee() {
        assertEquals(11, DeliveryFareCalculator.fromDistanceKm(1.01).chargeRupees)
    }
}
