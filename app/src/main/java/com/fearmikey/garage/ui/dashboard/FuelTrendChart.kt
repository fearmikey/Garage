package com.fearmikey.garage.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.ui.components.TrendChart
import com.fearmikey.garage.ui.components.TrendFooter
import com.fearmikey.garage.ui.components.TrendHeadline
import com.fearmikey.garage.ui.fuel.FuelTrendMetric
import com.fearmikey.garage.ui.fuel.TrendRange
import com.fearmikey.garage.ui.fuel.buildFuelTrend
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitSystem

/**
 * Compact, non-interactive fuel-economy sparkline for the dashboard vehicle card.
 * The full interactive version (ranges, metrics, tooltips) lives on the Fuel screen.
 */
@Composable
fun FuelTrendChart(
    entries: List<FuelEconomyEntry>,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    chartHeight: Dp = 56.dp,
) {
    if (entries.size < 2) return

    val series = remember(entries, unitSystem) {
        buildFuelTrend(
            metric = FuelTrendMetric.ECONOMY,
            range = TrendRange.LAST_10,
            entries = entries,
            records = emptyList(),
            unitSystem = unitSystem,
            currency = "",
        )
    }
    // Keep the glance view minimal: no outlier markers here.
    val points = remember(series) { series.points.map { it.copy(isOutlier = false) } }
    if (points.size < 2) return

    val latestText = series.formatValue(points.last().value)

    Column(
        modifier = modifier.semantics {
            contentDescription = "Fuel trend over last ${points.size} fill-ups. Latest $latestText."
        },
    ) {
        TrendHeadline(
            title = "Fuel Trend",
            latestText = latestText,
            delta = series.deltaFromPrevious,
            higherIsBetter = series.higherIsBetter,
            deltaFormat = series.formatDelta,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrendChart(
            points = points,
            formatValue = series.formatValue,
            average = series.average,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        )
        Spacer(modifier = Modifier.height(4.dp))
        TrendFooter(
            startDate = points.first().date,
            endDate = points.last().date,
            centerText = series.average?.let { "avg ${series.formatValue(it)}" },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FuelTrendChartPreview() {
    GarageTheme {
        Surface {
            FuelTrendChart(
                entries = FuelEconomyCalculator.entriesFor(SampleData.tacomaFuelRecords),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
