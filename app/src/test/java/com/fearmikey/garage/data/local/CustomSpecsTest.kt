package com.fearmikey.garage.data.local

import com.fearmikey.garage.data.local.entity.CustomSpec
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.ui.vehicle.VehicleSpecsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSpecsTest {
    private val converters = Converters()

    @Test
    fun customSpecs_roundTripThroughConverter() {
        val specs = listOf(CustomSpec("Chain size", "520"), CustomSpec("Tire PSI", "36 / 42"))
        assertEquals(specs, converters.toCustomSpecs(converters.fromCustomSpecs(specs)))
    }

    @Test
    fun customSpecs_toleratesMalformedOrDefaultJson() {
        assertEquals(emptyList<CustomSpec>(), converters.toCustomSpecs("[]"))
        assertEquals(emptyList<CustomSpec>(), converters.toCustomSpecs("not json"))
        assertEquals(emptyList<CustomSpec>(), converters.toCustomSpecs(null))
        assertEquals(listOf(CustomSpec("", "x")), converters.toCustomSpecs("""[{"value":"x"}]"""))
    }

    @Test
    fun normalize_blanksBecomeNullAndEmptyCustomRowsDropped() {
        val normalized = VehicleSpecsViewModel.normalize(
            VehicleSpecs(
                vehicleId = 1,
                engineHp = "  ",
                displacementL = " 0.65 ",
                customSpecs = listOf(CustomSpec(" ", ""), CustomSpec(" Chain ", " 520 ")),
            ),
        )
        assertNull(normalized.engineHp)
        assertEquals("0.65", normalized.displacementL)
        assertEquals(listOf(CustomSpec("Chain", "520")), normalized.customSpecs)
    }

    @Test
    fun isEmpty_considersCustomSpecs() {
        assertTrue(VehicleSpecs(vehicleId = 1).isEmpty())
        assertFalse(VehicleSpecs(vehicleId = 1, customSpecs = listOf(CustomSpec("Chain", "520"))).isEmpty())
    }
}
