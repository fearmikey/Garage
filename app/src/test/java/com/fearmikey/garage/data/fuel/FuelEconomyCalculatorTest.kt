package com.fearmikey.garage.data.fuel

import com.fearmikey.garage.data.local.entity.FuelRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEconomyCalculatorTest {

    @Test
    fun `entriesFor returns empty list when no records`() {
        val entries = FuelEconomyCalculator.entriesFor(emptyList())
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `entriesFor returns empty list when no full tank record`() {
        val records = listOf(
            FuelRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, isFullTank = false),
            FuelRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10250, gallons = 8.0, totalCost = 28.0, pricePerGallon = 3.5, isFullTank = false),
        )
        val entries = FuelEconomyCalculator.entriesFor(records)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `entriesFor computes segment MPG and most recent tank correctly`() {
        val records = listOf(
            FuelRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, gallons = 12.0, totalCost = 42.0, pricePerGallon = 3.5, isFullTank = true),
            FuelRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10300, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, isFullTank = true), // 300 miles / 10 gallons = 30.0 mpg
            FuelRecord(id = 3, vehicleId = 1, date = 3000L, mileage = 10550, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, isFullTank = true), // 250 miles / 10 gallons = 25.0 mpg
        )

        val entries = FuelEconomyCalculator.entriesFor(records)
        assertEquals(2, entries.size)

        assertEquals(30.0, entries[0].mpg, 0.001)
        assertEquals(25.0, entries[1].mpg, 0.001)

        val recentTankEntry = entries.lastOrNull()
        assertEquals(25.0, recentTankEntry?.mpg ?: 0.0, 0.001)
    }

    @Test
    fun `entriesFor correctly handles partial fill-ups rolled into next full tank`() {
        val records = listOf(
            FuelRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, gallons = 12.0, totalCost = 42.0, pricePerGallon = 3.5, isFullTank = true),
            FuelRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10150, gallons = 5.0, totalCost = 17.5, pricePerGallon = 3.5, isFullTank = false), // Partial
            FuelRecord(id = 3, vehicleId = 1, date = 3000L, mileage = 10300, gallons = 5.0, totalCost = 17.5, pricePerGallon = 3.5, isFullTank = true), // Full -> total miles = 300, total gallons = 10.0 -> 30.0 mpg
        )

        val entries = FuelEconomyCalculator.entriesFor(records)
        assertEquals(1, entries.size)
        assertEquals(30.0, entries.last().mpg, 0.001)
    }

    @Test
    fun `averageMpg computes overall weighted average`() {
        val records = listOf(
            FuelRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, gallons = 12.0, totalCost = 42.0, pricePerGallon = 3.5, isFullTank = true),
            FuelRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10300, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, isFullTank = true), // 300 / 10 = 30
            FuelRecord(id = 3, vehicleId = 1, date = 3000L, mileage = 10500, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, isFullTank = true), // 200 / 10 = 20
        )

        val entries = FuelEconomyCalculator.entriesFor(records)
        // Total miles = 500, total gallons = 20 -> weighted average = 25.0
        val avg = FuelEconomyCalculator.averageMpg(entries)
        assertEquals(25.0, avg!!, 0.001)
    }
}
