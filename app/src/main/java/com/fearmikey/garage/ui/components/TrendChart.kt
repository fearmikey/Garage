package com.fearmikey.garage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.fuel.TrendStats
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.toDisplayDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One sample on a [TrendChart].
 *
 * @param id identifier the caller can use to look up the underlying record (e.g. a DB id).
 * @param date epoch millis, shown in the tooltip and footer.
 * @param value already converted into the unit being displayed.
 * @param detail optional secondary line for the tooltip (e.g. cost, distance).
 * @param isOutlier draws the point as a hollow "review me" marker.
 */
@Immutable
data class TrendPoint(
    val id: Long,
    val date: Long,
    val value: Double,
    val detail: String? = null,
    val isOutlier: Boolean = false,
)

/**
 * Generic, theme-aware line chart used for fuel economy, costs, EV efficiency and battery health.
 *
 * Points are spaced evenly along X (by index) so clustered dates stay readable.
 *
 * When [interactive] is true, tapping or horizontally dragging over the chart selects the nearest
 * point and shows a tooltip. If [onPointClick] is provided, the tooltip becomes tappable
 * (e.g. to open the underlying record for editing).
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    formatValue: (Double) -> String = { "%.1f".format(it) },
    average: Double? = null,
    rollingAverage: List<Double>? = null,
    interactive: Boolean = false,
    showYAxisLabels: Boolean = true,
    onPointClick: ((TrendPoint) -> Unit)? = null,
    pointClickLabel: String = "Tap to edit",
    lineColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    if (points.size < 2) return

    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val rollingColor = MaterialTheme.colorScheme.tertiary
    val outlierColor = MaterialTheme.colorScheme.error
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    val yAxisStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
    )

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val insetPx = with(density) { 8.dp.toPx() }

        val geometry = remember(points, average, rollingAverage, widthPx, heightPx, insetPx) {
            TrendGeometry(
                values = points.map { it.value },
                extraValues = listOfNotNull(average) + rollingAverage.orEmpty(),
                width = widthPx,
                height = heightPx,
                inset = insetPx,
            )
        }
        val currentGeometry by rememberUpdatedState(geometry)

        val gestureModifier = if (interactive) {
            Modifier
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val index = currentGeometry.nearestIndex(offset.x)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> selectedIndex = currentGeometry.nearestIndex(offset.x) },
                    ) { change, _ ->
                        change.consume()
                        selectedIndex = currentGeometry.nearestIndex(change.position.x)
                    }
                }
        } else {
            Modifier
        }

        Canvas(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
            drawTrend(
                geometry = geometry,
                points = points,
                average = average,
                rollingAverage = rollingAverage,
                selectedIndex = selectedIndex,
                lineColor = lineColor,
                guideColor = guideColor,
                rollingColor = rollingColor,
                outlierColor = outlierColor,
                backgroundColor = backgroundColor,
                highlightAllPoints = interactive,
                showYAxisLabels = showYAxisLabels,
                formatValue = formatValue,
                textMeasurer = textMeasurer,
                yAxisStyle = yAxisStyle,
            )
        }

        selectedIndex?.let { index ->
            val point = points[index]
            val anchor = geometry.offsetFor(index)
            var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
            val margin = with(density) { 4.dp.roundToPx() }

            Surface(
                onClick = { onPointClick?.invoke(point) },
                enabled = onPointClick != null,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .offset {
                        val x = (anchor.x - tooltipSize.width / 2f).roundToInt()
                            .coerceIn(0, (widthPx.toInt() - tooltipSize.width).coerceAtLeast(0))
                        // Keep the tooltip on the opposite half from the point so it never hides it.
                        val y = if (anchor.y > heightPx / 2f) {
                            margin
                        } else {
                            (heightPx.toInt() - tooltipSize.height - margin).coerceAtLeast(0)
                        }
                        IntOffset(x, y)
                    }
                    .onSizeChanged { tooltipSize = it },
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = point.date.toDisplayDate(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatValue(point.value),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    point.detail?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall)
                    }
                    if (point.isOutlier) {
                        Text(
                            text = "Unusual reading",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.errorContainer,
                        )
                    }
                    if (onPointClick != null) {
                        Text(
                            text = "$pointClickLabel ›",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact headline: latest value plus a ▲/▼ change chip. The chip colour reflects whether the
 * change is an improvement ([higherIsBetter] decides direction), not just its sign.
 */
