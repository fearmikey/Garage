package com.fearmikey.garage.ui.fuel

import com.fearmikey.garage.data.fuel.ChargingEfficiencyEntry
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.data.fuel.TrendStats
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.components.TrendPoint
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

private const val DAY_MS = 86_400_000L

/** Time window applied to a trend chart. */
enum class TrendRange(val label: String) {
    LAST_10("Last 10"),
    SIX_MONTHS("6 mo"),
    ONE_YEAR("1 yr"),
    ALL("All"),
    ;

    /** Filters [items] (oldest first) to this range. */
    fun <T> apply(items: List<T>, dateOf: (T) -> Long, now: Long): List<T> = when (this) {
        LAST_10 -> items.takeLast(10)
        SIX_MONTHS -> items.filter { dateOf(it) >= now - 182 * DAY_MS }
        ONE_YEAR -> items.filter { dateOf(it) >= now - 365 * DAY_MS }
        ALL -> items
    }
}

enum class FuelTrendMetric {
    ECONOMY,
    COST_PER_DISTANCE,
    PRICE_PER_VOLUME,
    ;

    fun label(unitSystem: UnitSystem): String = when (this) {
        ECONOMY -> "Economy"
        COST_PER_DISTANCE -> "Cost/${unitSystem.distanceUnit}"
        PRICE_PER_VOLUME -> "Price/${unitSystem.volumeUnit}"
    }
}

enum class ChargingTrendMetric {
    EFFICIENCY,
    COST_PER_DISTANCE,
    PRICE_PER_KWH,
    ;

    fun label(unitSystem: UnitSystem): String = when (this) {
        EFFICIENCY -> "Efficiency"
        COST_PER_DISTANCE -> "Cost/${unitSystem.distanceUnit}"
        PRICE_PER_KWH -> "Price/kWh"
    }
}

/** Everything a trend card needs to render one metric over one range. */
data class TrendSeries(
    val points: List<TrendPoint>,
    val rollingAverage: List<Double>,
    val average: Double?,
    val higherIsBetter: Boolean,
    val formatValue: (Double) -> String,
    /** Format for the ▲/▼ delta chip (no unit, to stay compact). */
    val formatDelta: (Double) -> String,
) {
    val latest: TrendPoint? get() = points.lastOrNull()
    val deltaFromPrevious: Double?
        get() = if (points.size >= 2) points.last().value - points[points.size - 2].value else null
    val outlierCount: Int get() = points.count { it.isOutlier }

    companion object {
        val EMPTY = TrendSeries(emptyList(), emptyList(), null, true, { "" }, { "" })
    }
}

/**
 * Applies outlier detection and the rolling average over the *full* history (so the first
 * in-range point still has context), then trims to [range].
 */
private fun finalizeSeries(
    all: List<TrendPoint>,
    range: TrendRange,
    now: Long,
    average: (List<TrendPoint>) -> Double?,
    higherIsBetter: Boolean,
    formatValue: (Double) -> String,
    formatDelta: (Double) -> String,
): TrendSeries {
    if (all.isEmpty()) return TrendSeries.EMPTY.copy(formatValue = formatValue, formatDelta = formatDelta)
    val values = all.map { it.value }
    val outliers = TrendStats.outlierIndices(values)
    val rolling = TrendStats.rollingAverage(values)

    val flagged = all.mapIndexed { i, p -> IndexedValue(i, p.copy(isOutlier = i in outliers)) }
    val inRange = range.apply(flagged, { it.value.date }, now)
    val points = inRange.map { it.value }
    return TrendSeries(
        points = points,
        rollingAverage = inRange.map { rolling[it.index] },
        average = if (points.isEmpty()) null else average(points),
        higherIsBetter = higherIsBetter,
        formatValue = formatValue,
        formatDelta = formatDelta,
    )
}

private fun distanceInDisplayUnits(miles: Int, unitSystem: UnitSystem): Double = when (unitSystem) {
    UnitSystem.IMPERIAL -> miles.toDouble()
    UnitSystem.METRIC -> UnitConverter.milesToKm(miles.toDouble())
}

private fun money(currency: String, amount: Double) = "%s%.2f".format(currency, amount)

