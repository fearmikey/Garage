package com.fearmikey.garage.data.fuel

import com.fearmikey.garage.data.local.entity.FuelRecord

/**
 * A single full-tank-to-full-tank fuel economy data point, anchored on the
 * fill-up ([record]) that completed the segment.
 */
data class FuelEconomyEntry(
    val record: FuelRecord,
    val milesDriven: Int,
    val gallonsUsed: Double,
) {
    val mpg: Double get() = milesDriven / gallonsUsed
}

/**
 * Computes fuel economy using the standard "fill-to-full" method: MPG for a
 * segment is the mileage driven between two full-tank fill-ups, divided by
 * *every* gallon purchased in between -- including any partial fill-ups
 * along the way, not just the gallons from the final (full) fill-up.
 *
 * A partial fill-up can never produce a standalone data point on its own,
 * since there's no way to know how much fuel was already in the tank when it
 * happened; its gallons are simply rolled forward into the next full-tank
 * segment instead.
 */
object FuelEconomyCalculator {

    /** One entry per full-tank fill-up that completes a measurable segment, oldest first. */
    fun entriesFor(records: List<FuelRecord>): List<FuelEconomyEntry> {
        val sorted = records.sortedBy { it.mileage }
        val lastFullIndex = sorted.indexOfFirst { it.isFullTank }
        if (lastFullIndex == -1) return emptyList()

        val entries = mutableListOf<FuelEconomyEntry>()
        var segmentStart = lastFullIndex
        var gallonsSinceLastFull = 0.0
        for (i in (segmentStart + 1) until sorted.size) {
            gallonsSinceLastFull += sorted[i].gallons
            if (sorted[i].isFullTank) {
                val milesDriven = sorted[i].mileage - sorted[segmentStart].mileage
                if ((milesDriven > 0) && (gallonsSinceLastFull > 0.0)) {
                    entries.add(FuelEconomyEntry(sorted[i], milesDriven, gallonsSinceLastFull))
                }
                segmentStart = i
                gallonsSinceLastFull = 0.0
            }
        }
        return entries
    }

    /**
     * Overall average MPG across every computed segment, weighted by
     * distance -- i.e. total miles driven / total gallons burned across all
     * segments, not a simple average of each segment's individual MPG.
     */
    fun averageMpg(entries: List<FuelEconomyEntry>): Double? {
        if (entries.isEmpty()) return null
        val totalMiles = entries.sumOf { it.milesDriven }
        val totalGallons = entries.sumOf { it.gallonsUsed }
        return totalGallons.takeIf { it > 0.0 }?.let { totalMiles / it }
    }
}
