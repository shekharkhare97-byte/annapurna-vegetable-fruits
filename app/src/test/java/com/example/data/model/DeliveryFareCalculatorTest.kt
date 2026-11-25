package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryFareCalculatorTest {
    @Test
    fun chargesTenRupeesForOneStartedKilometre() {
        assertEquals(10, DeliveryFareCalculator.fromDistanceKm(0.2).chargeRupees)
        assertEquals(10, DeliveryFareCalculator.fromDistanceKm(1.0).chargeRupees)
    }

    @Test
    fun roundsUpPartialKilometres() {
        val fare = DeliveryFareCalculator.fromDistanceKm(1.01)
        assertEquals(2, fare.chargeableKm)
        assertEquals(20, fare.chargeRupees)
    }

    @Test
    fun calculatesLongerDistanceAtTenRupeesPerKilometre() {
        assertEquals(50, DeliveryFareCalculator.fromDistanceKm(4.2).chargeRupees)
    }
}
