package com.fearmikey.garage.ui.vehicle

import com.fearmikey.garage.ui.util.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class SpecUnitsTest {

    @Test
    fun displacement_commaDecimalGetsLitreUnitForCars() {
        assertEquals("1,6 L", SpecUnits.formatDisplacement("1,6", isMotorcycle = false))
        assertEquals("1.8L", SpecUnits.formatDisplacement("1.8L", isMotorcycle = false))
    }

    @Test
    fun displacement_motorcycleShowsCc() {
        assertEquals("452 cc", SpecUnits.formatDisplacement("0,452", isMotorcycle = true))
        assertEquals("452 cc", SpecUnits.formatDisplacement("452", isMotorcycle = true))
    }

    @Test
    fun displacement_motorcycleInputRoundTripsAsLitres() {
        assertEquals("452", SpecUnits.displacementForInput("0.452", isMotorcycle = true))
        assertEquals("0.452", SpecUnits.displacementFromInput("452", isMotorcycle = true))
        assertEquals("1.6", SpecUnits.displacementFromInput("1.6", isMotorcycle = false))
    }

    @Test
    fun weight_bareNumberUsesUnitSystem() {
        assertEquals("1200 kg", SpecUnits.formatWeight("1200", UnitSystem.METRIC))
        assertEquals("2645 lbs", SpecUnits.formatWeight("2645", UnitSystem.IMPERIAL))
        assertEquals("4480 lbs", SpecUnits.formatWeight("4480 lbs", UnitSystem.METRIC))
    }
}
