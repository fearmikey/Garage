package com.fearmikey.garage.ui.fuel

import com.fearmikey.garage.data.fuel.ChargingCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelTrendsTest {

    private val day = 86_400_000L
    private val now = 1_000L * day

    private fun fuel(id: Long, daysAgo: Long, mileage: Int, gallons: Double, cost: Double, full: Boolean = true) =
        FuelRecord(
            id = id,
            vehicleId = 1,
            date = now - daysAgo * day,
            mileage = mileage,
            gallons = gallons,
            totalCost = cost,
            pricePerGallon = cost / gallons,
            isFullTank = full,
        )

    // 400 days of fill-ups, 300 mi / 10 gal (30 mpg) each, $35 each.
    private val records = (0..12).map { i ->
        fuel(id = i.toLong() + 1, daysAgo = 400L - i * 30, mileage = 10_000 + i * 300, gallons = 10.0, cost = 35.0)
    }
    private val entries = FuelEconomyCalculator.entriesFor(records)

    @Test
    fun `entries carry segment cost including partial fill-ups`() {
        val recs = listOf(
            fuel(1, 30, 10_000, 12.0, 40.0),
            fuel(2, 20, 10_150, 5.0, 20.0, full = false),
            fuel(3, 10, 10_300, 5.0, 18.0),
        )
        val e = FuelEconomyCalculator.entriesFor(recs).single()
        assertEquals(38.0, e.costUsed, 1e-9)
        assertEquals(10.0, e.gallonsUsed, 1e-9)
    }

    @Test
    fun `last 10 range keeps the most recent ten points`() {
        val s = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.LAST_10, entries, records, UnitSystem.IMPERIAL, "$", now)
        assertEquals(10, s.points.size)
        assertEquals(entries.last().record.id, s.points.last().id)
        assertEquals(10, s.rollingAverage.size)
    }

    @Test
    fun `six month and one year ranges filter by date`() {
        val six = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.SIX_MONTHS, entries, records, UnitSystem.IMPERIAL, "$", now)
        val year = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.ONE_YEAR, entries, records, UnitSystem.IMPERIAL, "$", now)
        val all = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.ALL, entries, records, UnitSystem.IMPERIAL, "$", now)
        assertTrue(six.points.all { it.date >= now - 182 * day })
        assertTrue(year.points.all { it.date >= now - 365 * day })
        assertTrue(six.points.size < year.points.size)
        assertTrue(year.points.size < all.points.size)
        assertEquals(entries.size, all.points.size)
    }

    @Test
    fun `economy series in imperial is mpg and higher is better`() {
        val s = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.ALL, entries, records, UnitSystem.IMPERIAL, "$", now)
        assertTrue(s.higherIsBetter)
        assertEquals(30.0, s.points.first().value, 1e-9)
        assertEquals(30.0, s.average!!, 1e-9)
    }

    @Test
    fun `economy series in metric is L per 100km and lower is better`() {
        val s = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.ALL, entries, records, UnitSystem.METRIC, "€", now)
        assertFalse(s.higherIsBetter)
        assertEquals(UnitConverter.mpgToLitersPer100Km(30.0)!!, s.points.first().value, 1e-9)
    }

    @Test
    fun `cost per distance uses segment cost over distance`() {
        val s = buildFuelTrend(FuelTrendMetric.COST_PER_DISTANCE, TrendRange.ALL, entries, records, UnitSystem.IMPERIAL, "$", now)
        assertEquals(35.0 / 300.0, s.points.first().value, 1e-9)
        assertEquals(35.0 / 300.0, s.average!!, 1e-9)
        assertFalse(s.higherIsBetter)
    }

    @Test
    fun `price per volume uses every fill-up including the first`() {
        val s = buildFuelTrend(FuelTrendMetric.PRICE_PER_VOLUME, TrendRange.ALL, entries, records, UnitSystem.METRIC, "$", now)
        assertEquals(records.size, s.points.size)
        assertEquals(35.0 / UnitConverter.gallonsToLiters(10.0), s.points.first().value, 1e-9)
    }

    @Test
    fun `outliers are flagged and survive range filtering`() {
        // Make the most recent segment a mistyped mileage: 900 mi on 10 gal.
        val bad = records.dropLast(1) + records.last().copy(mileage = records[records.size - 2].mileage + 900)
        val badEntries = FuelEconomyCalculator.entriesFor(bad)
        val s = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.LAST_10, badEntries, bad, UnitSystem.IMPERIAL, "$", now)
        assertTrue(s.points.last().isOutlier)
        assertEquals(1, s.outlierCount)
    }

    @Test
    fun `empty input yields empty series`() {
        val s = buildFuelTrend(FuelTrendMetric.ECONOMY, TrendRange.ALL, emptyList(), emptyList(), UnitSystem.IMPERIAL, "$", now)
        assertTrue(s.points.isEmpty())
    }

    @Test
    fun `charging efficiency series is Wh per distance`() {
        val charging = (0..4).map { i ->
            ChargingRecord(
                id = i.toLong() + 1,
                vehicleId = 1,
                date = now - (50L - i * 10) * day,
                mileage = 1_000 + i * 200,
                kwhAdded = 50.0,
                totalCost = 10.0,
            )
        }
        val chargingEntries = ChargingCalculator.entriesFor(charging)
        val s = buildChargingTrend(ChargingTrendMetric.EFFICIENCY, TrendRange.ALL, chargingEntries, charging, UnitSystem.IMPERIAL, "$", now)
        assertEquals(4, s.points.size)
        assertEquals(250.0, s.points.first().value, 1e-9)
        assertNotNull(s.average)
        assertFalse(s.higherIsBetter)

        val price = buildChargingTrend(ChargingTrendMetric.PRICE_PER_KWH, TrendRange.ALL, chargingEntries, charging, UnitSystem.IMPERIAL, "$", now)
        assertEquals(5, price.points.size)
        assertEquals(0.2, price.points.first().value, 1e-9)
    }
}
