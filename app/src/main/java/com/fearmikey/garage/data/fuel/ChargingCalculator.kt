package com.fearmikey.garage.data.fuel

import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.ui.util.UnitConverter

data class ChargingEfficiencyEntry(
    val record: ChargingRecord,
    val milesDriven: Int,
    val kwhUsed: Double,
) {
    /** Efficiency in Watt-hours per mile. */
    val whPerMi: Double get() = (kwhUsed * 1000.0) / milesDriven

    /** Miles per gallon gasoline equivalent (EPA standard: 33.7 kWh = 1 gallon gasoline). */
    val mpge: Double get() = (milesDriven / kwhUsed) * 33.7
}

data class BatteryHealthPoint(
    val date: Long,
    val mileage: Int,
    val rangeAt100: Int,
)

data class BatteryHealthSummary(
    val initialRangeAt100: Int? = null,
    val currentRangeAt100: Int? = null,
    val capacityRetentionPercent: Double? = null,
    val degradationPercent: Double? = null,
    val history: List<BatteryHealthPoint> = emptyList(),
)

object ChargingCalculator {

    /**
     * Standard EPA energy equivalency: 1 gallon of gasoline = 33.7 kWh.
     */
    const val KWH_PER_GALLON_GASOLINE = 33.7

    /**
     * Computes efficiency segment entries between consecutive charging sessions (sorted by mileage).
     */
    fun entriesFor(records: List<ChargingRecord>): List<ChargingEfficiencyEntry> {
        val sorted = records.sortedBy { it.mileage }
        if (sorted.size < 2) return emptyList()

        val entries = mutableListOf<ChargingEfficiencyEntry>()
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            val milesDriven = curr.mileage - prev.mileage
            val kwhUsed = curr.kwhAdded
            if ((milesDriven > 0) && (kwhUsed > 0.0)) {
                entries.add(ChargingEfficiencyEntry(curr, milesDriven, kwhUsed))
            }
        }
        return entries
    }

    /** Overall average Wh/mi across all calculated entries. */
    fun averageWhPerMi(entries: List<ChargingEfficiencyEntry>): Double? {
        if (entries.isEmpty()) return null
        val totalMiles = entries.sumOf { it.milesDriven }
        val totalKwh = entries.sumOf { it.kwhUsed }
        return if (totalMiles > 0) (totalKwh * 1000.0) / totalMiles else null
    }

    /** Overall average kWh/100km across all calculated entries. */
    fun averageKwhPer100Km(entries: List<ChargingEfficiencyEntry>): Double? {
        if (entries.isEmpty()) return null
        val totalMiles = entries.sumOf { it.milesDriven }
        val totalKwh = entries.sumOf { it.kwhUsed }
        val totalKm = totalMiles * UnitConverter.KM_PER_MILE
        return if (totalKm > 0) (totalKwh / totalKm) * 100.0 else null
    }

    /** Overall average MPGe across all calculated entries. */
    fun averageMpge(entries: List<ChargingEfficiencyEntry>): Double? {
        if (entries.isEmpty()) return null
        val totalMiles = entries.sumOf { it.milesDriven }
        val totalKwh = entries.sumOf { it.kwhUsed }
        return if (totalKwh > 0.0) (totalMiles / totalKwh) * KWH_PER_GALLON_GASOLINE else null
    }

    /**
     * Summarizes battery health degradation trend from charging records that recorded 100% SoC range.
     */
    fun computeBatteryHealth(records: List<ChargingRecord>): BatteryHealthSummary {
        val history = records.asSequence()
            .filter { (it.estimatedRangeAt100 != null) && (it.estimatedRangeAt100 > 0) }
            .sortedBy { it.date }
            .map { BatteryHealthPoint(it.date, it.mileage, it.estimatedRangeAt100!!) }
            .toList()

        if (history.isEmpty()) return BatteryHealthSummary()

        val initial = history.first().rangeAt100
        val current = history.last().rangeAt100
        val retention = if (initial > 0) (current.toDouble() / initial.toDouble()) * 100.0 else 100.0
        val degradation = (100.0 - retention).coerceAtLeast(0.0)

        return BatteryHealthSummary(
            initialRangeAt100 = initial,
            currentRangeAt100 = current,
            capacityRetentionPercent = retention,
            degradationPercent = degradation,
            history = history,
        )
    }
}
