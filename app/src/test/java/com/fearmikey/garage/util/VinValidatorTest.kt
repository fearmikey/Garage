package com.fearmikey.garage.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VinValidatorTest {

    @Test
    fun `normalize strips spaces and separators`() {
        assertEquals("ZDM1234567B000000", VinValidator.normalize("zdm 1234-567 b000000"))
    }

    @Test
    fun `normalize maps I O Q to 1 0 0`() {
        assertEquals("1000", VinValidator.normalize("IOQ0"))
    }

    @Test
    fun `extractVinFromScan rejects short non-VIN QR payloads`() {
        assertNull(VinValidator.extractVinFromScan("03176243"))
    }

    @Test
    fun `extractVinFromScan accepts plain VIN and VIN embedded in QR data`() {
        val vin = "MLHND16B6P5102986"
        assertEquals(vin, VinValidator.extractVinFromScan(vin))
        assertEquals(vin, VinValidator.extractVinFromScan("DOC|12345|$vin|2023"))
        assertEquals(vin, VinValidator.extractVinFromScan("I$vin"))
    }

    @Test
    fun `normalize drops leading Code 39 import marker`() {
        val vin = "1HGCM82633A004352"
        assertEquals(vin, VinValidator.normalize("I$vin"))
        assertTrue(VinValidator.isValidVin(VinValidator.normalize("I$vin")))
    }
}
