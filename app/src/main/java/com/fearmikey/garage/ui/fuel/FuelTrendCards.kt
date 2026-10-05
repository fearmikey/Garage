package com.fearmikey.garage.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.fuel.ChargingEfficiencyEntry
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.components.TrendChart
import com.fearmikey.garage.ui.components.TrendFooter
import com.fearmikey.garage.ui.components.TrendHeadline
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitSystem

/** Interactive fuel trend card: metric + range selectors, tooltip, tap-to-edit. */
@Composable
internal fun FuelTrendCard(
    entries: List<FuelEconomyEntry>,
    records: List<FuelRecord>,
    unitSystem: UnitSystem,
    currencySymbol: String,
    onEditRecord: (FuelRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by rememberSaveable { mutableStateOf(FuelTrendMetric.ECONOMY) }
    var range by rememberSaveable { mutableStateOf(TrendRange.LAST_10) }
    val series = remember(metric, range, entries, records, unitSystem, currencySymbol) {
        buildFuelTrend(metric, range, entries, records, unitSystem, currencySymbol)
    }
    val recordsById = remember(records) { records.associateBy { it.id } }

    TrendCard(
        title = "Fuel Trends",
        metricLabels = FuelTrendMetric.entries.map { it.label(unitSystem) },
        selectedMetricIndex = metric.ordinal,
        onMetricSelected = { metric = FuelTrendMetric.entries[it] },
        range = range,
        onRangeSelected = { range = it },
        series = series,
        rollingLabel = "3-fill avg",
        onPointClick = { id -> recordsById[id]?.let(onEditRecord) },
        modifier = modifier,
    )
}

/** Interactive EV charging trend card, mirroring [FuelTrendCard]. */
@Composable
internal fun ChargingTrendCard(
    entries: List<ChargingEfficiencyEntry>,
    records: List<ChargingRecord>,
    unitSystem: UnitSystem,
    currencySymbol: String,
    onEditRecord: (ChargingRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by rememberSaveable { mutableStateOf(ChargingTrendMetric.EFFICIENCY) }
    var range by rememberSaveable { mutableStateOf(TrendRange.LAST_10) }
    val series = remember(metric, range, entries, records, unitSystem, currencySymbol) {
        buildChargingTrend(metric, range, entries, records, unitSystem, currencySymbol)
    }
    val recordsById = remember(records) { records.associateBy { it.id } }

    TrendCard(
        title = "Charging Trends",
        metricLabels = ChargingTrendMetric.entries.map { it.label(unitSystem) },
        selectedMetricIndex = metric.ordinal,
        onMetricSelected = { metric = ChargingTrendMetric.entries[it] },
        range = range,
        onRangeSelected = { range = it },
        series = series,
        rollingLabel = "3-session avg",
        onPointClick = { id -> recordsById[id]?.let(onEditRecord) },
        modifier = modifier,
    )
}

private const val MIN_POINTS_FOR_ROLLING = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendCard(
    title: String,
    metricLabels: List<String>,
    selectedMetricIndex: Int,
    onMetricSelected: (Int) -> Unit,
    range: TrendRange,
    onRangeSelected: (TrendRange) -> Unit,
    series: TrendSeries,
    rollingLabel: String,
    onPointClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TrendHeadline(
                title = title,
                latestText = series.points.lastOrNull()?.let { series.formatValue(it.value) } ?: "—",
                delta = series.deltaFromPrevious,
                higherIsBetter = series.higherIsBetter,
                deltaFormat = series.formatDelta,
                titleStyleEmphasis = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                metricLabels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = index == selectedMetricIndex,
                        onClick = { onMetricSelected(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = metricLabels.size),
                        icon = {},
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendRange.entries.forEach { option ->
                    FilterChip(
                        selected = option == range,
                        onClick = { onRangeSelected(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (series.points.size >= 2) {
                val latestText = series.formatValue(series.points.last().value)
                // A rolling average over 3 is only informative once there are a few more points than the window.
                val showRolling = series.points.size >= MIN_POINTS_FOR_ROLLING
                TrendChart(
                    points = series.points,
                    formatValue = series.formatValue,
                    average = series.average,
                    rollingAverage = series.rollingAverage.takeIf { showRolling },
                    interactive = true,
                    onPointClick = { onPointClick(it.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .semantics {
                            contentDescription =
                                "$title chart, ${series.points.size} points, latest $latestText. " +
                                    "Tap or drag to inspect points."
                        },
                )
                Spacer(modifier = Modifier.height(4.dp))
                TrendFooter(
                    startDate = series.points.first().date,
                    endDate = series.points.last().date,
                    centerText = series.average?.let { "avg ${series.formatValue(it)}" },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        if (showRolling) append("Faint line: $rollingLabel · ")
                        append("Dashed: average")
                        if (series.outlierCount > 0) {
                            append(" · Hollow dots: unusual readings, tap to review")
                        } else {
                            append(" · Tap or drag to inspect")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Not enough data in this range yet. Try a wider range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun FuelTrendCardPreview() {
    GarageTheme {
        FuelTrendCard(
            entries = FuelEconomyCalculator.entriesFor(SampleData.tacomaFuelRecords),
            records = SampleData.tacomaFuelRecords,
            unitSystem = UnitSystem.IMPERIAL,
            currencySymbol = "$",
            onEditRecord = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
