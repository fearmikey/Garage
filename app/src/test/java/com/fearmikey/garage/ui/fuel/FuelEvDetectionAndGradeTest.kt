package com.fearmikey.garage.ui.fuel

import com.fearmikey.garage.data.local.entity.FuelGrade
import com.fearmikey.garage.data.local.entity.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEvDetectionAndGradeTest {

    private fun vehicle(make: String, model: String, trim: String = "") =
        Vehicle(make = make, model = model, trim = trim)

    @Test
    fun `gas vehicles whose names merely contain ev are not treated as EVs`() {
        assertFalse(isVehicleEvByDetails(vehicle("Chevrolet", "Silverado 1500", "LT"), null))
        assertFalse(isVehicleEvByDetails(vehicle("CHEVROLET", "Malibu"), null))
        assertFalse(isVehicleEvByDetails(vehicle("Land Rover", "Range Rover Evoque"), null))
        assertFalse(isVehicleEvByDetails(vehicle("Maserati", "Levante"), null))
        assertFalse(isVehicleEvByDetails(vehicle("Toyota", "Tacoma", "SR5"), null))
    }

    @Test
    fun `known EVs and plug-ins are still detected`() {
        assertTrue(isVehicleEvByDetails(vehicle("Tesla", "Model 3"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Kia", "EV6", "GT-Line"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Chevrolet", "Bolt EV"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Nissan", "Leaf"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Ford", "F-150 Lightning"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Mitsubishi", "Outlander", "PHEV"), null))
        assertTrue(isVehicleEvByDetails(vehicle("Hyundai", "Kona", "Electric"), null))
    }

    @Test
    fun `fuel grade presets round-trip by name and custom text is not a preset`() {
        FuelGrade.entries.forEach { assertEquals(it, FuelGrade.fromStored(it.name)) }
        assertNull(FuelGrade.fromStored("98 RON"))
        assertNull(FuelGrade.fromStored(null))
    }
}
