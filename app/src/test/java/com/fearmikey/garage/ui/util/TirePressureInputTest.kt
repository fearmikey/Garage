package com.fearmikey.garage.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** The Edit Parts screen must show and save tire pressure in the same unit the Parts list shows. */
class TirePressureInputTest {

    private fun <T> inUs(block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        return try { block() } finally { Locale.setDefault(previous) }
    }

    @Test
    fun storedPsiIsShownInBarForMetricAndUk() = inUs {
        assertEquals("2.4", UnitConverter.tirePressureForInput("35 PSI", UnitSystem.METRIC))
        assertEquals("2.4", UnitConverter.tirePressureForInput("35", UnitSystem.UK_MIXED))
        assertEquals("35", UnitConverter.tirePressureForInput("35 psi", UnitSystem.IMPERIAL))
        assertEquals("35", UnitConverter.tirePressureForInput("2.4 bar", UnitSystem.IMPERIAL))
        assertEquals("2.2", UnitConverter.tirePressureForInput("220 kPa", UnitSystem.METRIC))
        assertEquals("N/A (Electric)", UnitConverter.tirePressureForInput("N/A (Electric)", UnitSystem.METRIC))
    }

    @Test
    fun bareNumbersAreSavedWithTheUsersUnit() {
        assertEquals("2.4 bar", UnitConverter.tirePressureFromInput(" 2.4 ", UnitSystem.METRIC))
        assertEquals("35 PSI", UnitConverter.tirePressureFromInput("35", UnitSystem.IMPERIAL))
        assertEquals("36 psi", UnitConverter.tirePressureFromInput("36 psi", UnitSystem.METRIC))
        assertNull(UnitConverter.tirePressureFromInput("  ", UnitSystem.METRIC))
    }

    @Test
    fun metricValueEnteredInBarDisplaysAsTheSameBarValue() = inUs {
        // Previously "2.4" was assumed to be PSI and the list showed "0.2 bar".
        val stored = UnitConverter.tirePressureFromInput("2.4", UnitSystem.METRIC)
        assertEquals("2.4 bar", UnitConverter.formatTirePressure(stored, UnitSystem.METRIC))
        assertEquals("2.4", UnitConverter.tirePressureForInput(stored, UnitSystem.METRIC))
    }
}
