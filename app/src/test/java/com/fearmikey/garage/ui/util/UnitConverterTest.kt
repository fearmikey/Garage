package com.fearmikey.garage.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `UnitSystem fromString returns expected enum`() {
        assertEquals(UnitSystem.METRIC, UnitSystem.fromString("metric"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromString("Metric"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromString("imperial"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromString(null))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromString("unknown"))
    }

    @Test
    fun `distance conversions and formatting`() {
        val miles = 10000
        val expectedKm = 16093

        assertEquals(expectedKm, UnitConverter.milesToKm(miles))
        assertEquals(miles, UnitConverter.kmToMiles(expectedKm))

        assertEquals(10000, UnitConverter.displayDistanceValue(miles, UnitSystem.IMPERIAL))
        assertEquals(16093, UnitConverter.displayDistanceValue(miles, UnitSystem.METRIC))

        assertEquals(10000, UnitConverter.canonicalMilesFromInput(10000, UnitSystem.IMPERIAL))
        assertEquals(10000, UnitConverter.canonicalMilesFromInput(16093, UnitSystem.METRIC))

        assertEquals("10,000 mi", UnitConverter.formatDistance(miles, UnitSystem.IMPERIAL))
        assertEquals("16,093 km", UnitConverter.formatDistance(miles, UnitSystem.METRIC))
    }

    @Test
    fun `volume conversions and formatting`() {
        val gallons = 10.0
        val expectedLiters = 37.85411784

        assertEquals(expectedLiters, UnitConverter.gallonsToLiters(gallons), 0.0001)
        assertEquals(gallons, UnitConverter.litersToGallons(expectedLiters), 0.0001)

        assertEquals(10.0, UnitConverter.displayVolumeValue(gallons, UnitSystem.IMPERIAL), 0.001)
        assertEquals(37.854, UnitConverter.displayVolumeValue(gallons, UnitSystem.METRIC), 0.001)

        assertEquals(10.0, UnitConverter.canonicalGallonsFromInput(10.0, UnitSystem.IMPERIAL), 0.001)
        assertEquals(10.0, UnitConverter.canonicalGallonsFromInput(37.85411784, UnitSystem.METRIC), 0.001)

        assertEquals("10.000 gal", UnitConverter.formatVolume(gallons, UnitSystem.IMPERIAL))
        assertEquals("37.854 L", UnitConverter.formatVolume(gallons, UnitSystem.METRIC))

        val pricePerGallon = 3.785411784
        assertEquals("$3.785/gal", UnitConverter.formatPricePerVolume(pricePerGallon, UnitSystem.IMPERIAL))
        assertEquals("$1.000/L", UnitConverter.formatPricePerVolume(pricePerGallon, UnitSystem.METRIC))
    }

    @Test
    fun `fuel economy conversions and formatting`() {
        val mpg = 23.5214583 // Should equal 10.0 L/100km

        assertEquals(10.0, UnitConverter.mpgToLitersPer100Km(mpg)!!, 0.01)

        assertEquals("23.5 mpg", UnitConverter.formatFuelEconomy(mpg, UnitSystem.IMPERIAL))
        assertEquals("10.0 L/100km", UnitConverter.formatFuelEconomy(mpg, UnitSystem.METRIC))

        assertEquals("—", UnitConverter.formatFuelEconomy(null, UnitSystem.IMPERIAL))
        assertEquals("—", UnitConverter.formatFuelEconomy(0.0, UnitSystem.METRIC))

        assertEquals("23.5 mpg since last full tank", UnitConverter.formatSegmentFuelEconomy(mpg, UnitSystem.IMPERIAL))
        assertEquals("10.0 L/100km since last full tank", UnitConverter.formatSegmentFuelEconomy(mpg, UnitSystem.METRIC))
    }
}
