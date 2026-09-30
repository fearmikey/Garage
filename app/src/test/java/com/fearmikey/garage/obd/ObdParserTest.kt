package com.fearmikey.garage.obd

import org.junit.Assert.assertEquals
import org.junit.Test

class ObdParserTest {

    private val parser = ObdParser()

    @Test
    fun testParseDtcs_noDtcs() {
        val response = "43 00 00 00 00"
        val dtcs = parser.parseDtcs(response)
        assertEquals(emptyList<String>(), dtcs)
    }

    @Test
    fun testParseDtcs_oneDtc() {
        val response = "43 01 33 00 00"
        val dtcs = parser.parseDtcs(response)
        assertEquals(listOf("P0133"), dtcs)
    }
    
    @Test
    fun testParseDtcs_multipleDtcs() {
        val response = "43 01 33 41 33"
        val dtcs = parser.parseDtcs(response)
        assertEquals(listOf("P0133", "C0133"), dtcs)
    }

    @Test
    fun testParseOdometer() {
        // 41 A6 00 00 27 10 -> 10000 / 10 = 1000
        val response = "41 A6 00 00 27 10"
        val odometer = parser.parseOdometer(response)
        assertEquals(1000, odometer)
    }

    @Test
    fun testParseDistanceSinceCleared() {
        // 41 31 03 E8 -> 1000
        val response = "41 31 03 E8"
        val distance = parser.parseDistanceSinceCodesCleared(response)
        assertEquals(1000, distance)
    }
}