@Composable
fun TrendHeadline(
    title: String,
    latestText: String,
    delta: Double?,
    higherIsBetter: Boolean,
    modifier: Modifier = Modifier,
    deltaFormat: (Double) -> String = { "%.1f".format(it) },
    titleStyleEmphasis: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = if (titleStyleEmphasis) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = if (titleStyleEmphasis) FontWeight.Bold else null,
            color = if (titleStyleEmphasis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = latestText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (delta != null && abs(delta) >= 0.0005) {
            val improved = if (higherIsBetter) delta > 0 else delta < 0
            Surface(
                color = if (improved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (improved) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = "%s %s".format(if (delta > 0) "▲" else "▼", deltaFormat(abs(delta))),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Start date · optional centre label · end date, shown under a [TrendChart]. */
@Composable
fun TrendFooter(
    startDate: Long,
    endDate: Long,
    modifier: Modifier = Modifier,
    centerText: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        val style = MaterialTheme.typography.labelSmall
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text(text = startDate.toDisplayDate(), style = style, color = color)
        Spacer(modifier = Modifier.weight(1f))
        centerText?.let {
            Text(text = it, style = style, color = color)
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(text = endDate.toDisplayDate(), style = style, color = color)
    }
}

/** Maps data values to canvas coordinates; shared by drawing, hit-testing and tooltip placement. */
private class TrendGeometry(
    val values: List<Double>,
    extraValues: List<Double>,
    width: Float,
    height: Float,
    inset: Float,
) {
    val left = inset
    val right = width - inset
    val top = inset
    val bottom = height - inset
    private val stepX = (right - left) / (values.size - 1).coerceAtLeast(1)
    private val lo: Double
    private val hi: Double

    val minDataValue: Double = values.min()
    val maxDataValue: Double = values.max()

    init {
        val all = values + extraValues
        val minV = all.min()
        val maxV = all.max()
        // Pad the range so small wiggles don't look dramatic and flat data sits centred.
        val mid = (minV + maxV) / 2
        val span = (maxV - minV)
            .coerceAtLeast(abs(mid) * 0.1)
            .coerceAtLeast(1e-6)
        lo = mid - span * 0.6
        hi = mid + span * 0.6
    }

    fun x(index: Int): Float = left + index * stepX
    fun y(value: Double): Float = (bottom - ((value - lo) / (hi - lo)) * (bottom - top)).toFloat()
    fun offsetFor(index: Int): Offset = Offset(x(index), y(values[index]))

    fun nearestIndex(xPos: Float): Int =
        ((xPos - left) / stepX).roundToInt().coerceIn(0, values.lastIndex)

    fun smoothPath(series: List<Double>): Path = Path().apply {
        val pts = series.mapIndexed { i, v -> Offset(x(i), y(v)) }
        moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) {
            val p0 = pts[i - 1]
            val p1 = pts[i]
            val midX = (p0.x + p1.x) / 2f
            // Horizontal-midpoint control points: smooth, and never overshoots the data in Y.
            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }
    }
}

private fun DrawScope.drawTrend(
    geometry: TrendGeometry,
    points: List<TrendPoint>,
    average: Double?,
    rollingAverage: List<Double>?,
    selectedIndex: Int?,
    lineColor: Color,
    guideColor: Color,
    rollingColor: Color,
    outlierColor: Color,
    backgroundColor: Color,
    highlightAllPoints: Boolean,
    showYAxisLabels: Boolean,
    formatValue: (Double) -> String,
    textMeasurer: TextMeasurer,
    yAxisStyle: TextStyle,
) {
    val g = geometry

    // Baseline
    drawLine(
        color = guideColor.copy(alpha = 0.6f),
        start = Offset(g.left, g.bottom),
        end = Offset(g.right, g.bottom),
        strokeWidth = 1.dp.toPx(),
    )

    if (showYAxisLabels && points.size >= 2) {
        val yMax = g.y(g.maxDataValue)
        val yMin = g.y(g.minDataValue)

        // Draw Max guide (top left)
        drawLine(
            color = guideColor.copy(alpha = 0.35f),
            start = Offset(g.left, yMax),
            end = Offset(g.right, yMax),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )

        // Draw Min guide (bottom left) if distinct from Max
        if (abs(yMin - yMax) > 16.dp.toPx()) {
            drawLine(
                color = guideColor.copy(alpha = 0.35f),
                start = Offset(g.left, yMin),
                end = Offset(g.right, yMin),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }

        // Draw Average guide line
        average?.let { avg ->
            val yAvg = g.y(avg)
            drawLine(
                color = guideColor,
                start = Offset(g.left, yAvg),
                end = Offset(g.right, yAvg),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }
    } else {
        average?.let {
            val y = g.y(it)
            drawLine(
                color = guideColor,
                start = Offset(g.left, y),
                end = Offset(g.right, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }
    }

    val linePath = g.smoothPath(g.values)
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(g.x(points.lastIndex), g.bottom)
        lineTo(g.x(0), g.bottom)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
            startY = g.top,
            endY = g.bottom,
        ),
    )

    // Faint rolling-average line sits under the main line.
    if (rollingAverage != null && rollingAverage.size == points.size) {
        drawPath(
            path = g.smoothPath(rollingAverage),
            color = rollingColor.copy(alpha = 0.55f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Draw Y-axis labels on top of lines
    if (showYAxisLabels && points.size >= 2) {
        val yMax = g.y(g.maxDataValue)
        val yMin = g.y(g.minDataValue)

        val maxLabelResult = textMeasurer.measure(formatValue(g.maxDataValue), yAxisStyle)
        val minLabelResult = textMeasurer.measure(formatValue(g.minDataValue), yAxisStyle)

        val padding = 2.dp.toPx()
        val bgAlpha = 0.85f

        fun drawLabelWithBackground(result: androidx.compose.ui.text.TextLayoutResult, topLeft: Offset) {
            drawRoundRect(
                color = backgroundColor.copy(alpha = bgAlpha),
                topLeft = Offset(topLeft.x - padding, topLeft.y - padding),
                size = androidx.compose.ui.geometry.Size(result.size.width + padding * 2, result.size.height + padding * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            drawText(textLayoutResult = result, topLeft = topLeft)
        }

        // Max Label (top left)
        val maxTopLeft = Offset(g.left + padding, (yMax - maxLabelResult.size.height - padding).coerceAtLeast(0f))
        drawLabelWithBackground(maxLabelResult, maxTopLeft)

        // Min Label (bottom left)
        if (abs(yMin - yMax) > 16.dp.toPx()) {
            val minTopLeft = Offset(
                g.left + padding,
                (yMin - minLabelResult.size.height - padding).coerceIn(0f, g.bottom - minLabelResult.size.height)
            )
            drawLabelWithBackground(minLabelResult, minTopLeft)
        }
    }

    selectedIndex?.let {
        val x = g.x(it)
        drawLine(
            color = guideColor,
            start = Offset(x, g.top),
            end = Offset(x, g.bottom),
            strokeWidth = 1.dp.toPx(),
        )
    }

    points.forEachIndexed { i, point ->
        val center = g.offsetFor(i)
        val isSelected = i == selectedIndex
        val isLast = i == points.lastIndex
        when {
            point.isOutlier -> {
                // Hollow "review me" marker.
                drawCircle(color = backgroundColor, radius = 4.5.dp.toPx(), center = center)
                drawCircle(
                    color = outlierColor,
                    radius = (if (isSelected) 5.5.dp else 4.5.dp).toPx(),
                    center = center,
                    style = Stroke(width = 1.75.dp.toPx()),
                )
            }
            isSelected || isLast -> {
                drawCircle(color = backgroundColor, radius = 5.5.dp.toPx(), center = center)
                drawCircle(color = lineColor, radius = 3.75.dp.toPx(), center = center)
            }
            highlightAllPoints -> {
                drawCircle(color = lineColor, radius = 2.dp.toPx(), center = center)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TrendChartPreview() {
    val values = listOf(24.1, 25.3, 23.8, 26.0, 14.2, 25.1, 24.7, 26.4, 25.9, 27.0)
    val outliers = TrendStats.outlierIndices(values)
    val points = values.mapIndexed { i, v ->
        TrendPoint(
            id = i.toLong(),
            date = 1_700_000_000_000L + i * 7L * 86_400_000L,
            value = v,
            isOutlier = i in outliers,
        )
    }
    GarageTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column(modifier = Modifier.padding(16.dp)) {
                TrendHeadline(title = "Fuel Trend", latestText = "27.0 mpg", delta = 1.1, higherIsBetter = true)
                Spacer(modifier = Modifier.height(8.dp))
                TrendChart(
                    points = points,
                    average = values.average(),
                    rollingAverage = TrendStats.rollingAverage(values),
                    interactive = true,
                    formatValue = { "%.1f mpg".format(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                TrendFooter(startDate = points.first().date, endDate = points.last().date, centerText = "avg 24.3 mpg")
            }
        }
    }
}