fun buildFuelTrend(
    metric: FuelTrendMetric,
    range: TrendRange,
    entries: List<FuelEconomyEntry>,
    records: List<FuelRecord>,
    unitSystem: UnitSystem,
    currency: String,
    now: Long = System.currentTimeMillis(),
): TrendSeries = when (metric) {
    FuelTrendMetric.ECONOMY -> {
        val byId = entries.associateBy { it.record.id }
        val all = entries.map { e ->
            TrendPoint(
                id = e.record.id,
                date = e.record.date,
                value = when (unitSystem) {
                    UnitSystem.IMPERIAL -> e.mpg
                    UnitSystem.METRIC -> UnitConverter.mpgToLitersPer100Km(e.mpg) ?: 0.0
                },
                detail = buildString {
                    append(UnitConverter.formatDistance(e.milesDriven, unitSystem))
                    if (e.costUsed > 0.0) append(" · ").append(money(currency, e.costUsed))
                },
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                // Distance-weighted, matching FuelEconomyCalculator.averageMpg.
                val segs = pts.mapNotNull { byId[it.id] }
                val miles = segs.sumOf { it.milesDriven }
                val gallons = segs.sumOf { it.gallonsUsed }
                val mpg = if (gallons > 0.0) miles / gallons else null
                when (unitSystem) {
                    UnitSystem.IMPERIAL -> mpg
                    UnitSystem.METRIC -> mpg?.let { UnitConverter.mpgToLitersPer100Km(it) }
                }
            },
            higherIsBetter = unitSystem == UnitSystem.IMPERIAL,
            formatValue = { "%.1f %s".format(it, unitSystem.fuelEconomyUnit) },
            formatDelta = { "%.1f".format(it) },
        )
    }

    FuelTrendMetric.COST_PER_DISTANCE -> {
        val usable = entries.filter { it.costUsed > 0.0 }
        val byId = usable.associateBy { it.record.id }
        val all = usable.map { e ->
            TrendPoint(
                id = e.record.id,
                date = e.record.date,
                value = e.costUsed / distanceInDisplayUnits(e.milesDriven, unitSystem),
                detail = "${money(currency, e.costUsed)} · ${UnitConverter.formatFuelEconomy(e.mpg, unitSystem)}",
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                val segs = pts.mapNotNull { byId[it.id] }
                val distance = segs.sumOf { distanceInDisplayUnits(it.milesDriven, unitSystem) }
                if (distance > 0.0) segs.sumOf { it.costUsed } / distance else null
            },
            higherIsBetter = false,
            formatValue = { "%s%.3f/%s".format(currency, it, unitSystem.distanceUnit) },
            formatDelta = { "%.3f".format(it) },
        )
    }

    FuelTrendMetric.PRICE_PER_VOLUME -> {
        val usable = records
            .filter { it.gallons > 0.0 && it.totalCost > 0.0 }
            .sortedWith(compareBy({ it.date }, { it.mileage }))
        val byId = usable.associateBy { it.id }
        val all = usable.map { r ->
            TrendPoint(
                id = r.id,
                date = r.date,
                value = when (unitSystem) {
                    UnitSystem.IMPERIAL -> r.totalCost / r.gallons
                    UnitSystem.METRIC -> r.totalCost / UnitConverter.gallonsToLiters(r.gallons)
                },
                detail = "${money(currency, r.totalCost)} · ${UnitConverter.formatVolume(r.gallons, unitSystem)}" +
                    if (!r.isFullTank) " · partial" else "",
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                val recs = pts.mapNotNull { byId[it.id] }
                val volume = recs.sumOf { UnitConverter.displayVolumeValue(it.gallons, unitSystem) }
                if (volume > 0.0) recs.sumOf { it.totalCost } / volume else null
            },
            higherIsBetter = false,
            formatValue = { "%s%.3f/%s".format(currency, it, unitSystem.volumeUnit) },
            formatDelta = { "%.3f".format(it) },
        )
    }
}

fun buildChargingTrend(
    metric: ChargingTrendMetric,
    range: TrendRange,
    entries: List<ChargingEfficiencyEntry>,
    records: List<ChargingRecord>,
    unitSystem: UnitSystem,
    currency: String,
    now: Long = System.currentTimeMillis(),
): TrendSeries = when (metric) {
    ChargingTrendMetric.EFFICIENCY -> {
        val byId = entries.associateBy { it.record.id }
        val all = entries.map { e ->
            TrendPoint(
                id = e.record.id,
                date = e.record.date,
                value = when (unitSystem) {
                    UnitSystem.IMPERIAL -> e.whPerMi
                    UnitSystem.METRIC -> e.whPerMi / UnitConverter.KM_PER_MILE
                },
                detail = "%.1f kWh · %s".format(e.kwhUsed, UnitConverter.formatDistance(e.milesDriven, unitSystem)),
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                val segs = pts.mapNotNull { byId[it.id] }
                val distance = segs.sumOf { distanceInDisplayUnits(it.milesDriven, unitSystem) }
                if (distance > 0.0) segs.sumOf { it.kwhUsed } * 1000.0 / distance else null
            },
            higherIsBetter = false,
            formatValue = { "%.0f Wh/%s".format(it, unitSystem.distanceUnit) },
            formatDelta = { "%.0f".format(it) },
        )
    }

    ChargingTrendMetric.COST_PER_DISTANCE -> {
        val usable = entries.filter { it.record.totalCost > 0.0 }
        val byId = usable.associateBy { it.record.id }
        val all = usable.map { e ->
            TrendPoint(
                id = e.record.id,
                date = e.record.date,
                value = e.record.totalCost / distanceInDisplayUnits(e.milesDriven, unitSystem),
                detail = "${money(currency, e.record.totalCost)} · ${UnitConverter.formatDistance(e.milesDriven, unitSystem)}",
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                val segs = pts.mapNotNull { byId[it.id] }
                val distance = segs.sumOf { distanceInDisplayUnits(it.milesDriven, unitSystem) }
                if (distance > 0.0) segs.sumOf { it.record.totalCost } / distance else null
            },
            higherIsBetter = false,
            formatValue = { "%s%.3f/%s".format(currency, it, unitSystem.distanceUnit) },
            formatDelta = { "%.3f".format(it) },
        )
    }

    ChargingTrendMetric.PRICE_PER_KWH -> {
        val usable = records
            .filter { it.kwhAdded > 0.0 && it.totalCost > 0.0 }
            .sortedWith(compareBy({ it.date }, { it.mileage }))
        val byId = usable.associateBy { it.id }
        val all = usable.map { r ->
            TrendPoint(
                id = r.id,
                date = r.date,
                value = r.totalCost / r.kwhAdded,
                detail = "${money(currency, r.totalCost)} · %.1f kWh · ${r.chargerSpeed.displayName}".format(r.kwhAdded),
            )
        }
        finalizeSeries(
            all = all,
            range = range,
            now = now,
            average = { pts ->
                val recs = pts.mapNotNull { byId[it.id] }
                val kwh = recs.sumOf { it.kwhAdded }
                if (kwh > 0.0) recs.sumOf { it.totalCost } / kwh else null
            },
            higherIsBetter = false,
            formatValue = { "%s%.3f/kWh".format(currency, it) },
            formatDelta = { "%.3f".format(it) },
        )
    }
}
