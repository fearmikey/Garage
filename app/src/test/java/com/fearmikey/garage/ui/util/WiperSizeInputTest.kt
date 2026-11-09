package com.fearmikey.garage.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** The Edit Parts screen must show and save wiper sizes in the same unit the Parts list shows. */
class WiperSizeInputTest {

    private fun <T> inUs(block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        return try { block() } finally { Locale.setDefault(previous) }
    }

    @Test
    fun storedSizeIsShownInTheUsersUnit() = inUs {
        assertEquals("660", UnitConverter.wiperSizeForInput("26 in", UnitSystem.METRIC))
        assertEquals("660", UnitConverter.wiperSizeForInput("26", UnitSystem.UK_MIXED))
        assertEquals("26", UnitConverter.wiperSizeForInput("26 in", UnitSystem.IMPERIAL))
        assertEquals("26", UnitConverter.wiperSizeForInput("650 mm", UnitSystem.IMPERIAL))
        assertEquals("16.5", UnitConverter.wiperSizeForInput("16.5", UnitSystem.IMPERIAL))
        assertEquals("650", UnitConverter.wiperSizeForInput("65 cm", UnitSystem.METRIC))
        assertEquals("N/A", UnitConverter.wiperSizeForInput("N/A", UnitSystem.METRIC))
    }

    @Test
    fun bareNumbersAreSavedWithTheUsersUnit() {
        assertEquals("650 mm", UnitConverter.wiperSizeFromInput(" 650 ", UnitSystem.METRIC))
        assertEquals("26 in", UnitConverter.wiperSizeFromInput("26", UnitSystem.IMPERIAL))
        assertEquals("24 in", UnitConverter.wiperSizeFromInput("24 in", UnitSystem.METRIC))
        assertNull(UnitConverter.wiperSizeFromInput("", UnitSystem.METRIC))
    }

    @Test
    fun metricValueEnteredInMmDisplaysAsTheSameMmValue() {
        // Previously "650" was assumed to be inches and the list showed "16510 mm".
        val stored = UnitConverter.wiperSizeFromInput("650", UnitSystem.METRIC)
        assertEquals("650 mm", UnitConverter.formatWiperSize(stored, UnitSystem.METRIC))
        assertEquals("650", UnitConverter.wiperSizeForInput(stored, UnitSystem.METRIC))
    }
}
