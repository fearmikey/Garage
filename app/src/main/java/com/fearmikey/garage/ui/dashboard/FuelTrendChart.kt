package com.fearmikey.garage.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import kotlin.math.max

@Composable
fun FuelTrendChart(
    entries: List<FuelEconomyEntry>,
    modifier: Modifier = Modifier.fillMaxWidth().height(150.dp)
) {
    if (entries.size < 2) {
        return
    }

    val minMpg = entries.minOf { it.mpg }.coerceAtLeast(0.0)
    val maxMpg = entries.maxOf { it.mpg }.coerceAtLeast(1.0)
    val rangeMpg = max(maxMpg - minMpg, 1.0)

    val minDate = entries.minOf { it.record.date }
    val maxDate = entries.maxOf { it.record.date }
    val rangeDate = max(maxDate - minDate, 1L)

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            entries.forEachIndexed { index, entry ->
                val x = (width * (entry.record.date - minDate).toDouble() / rangeDate.toDouble()).toFloat()
                val y = (height - (height * (entry.mpg - minMpg) / rangeMpg)).toFloat()

                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Draw points
        entries.forEach { entry ->
            val x = (width * (entry.record.date - minDate).toDouble() / rangeDate.toDouble()).toFloat()
            val y = (height - (height * (entry.mpg - minMpg) / rangeMpg)).toFloat()
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
