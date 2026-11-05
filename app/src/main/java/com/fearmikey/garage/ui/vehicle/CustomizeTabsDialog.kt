package com.fearmikey.garage.ui.vehicle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fearmikey.garage.R
import kotlin.math.roundToInt

/**
 * Lets the user pick which Vehicle Detail tabs are visible for a single vehicle,
 * and drag-and-drop reorder the tabs smoothly without interruptions.
 * Tabs with [VehicleTab.canHide] == false are shown checked and disabled.
 */
@Composable
fun CustomizeTabsDialog(
    tabOrder: List<VehicleTab> = VehicleTab.entriesList,
    hiddenTabs: Set<VehicleTab>,
    onDismiss: () -> Unit,
    onSave: (orderedTabs: List<VehicleTab>, hiddenTabs: Set<VehicleTab>) -> Unit,
) {
    var tabs by remember { mutableStateOf(tabOrder) }
    var hidden by remember { mutableStateOf(hiddenTabs.map { it.name }.toSet()) }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    var itemHeightPx by remember { mutableFloatStateOf(with(density) { 52.dp.toPx() }) }

    val scrollState = rememberScrollState()

    val activeDragIdx = draggingIndex
    val targetIndex = if (activeDragIdx != null) {
        val shift = (dragOffsetY / itemHeightPx).roundToInt()
        (activeDragIdx + shift).coerceIn(0, tabs.lastIndex)
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.customize_tabs)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    state = scrollState,
                    enabled = draggingIndex == null,
                ),
            ) {
                Text(
                    stringResource(R.string.customize_tabs_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                tabs.forEachIndexed { index, tab ->
                    val visible = tab.name !in hidden
                    val isDragging = activeDragIdx == index

                    val rawTranslationY = when {
                        isDragging -> dragOffsetY
                        activeDragIdx != null && targetIndex != null && index in (activeDragIdx + 1)..targetIndex -> -itemHeightPx
                        activeDragIdx != null && targetIndex != null && index in targetIndex..<activeDragIdx -> itemHeightPx
                        else -> 0f
                    }

                    val animatedTranslationY by animateFloatAsState(
                        targetValue = rawTranslationY,
                        label = "tab_reorder_offset",
                    )

                    val actualTranslationY = if (isDragging) dragOffsetY else animatedTranslationY

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) {
                                    itemHeightPx = coordinates.size.height.toFloat()
                                }
                            }
                            .zIndex(if (isDragging) 2f else 0f)
                            .graphicsLayer {
                                this.translationY = actualTranslationY
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface,
                            )
                            .toggleable(
                                value = visible,
                                enabled = tab.canHide,
                                role = Role.Checkbox,
                                onValueChange = { show ->
                                    hidden = if (show) hidden - tab.name else hidden + tab.name
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = visible, onCheckedChange = null, enabled = tab.canHide)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(stringResource(tab.titleRes), style = MaterialTheme.typography.bodyLarge)
                            if (!tab.canHide) {
                                Text(
                                    stringResource(R.string.customize_tabs_always_shown),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder tab",
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            val fromIdx = draggingIndex
                                            val offset = dragOffsetY
                                            if (fromIdx != null) {
                                                val shift = (offset / itemHeightPx).roundToInt()
                                                val toIdx = (fromIdx + shift).coerceIn(0, tabs.lastIndex)
                                                if (fromIdx != toIdx) {
                                                    val mutable = tabs.toMutableList()
                                                    val item = mutable.removeAt(fromIdx)
                                                    mutable.add(toIdx, item)
                                                    tabs = mutable
                                                }
                                            }
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount
                                        },
                                    )
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        tabs,
                        tabs.filter { it.name in hidden && it.canHide }.toSet(),
                    )
                },
            ) {
                Text(stringResource(R.string.customize_tabs_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.customize_tabs_cancel))
            }
        },
    )
}

@Preview
@Composable
private fun CustomizeTabsDialogPreview() {
    CustomizeTabsDialog(
        tabOrder = VehicleTab.entriesList,
        hiddenTabs = setOf(VehicleTab.MODS, VehicleTab.TRIPS),
        onDismiss = {},
        onSave = { _, _ -> },
    )
}
