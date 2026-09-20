package com.fearmikey.garage.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adds a smooth, jitter-free vertical scrollbar to a scrollable container backed by [ScrollState].
 */
@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 36.dp,
    padding: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    cornerRadius: Dp = 2.dp,
    autoHide: Boolean = true,
): Modifier {
    var isVisible by remember { mutableStateOf(!autoHide) }

    if (autoHide) {
        LaunchedEffect(state) {
            snapshotFlow { Pair(state.value, state.isScrollInProgress) }
                .collect {
                    if (state.maxValue > 0) {
                        isVisible = true
                        delay(1200.milliseconds)
                        if (!state.isScrollInProgress) {
                            isVisible = false
                        }
                    } else {
                        isVisible = false
                    }
                }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && (state.maxValue > 0)) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbar_alpha_scrollstate",
    )

    if (alpha <= 0f) return this

    return this.drawWithContent {
        drawContent()

        if (state.maxValue <= 0) return@drawWithContent

        val paddingPx = padding.toPx()
        val trackHeight = size.height - (paddingPx * 2)
        if (trackHeight <= 0) return@drawWithContent

        val contentHeight = state.maxValue + size.height
        val visibleFraction = (size.height / contentHeight).coerceIn(0f, 1f)
        val thumbHeightPx = (trackHeight * visibleFraction).coerceIn(minThumbHeight.toPx(), trackHeight)
        val availableTrack = trackHeight - thumbHeightPx
        val scrollProgress = (state.value.toFloat() / state.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbTopPx = paddingPx + (scrollProgress * availableTrack)

        val widthPx = width.toPx()
        val x = size.width - widthPx - paddingPx

        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(x, thumbTopPx),
            size = Size(widthPx, thumbHeightPx),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
}

/**
 * Adds a smooth, jitter-free vertical scrollbar to a lazy container backed by [LazyListState].
 */
@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 36.dp,
    padding: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    cornerRadius: Dp = 2.dp,
    autoHide: Boolean = true,
): Modifier {
    var isVisible by remember { mutableStateOf(!autoHide) }

    if (autoHide) {
        LaunchedEffect(state) {
            snapshotFlow { Triple(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, state.isScrollInProgress) }
                .collect {
                    val layoutInfo = state.layoutInfo
                    val totalItems = layoutInfo.totalItemsCount
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val isScrollable = (totalItems > visibleItems.size) ||
                        ((visibleItems.isNotEmpty()) && ((visibleItems.first().index > 0) || (visibleItems.first().offset < 0)))

                    if (isScrollable) {
                        isVisible = true
                        delay(1200.milliseconds)
                        if (!state.isScrollInProgress) {
                            isVisible = false
                        }
                    } else {
                        isVisible = false
                    }
                }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbar_alpha_lazyliststate",
    )

    if (alpha <= 0f) return this

    return this.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        if ((totalItems == 0) || visibleItems.isEmpty()) return@drawWithContent

        val firstItem = visibleItems.first()
        val lastItem = visibleItems.last()
        val visibleItemCount = visibleItems.size
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()

        val isFullyVisible = (visibleItemCount >= totalItems) &&
            (firstItem.index == 0) &&
            (firstItem.offset == 0) &&
            ((lastItem.offset + lastItem.size) <= viewportHeight.toInt())

        if (isFullyVisible) return@drawWithContent

        val paddingPx = padding.toPx()
        val trackHeight = size.height - (paddingPx * 2)
        if (trackHeight <= 0) return@drawWithContent

        // Smooth average item height to prevent thumb resizing jumpiness
        val avgItemHeight = visibleItems.sumOf { it.size }.toFloat() / visibleItemCount.coerceAtLeast(1)
        val totalContentHeight = (totalItems * avgItemHeight).coerceAtLeast(viewportHeight + 1f)

        val visibleFraction = (viewportHeight / totalContentHeight).coerceIn(0f, 1f)
        val thumbHeightPx = (trackHeight * visibleFraction).coerceIn(minThumbHeight.toPx(), trackHeight)

        val totalScrollableHeight = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
        val scrolledDistance = (firstItem.index * avgItemHeight) - firstItem.offset.toFloat()

        val scrollProgress = if ((lastItem.index == (totalItems - 1)) && ((lastItem.offset + lastItem.size) <= viewportHeight.toInt())) {
            1f
        } else {
            (scrolledDistance / totalScrollableHeight).coerceIn(0f, 1f)
        }

        val availableTrack = trackHeight - thumbHeightPx
        val thumbTopPx = paddingPx + (scrollProgress * availableTrack)

        val widthPx = width.toPx()
        val x = size.width - widthPx - paddingPx

        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(x, thumbTopPx),
            size = Size(widthPx, thumbHeightPx),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
}

/**
 * Adds a smooth, jitter-free vertical scrollbar to a grid container backed by [LazyGridState].
 */
@Composable
fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 36.dp,
    padding: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    cornerRadius: Dp = 2.dp,
    autoHide: Boolean = true,
): Modifier {
    var isVisible by remember { mutableStateOf(!autoHide) }

    if (autoHide) {
        LaunchedEffect(state) {
            snapshotFlow { Triple(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, state.isScrollInProgress) }
                .collect {
                    val layoutInfo = state.layoutInfo
                    val totalItems = layoutInfo.totalItemsCount
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val isScrollable = (totalItems > visibleItems.size) ||
                        ((visibleItems.isNotEmpty()) && ((visibleItems.first().index > 0) || (visibleItems.first().offset.y < 0)))

                    if (isScrollable) {
                        isVisible = true
                        delay(1200.milliseconds)
                        if (!state.isScrollInProgress) {
                            isVisible = false
                        }
                    } else {
                        isVisible = false
                    }
                }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbar_alpha_lazygridstate",
    )

    if (alpha <= 0f) return this

    return this.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        if ((totalItems == 0) || visibleItems.isEmpty()) return@drawWithContent

        val firstItem = visibleItems.first()
        val lastItem = visibleItems.last()
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()

        val isFullyVisible = (visibleItems.size >= totalItems) &&
            (firstItem.index == 0) &&
            (firstItem.offset.y == 0) &&
            ((lastItem.offset.y + lastItem.size.height) <= viewportHeight.toInt())

        if (isFullyVisible) return@drawWithContent

        val paddingPx = padding.toPx()
        val trackHeight = size.height - (paddingPx * 2)
        if (trackHeight <= 0) return@drawWithContent

        val visibleRows = visibleItems.groupBy { it.row }
        val itemsPerRow = visibleRows.values.firstOrNull()?.size?.coerceAtLeast(1) ?: 1
        val totalRows = ceil(totalItems.toFloat() / itemsPerRow).toInt().coerceAtLeast(1)

        val avgRowHeight = if (visibleRows.isNotEmpty()) {
            visibleRows.values.sumOf { rowItems -> rowItems.maxOf { it.size.height } }.toFloat() / visibleRows.size
        } else {
            1f
        }

        val totalContentHeight = (totalRows * avgRowHeight).coerceAtLeast(viewportHeight + 1f)
        val visibleFraction = (viewportHeight / totalContentHeight).coerceIn(0f, 1f)
        val thumbHeightPx = (trackHeight * visibleFraction).coerceIn(minThumbHeight.toPx(), trackHeight)

        val totalScrollableHeight = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
        val scrolledDistance = (firstItem.row * avgRowHeight) - firstItem.offset.y.toFloat()

        val scrollProgress = if ((lastItem.index == (totalItems - 1)) && ((lastItem.offset.y + lastItem.size.height) <= viewportHeight.toInt())) {
            1f
        } else {
            (scrolledDistance / totalScrollableHeight).coerceIn(0f, 1f)
        }

        val availableTrack = trackHeight - thumbHeightPx
        val thumbTopPx = paddingPx + (scrollProgress * availableTrack)

        val widthPx = width.toPx()
        val x = size.width - widthPx - paddingPx

        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(x, thumbTopPx),
            size = Size(widthPx, thumbHeightPx),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
}
