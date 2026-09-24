package com.fearmikey.garage.data.fuel

import com.fearmikey.garage.data.local.entity.ChargingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChargingCalculatorTest {

    @Test
    fun `entriesFor returns empty list for insufficient records`() {
        val emptyResult = ChargingCalculator.entriesFor(emptyList())
        assertEquals(0, emptyResult.size)

        val singleRecord = listOf(
            ChargingRecord(
                id = 1,
                vehicleId = 1,
                date = 1000L,
                mileage = 10000,
                kwhAdded = 40.0,
            ),
        )
        val singleResult = ChargingCalculator.entriesFor(singleRecord)
        assertEquals(0, singleResult.size)
    }

    @Test
    fun `entriesFor computes segment efficiency correctly`() {
        val records = listOf(
            ChargingRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, kwhAdded = 40.0),
            ChargingRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10200, kwhAdded = 50.0),
            ChargingRecord(id = 3, vehicleId = 1, date = 3000L, mileage = 10450, kwhAdded = 60.0),
        )

        val entries = ChargingCalculator.entriesFor(records)
        assertEquals(2, entries.size)

        // Segment 1: 10000 -> 10200 (200 mi), 50 kWh added
        assertEquals(200, entries[0].milesDriven)
        assertEquals(50.0, entries[0].kwhUsed, 0.001)
        assertEquals(250.0, entries[0].whPerMi, 0.1) // (50 * 1000) / 200 = 250 Wh/mi
        assertEquals(134.8, entries[0].mpge, 0.1) // (200 / 50) * 33.7 = 134.8 MPGe

        // Segment 2: 10200 -> 10450 (250 mi), 60 kWh added
        assertEquals(250, entries[1].milesDriven)
        assertEquals(60.0, entries[1].kwhUsed, 0.001)
        assertEquals(240.0, entries[1].whPerMi, 0.1) // (60 * 1000) / 250 = 240 Wh/mi
        assertEquals(140.4, entries[1].mpge, 0.1) // (250 / 60) * 33.7 = 140.416 MPGe
    }

    @Test
    fun `averageWhPerMi and averageMpge calculate weighted overall averages`() {
        val records = listOf(
            ChargingRecord(id = 1, vehicleId = 1, date = 1000L, mileage = 10000, kwhAdded = 40.0),
            ChargingRecord(id = 2, vehicleId = 1, date = 2000L, mileage = 10200, kwhAdded = 50.0),
            ChargingRecord(id = 3, vehicleId = 1, date = 3000L, mileage = 10450, kwhAdded = 60.0),
        )

        val entries = ChargingCalculator.entriesFor(records)
        val avgWhPerMi = ChargingCalculator.averageWhPerMi(entries)
        val avgMpge = ChargingCalculator.averageMpge(entries)

        assertNotNull(avgWhPerMi)
        assertNotNull(avgMpge)

        // Total miles = 450, Total kWh = 110
        // Wh/mi = (110 * 1000) / 450 = 244.44
        assertEquals(244.44, avgWhPerMi!!, 0.1)

        // MPGe = (450 / 110) * 33.7 = 137.86
        assertEquals(137.86, avgMpge!!, 0.1)
    }

    @Test
    fun `computeBatteryHealth calculates degradation trend correctly`() {
        val records = listOf(
            ChargingRecord(
                id = 1,
                vehicleId = 1,
                date = 100000L,
                mileage = 1000,
                kwhAdded = 45.0,
                estimatedRangeAt100 = 300,
            ),
            ChargingRecord(
                id = 2,
                vehicleId = 1,
                date = 200000L,
                mileage = 15000,
                kwhAdded = 50.0,
                estimatedRangeAt100 = 291,
            ),
            ChargingRecord(
                id = 3,
                vehicleId = 1,
                date = 300000L,
                mileage = 30000,
                kwhAdded = 52.0,
                estimatedRangeAt100 = 285,
            ),
        )

        val health = ChargingCalculator.computeBatteryHealth(records)

        assertEquals(300, health.initialRangeAt100)
        assertEquals(285, health.currentRangeAt100)
        assertEquals(95.0, health.capacityRetentionPercent!!, 0.1) // 285 / 300 = 95.0%
        assertEquals(5.0, health.degradationPercent!!, 0.1) // 100 - 95 = 5.0%
        assertEquals(3, health.history.size)
    }
}
