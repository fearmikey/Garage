package com.fearmikey.garage.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VinValidatorTest {

    @Test
    fun `valid 17-character VIN is accepted`() {
        assertTrue(VinValidator.isValidVin("3TYCZ5AN0PT000001"))
    }

    @Test
    fun `lowercase VIN is accepted (normalized to uppercase)`() {
        assertTrue(VinValidator.isValidVin("3tycz5an0pt000001"))
    }

    @Test
    fun `too short is rejected`() {
        assertFalse(VinValidator.isValidVin("3TYCZ5AN0PT00000"))
    }

    @Test
    fun `too long is rejected`() {
        assertFalse(VinValidator.isValidVin("3TYCZ5AN0PT0000011"))
    }

    @Test
    fun `disallowed letters I, O, Q are rejected`() {
        assertFalse(VinValidator.isValidVin("3TYCZ5AI0PT000001"))
        assertFalse(VinValidator.isValidVin("3TYCZ5AO0PT000001"))
        assertFalse(VinValidator.isValidVin("3TYCZ5AQ0PT000001"))
    }

    @Test
    fun `non-alphanumeric characters are rejected`() {
        assertFalse(VinValidator.isValidVin("3TYCZ5AN0PT00000-"))
    }

    @Test
    fun `findVinInText extracts a VIN-shaped token from surrounding text`() {
        val text = "VIN: 3TYCZ5AN0PT000001 (do not remove this sticker)"
        assertEquals("3TYCZ5AN0PT000001", VinValidator.findVinInText(text))
    }

    @Test
    fun `findVinInText returns null when no VIN-shaped token is present`() {
        assertEquals(null, VinValidator.findVinInText("no vin here"))
    }
}
