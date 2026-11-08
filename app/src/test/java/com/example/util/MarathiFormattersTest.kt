package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarathiFormattersTest {
    @Test
    fun normalizesAsciiAndMarathiMobileDigits() {
        assertEquals("9876543210", MarathiFormatters.normalizePhoneDigits("९८७६५४३२१०"))
        assertEquals("9876543210", MarathiFormatters.normalizePhoneDigits("+91 98765-43210"))
    }

    @Test
    fun preservesCountryPrefixWhileNumberIsBeingTyped() {
        assertEquals("91987654321", MarathiFormatters.normalizePhoneDigits("+91 987654321"))
        assertEquals("9876543210", MarathiFormatters.normalizePhoneDigits("+91 9876543210"))
    }

    @Test
    fun acceptsOnlyValidTenDigitIndianMobileNumbers() {
        assertTrue(MarathiFormatters.isValidIndianMobile("9876543210"))
        assertTrue(MarathiFormatters.isValidIndianMobile("६७६५४३२१०९"))
        assertFalse(MarathiFormatters.isValidIndianMobile("5876543210"))
        assertFalse(MarathiFormatters.isValidIndianMobile("987654321"))
        assertFalse(MarathiFormatters.isValidIndianMobile("98765432109"))
    }
}
