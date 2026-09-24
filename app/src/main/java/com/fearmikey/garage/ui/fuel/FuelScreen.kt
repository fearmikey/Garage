package com.fearmikey.garage.ui.fuel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.fuel.BatteryHealthPoint
import com.fearmikey.garage.data.fuel.BatteryHealthSummary
import com.fearmikey.garage.data.local.entity.ChargerSpeed
import com.fearmikey.garage.data.local.entity.ChargerVendorType
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.theme.FuelBestContainerDark
import com.fearmikey.garage.ui.theme.FuelBestContainerLight
import com.fearmikey.garage.ui.theme.FuelBestOnContainerDark
import com.fearmikey.garage.ui.theme.FuelBestOnContainerLight
import com.fearmikey.garage.ui.theme.FuelWorstContainerDark
import com.fearmikey.garage.ui.theme.FuelWorstContainerLight
import com.fearmikey.garage.ui.theme.FuelWorstOnContainerDark
import com.fearmikey.garage.ui.theme.FuelWorstOnContainerLight
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.ThousandsSeparatorVisualTransformation
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.sanitizeMileageInput
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import kotlin.math.abs

@Composable
fun FuelScreen(
    autoOpenAddSheet: Boolean = false,
    onAddSheetConsumed: () -> Unit = {},
    viewModel: FuelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddFuelSheet by remember { mutableStateOf(false) }
    var showAddChargingSheet by remember { mutableStateOf(false) }

    LaunchedEffect(autoOpenAddSheet) {
        if (autoOpenAddSheet) {
            if (uiState.isPureEv) {
                showAddChargingSheet = true
            } else {
                showAddFuelSheet = true
            }
            onAddSheetConsumed()
        }
    }

    FuelContent(
        uiState = uiState,
        onAddFuelClicked = { showAddFuelSheet = true },
        onAddChargingClicked = { showAddChargingSheet = true },
        onDeleteRecord = viewModel::deleteRecord,
        onDeleteChargingRecord = viewModel::deleteChargingRecord,
    )

    if (showAddFuelSheet) {
        AddEditFuelRecordSheet(
            unitSystem = uiState.unitSystem,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddFuelSheet = false },
            onSave = { record ->
                viewModel.saveRecord(record)
                showAddFuelSheet = false
            },
        )
    }

    if (showAddChargingSheet) {
        AddEditChargingRecordSheet(
            unitSystem = uiState.unitSystem,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddChargingSheet = false },
            onSave = { record ->
                viewModel.saveChargingRecord(record)
                showAddChargingSheet = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelContent(
    uiState: FuelUiState,
    onAddFuelClicked: () -> Unit,
    onAddChargingClicked: () -> Unit,
    onDeleteRecord: (FuelRecord) -> Unit,
    onDeleteChargingRecord: (ChargingRecord) -> Unit,
) {
    // 0 = Charging, 1 = Fuel
    var selectedEvTab by remember(uiState.isEvOrPhev, uiState.isPureEv) {
        mutableIntStateOf(if (uiState.isPureEv) 0 else if (uiState.isEvOrPhev && uiState.records.isEmpty() && uiState.chargingRecords.isNotEmpty()) 0 else 1)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (uiState.isEvOrPhev && !uiState.isPureEv) {
                // PHEV: can add either charging or fuel
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = onAddChargingClicked, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Filled.EvStation, contentDescription = "Add charging session")
                    }
                    FloatingActionButton(onClick = onAddFuelClicked) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = "Add fuel fill-up")
                    }
                }
            } else if (uiState.isPureEv) {
                FloatingActionButton(onClick = onAddChargingClicked) {
                    Icon(Icons.Filled.Add, contentDescription = "Add charging session")
                }
            } else {
                FloatingActionButton(onClick = onAddFuelClicked) {
                    Icon(Icons.Filled.Add, contentDescription = "Add fuel fill-up")
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isEvOrPhev && !uiState.isPureEv) {
                // PHEV mode: Toggle between Charging and Fuel
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    SegmentedButton(
                        selected = selectedEvTab == 0,
                        onClick = { selectedEvTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text("⚡ Charging (${uiState.chargingRecords.size})")
                    }
                    SegmentedButton(
                        selected = selectedEvTab == 1,
                        onClick = { selectedEvTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("⛽ Fuel (${uiState.records.size})")
                    }
                }
            }

            val showChargingView = uiState.isPureEv || (uiState.isEvOrPhev && (selectedEvTab == 0))

            if (showChargingView) {
                if (uiState.chargingRecords.isEmpty()) {
                    EmptyState(
                        message = "No charging sessions logged yet.\nTap + to log your first session.",
                    )
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.verticalScrollbar(listState),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            ChargingSummaryCard(
                                averageWhPerMi = uiState.averageWhPerMi,
                                averageMpge = uiState.averageMpge,
                                totalSpent = uiState.totalChargingSpent,
                                unitSystem = uiState.unitSystem,
                                currencySymbol = uiState.currencySymbol,
                            )
                        }
                        if (uiState.batteryHealth.history.isNotEmpty()) {
                            item {
                                BatteryHealthCard(
                                    summary = uiState.batteryHealth,
                                    unitSystem = uiState.unitSystem,
                                )
                            }
                        }
                        items(uiState.chargingRecords, key = { it.id }) { record ->
                            ChargingRecordRow(
                                record = record,
                                unitSystem = uiState.unitSystem,
                                currencySymbol = uiState.currencySymbol,
                                onDelete = { onDeleteChargingRecord(record) },
                            )
                        }
                    }
                }
            } else {
                if (uiState.records.isEmpty()) {
                    EmptyState(
                        message = "No fuel fill-ups logged yet.\nTap + to log your first one.",
                    )
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.verticalScrollbar(listState),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FuelSummaryCard(
                                averageMpg = uiState.averageMpg,
                                bestMpg = uiState.bestMpg,
                                worstMpg = uiState.worstMpg,
                                totalSpent = uiState.totalSpent,
                                unitSystem = uiState.unitSystem,
                                currencySymbol = uiState.currencySymbol,
                            )
                        }
                        items(uiState.records, key = { it.id }) { record ->
                            FuelRecordRow(
                                record = record,
                                segmentMpg = uiState.mpgByRecordId[record.id],
                                unitSystem = uiState.unitSystem,
                                currencySymbol = uiState.currencySymbol,
                                bestMpg = uiState.bestMpg,
                                worstMpg = uiState.worstMpg,
                                onDelete = { onDeleteRecord(record) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChargingSummaryCard(
    averageWhPerMi: Double?,
    averageMpge: Double?,
    totalSpent: Double,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Charging Efficiency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val (avgBg, avgFg) = averageMpgColors()

                MpgStatBox(
                    label = if (unitSystem == UnitSystem.METRIC) "Wh/km" else "Wh/mi",
                    value = UnitConverter.formatWhPerMi(averageWhPerMi, unitSystem),
                    containerColor = avgBg,
                    contentColor = avgFg,
                    modifier = Modifier.weight(1f),
                )
                MpgStatBox(
                    label = if (unitSystem == UnitSystem.METRIC) "kWh/100km" else "kWh/100mi",
                    value = UnitConverter.formatKwhPer100Km(averageWhPerMi, unitSystem),
                    containerColor = avgBg,
                    contentColor = avgFg,
                    modifier = Modifier.weight(1f),
                )
                MpgStatBox(
                    label = "MPGe",
                    value = UnitConverter.formatMpge(averageMpge),
                    containerColor = avgBg,
                    contentColor = avgFg,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Charging Spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%s%.2f".format(currencySymbol, totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BatteryHealthCard(
    summary: BatteryHealthSummary,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Filled.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Battery Health & Degradation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                summary.capacityRetentionPercent?.let { retention ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "%.1f%% Health".format(retention),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Current Est. 100% Range", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = summary.currentRangeAt100?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Initial Est. 100% Range", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = summary.initialRangeAt100?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (summary.history.size >= 2) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "100% State of Charge Range Trend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                BatteryHealthTrendChart(
                    history = summary.history,
                    unitSystem = unitSystem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            }
        }
    }
}

@Composable
private fun BatteryHealthTrendChart(
    history: List<BatteryHealthPoint>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        val ranges = history.map { UnitConverter.displayDistanceValue(it.rangeAt100, unitSystem).toFloat() }
        val minRange = (ranges.minOrNull() ?: 0f) * 0.95f
        val maxRange = (ranges.maxOrNull() ?: 100f) * 1.05f
        val rangeDiff = (maxRange - minRange).coerceAtLeast(1f)

        val width = size.width
        val height = size.height

        val stepX = if (history.size > 1) width / (history.size - 1) else width / 2f

        val points = ranges.mapIndexed { index, rangeVal ->
            val x = index * stepX
            val y = height - ((rangeVal - minRange) / rangeDiff) * height
            Pair(x, y)
        }

        // Draw guideline
        drawLine(
            color = surfaceVariant,
            start = Offset(0f, height / 2f),
            end = Offset(width, height / 2f),
            strokeWidth = 1.dp.toPx(),
        )

        // Draw line path
        val path = Path().apply {
            moveTo(points.first().first, points.first().second)
            for (i in 1 until points.size) {
                lineTo(points[i].first, points[i].second)
            }
        }

        // Fill path gradient
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().first, height)
            lineTo(points.first().first, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
            ),
        )

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 2.5.dp.toPx()),
        )

        // Draw point circles
        for (p in points) {
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(p.first, p.second),
            )
        }
    }
}

@Composable
private fun ChargingRecordRow(
    record: ChargingRecord,
    unitSystem: UnitSystem,
    currencySymbol: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Charging Session") },
            text = { Text("Are you sure you want to delete this charging record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "%.2f kWh".format(record.kwhAdded),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(record.chargerSpeed.displayName) },
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(record.vendor.ifBlank { record.vendorType.displayName }) },
                    )
                }
                Text(
                    "${record.date.toDisplayDate()} · ${UnitConverter.formatDistance(record.mileage, unitSystem)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "%s%.2f total · %d%% → %d%%".format(currencySymbol, record.totalCost, record.batteryPercentStart, record.batteryPercentEnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                record.estimatedRangeAt100?.let { range ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = "Est. 100%% SoC Range: %s".format(UnitConverter.formatDistance(range, unitSystem)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete charging session")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditChargingRecordSheet(
    onDismiss: () -> Unit,
    onSave: (ChargingRecord) -> Unit,
    initial: ChargingRecord? = null,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    currencySymbol: String = "$",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mileage by remember {
        mutableStateOf(
            initial?.let { UnitConverter.displayDistanceValue(it.mileage, unitSystem).toString() }.orEmpty()
        )
    }
    var kwhAdded by remember { mutableStateOf(initial?.kwhAdded?.toString().orEmpty()) }
    var chargerSpeed by remember { mutableStateOf(initial?.chargerSpeed ?: ChargerSpeed.LEVEL_2) }
    var vendorType by remember { mutableStateOf(initial?.vendorType ?: ChargerVendorType.HOME) }
    var vendorName by remember { mutableStateOf(initial?.vendor.orEmpty()) }
    var percentStart by remember { mutableStateOf(initial?.batteryPercentStart?.toString() ?: "20") }
    var percentEnd by remember { mutableStateOf(initial?.batteryPercentEnd?.toString() ?: "80") }
    var totalCost by remember { mutableStateOf(initial?.totalCost?.toString().orEmpty()) }
    var estRange100 by remember {
        mutableStateOf(
            initial?.estimatedRangeAt100?.let { UnitConverter.displayDistanceValue(it, unitSystem).toString() }.orEmpty()
        )
    }
    var rangeAtEnd by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val kwhValue = kwhAdded.toDoubleOrNull()
    val totalCostValue = totalCost.toDoubleOrNull()
    val canSave = (mileage.filter(Char::isDigit).toIntOrNull() != null) &&
        (kwhValue != null) && (kwhValue > 0.0) &&
        (totalCostValue != null)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcDatePickerMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            date = utcMillis.fromUtcDatePickerMillis()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .imePadding()
                .verticalScroll(sheetScrollState)
                .verticalScrollbar(sheetScrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Log Charging Session", style = MaterialTheme.typography.titleLarge)

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = date.toDisplayDate(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = false },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            OutlinedTextField(
                value = kwhAdded,
                onValueChange = { kwhAdded = it.filter { c -> (c.isDigit()) || (c == '.') } },
                label = { Text("kWh added") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Charger Speed", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChargerSpeed.entries.forEach { speed ->
                    FilterChip(
                        selected = chargerSpeed == speed,
                        onClick = { chargerSpeed = speed },
                        label = { Text(speed.displayName) },
                    )
                }
            }

            Text("Location / Vendor", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChargerVendorType.entries.forEach { type ->
                    FilterChip(
                        selected = vendorType == type,
                        onClick = { vendorType = type },
                        label = { Text(type.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = vendorName,
                onValueChange = { vendorName = it },
                label = { Text("Vendor name (optional, e.g. Electrify America)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = percentStart,
                    onValueChange = { percentStart = it.filter(Char::isDigit) },
                    label = { Text("Battery % Start") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = percentEnd,
                    onValueChange = { percentEnd = it.filter(Char::isDigit) },
                    label = { Text("Battery % End") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = totalCost,
                onValueChange = { totalCost = it.filter { c -> (c.isDigit()) || (c == '.') } },
                label = { Text("Total cost") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                supportingText = {
                    if ((kwhValue != null) && (kwhValue > 0.0) && (totalCostValue != null)) {
                        Text("%s%.3f / kWh".format(currencySymbol, totalCostValue / kwhValue))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = sanitizeMileageInput(it) },
                label = { Text("Odometer reading (${unitSystem.distanceUnit})") },
                singleLine = true,
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text("Battery Health (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = estRange100,
                    onValueChange = { estRange100 = it.filter(Char::isDigit) },
                    label = { Text("Est. Range @ 100% SoC (${unitSystem.distanceUnit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = rangeAtEnd,
                    onValueChange = { rangeAtEnd = it.filter(Char::isDigit) },
                    label = { Text("Est. Range at charge end") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val endPct = percentEnd.toIntOrNull()
                        val endRng = rangeAtEnd.toIntOrNull()
                        if (endPct != null && endPct > 0 && endRng != null) {
                            val calc100 = (endRng / (endPct / 100.0)).toInt()
                            estRange100 = calc100.toString()
                        }
                    },
                    enabled = percentEnd.toIntOrNull() != null && rangeAtEnd.toIntOrNull() != null,
                ) {
                    Text("Auto-Calculate")
                }
            }

            Button(
                onClick = {
                    val inputMileage = mileage.filter(Char::isDigit).toIntOrNull() ?: 0
                    val canonicalMileage = UnitConverter.canonicalMilesFromInput(inputMileage, unitSystem)
                    val canonicalKwh = kwhValue ?: 0.0
                    val finalTotalCost = totalCostValue ?: 0.0
                    val inputEstRange = estRange100.toIntOrNull()
                    val canonicalEstRange = inputEstRange?.let { UnitConverter.canonicalMilesFromInput(it, unitSystem) }

                    onSave(
                        ChargingRecord(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            date = date,
                            mileage = canonicalMileage,
                            kwhAdded = canonicalKwh,
                            chargerSpeed = chargerSpeed,
                            batteryPercentStart = percentStart.toIntOrNull() ?: 20,
                            batteryPercentEnd = percentEnd.toIntOrNull() ?: 80,
                            totalCost = finalTotalCost,
                            vendor = vendorName,
                            vendorType = vendorType,
                            estimatedRangeAt100 = canonicalEstRange,
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Charging Session")
            }
        }
    }
}

@Composable
private fun bestMpgColors(): Pair<Color, Color> {
    return if (isSystemInDarkTheme()) {
        FuelBestContainerDark to FuelBestOnContainerDark
    } else {
        FuelBestContainerLight to FuelBestOnContainerLight
    }
}

@Composable
private fun worstMpgColors(): Pair<Color, Color> {
    return if (isSystemInDarkTheme()) {
        FuelWorstContainerDark to FuelWorstOnContainerDark
    } else {
        FuelWorstContainerLight to FuelWorstOnContainerLight
    }
}

@Composable
private fun averageMpgColors(): Pair<Color, Color> {
    return MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
}

@Composable
private fun FuelSummaryCard(
    averageMpg: Double?,
    bestMpg: Double?,
    worstMpg: Double?,
    totalSpent: Double,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fuel Economy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val (bestBg, bestFg) = bestMpgColors()
                val (avgBg, avgFg) = averageMpgColors()
                val (worstBg, worstFg) = worstMpgColors()

                MpgStatBox(
                    label = "Best",
                    value = UnitConverter.formatFuelEconomy(bestMpg, unitSystem),
                    containerColor = bestBg,
                    contentColor = bestFg,
                    modifier = Modifier.weight(1f),
                )
                MpgStatBox(
                    label = "Average",
                    value = UnitConverter.formatFuelEconomy(averageMpg, unitSystem),
                    containerColor = avgBg,
                    contentColor = avgFg,
                    modifier = Modifier.weight(1f),
                )
                MpgStatBox(
                    label = "Worst",
                    value = UnitConverter.formatFuelEconomy(worstMpg, unitSystem),
                    containerColor = worstBg,
                    contentColor = worstFg,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%s%.2f".format(currencySymbol, totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MpgStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FuelRecordRow(
    record: FuelRecord,
    segmentMpg: Double?,
    unitSystem: UnitSystem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    bestMpg: Double? = null,
    worstMpg: Double? = null,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Fill-Up") },
            text = { Text("Are you sure you want to delete this fuel record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        UnitConverter.formatVolume(record.gallons, unitSystem),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!record.isFullTank) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("Partial") })
                    }
                }
                Text(
                    "${record.date.toDisplayDate()} · ${UnitConverter.formatDistance(record.mileage, unitSystem)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "%s%.2f total · %s".format(currencySymbol, record.totalCost, UnitConverter.formatPricePerVolume(record.pricePerGallon, unitSystem, currencySymbol)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                segmentMpg?.let { mpg ->
                    val isBest = (bestMpg != null) && (worstMpg != null) && (bestMpg != worstMpg) && (abs(mpg - bestMpg) < 0.001)
                    val isWorst = (bestMpg != null) && (worstMpg != null) && (bestMpg != worstMpg) && (abs(mpg - worstMpg) < 0.001)
                    val (segmentBg, segmentFg) = when {
                        isBest -> bestMpgColors()
                        isWorst -> worstMpgColors()
                        else -> averageMpgColors()
                    }
                    Surface(
                        color = segmentBg,
                        contentColor = segmentFg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = UnitConverter.formatSegmentFuelEconomy(mpg, unitSystem),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete fill-up")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditFuelRecordSheet(
    onDismiss: () -> Unit,
    onSave: (FuelRecord) -> Unit,
    initial: FuelRecord? = null,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    currencySymbol: String = "$",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mileage by remember {
        mutableStateOf(
            initial?.let { UnitConverter.displayDistanceValue(it.mileage, unitSystem).toString() }.orEmpty()
        )
    }
    var gallons by remember {
        mutableStateOf(
            initial?.let { "%.3f".format(UnitConverter.displayVolumeValue(it.gallons, unitSystem)) }.orEmpty()
        )
    }
    var totalCost by remember { mutableStateOf(initial?.totalCost?.toString().orEmpty()) }
    var isFullTank by remember { mutableStateOf(initial?.isFullTank ?: true) }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val gallonsValue = gallons.toDoubleOrNull()
    val totalCostValue = totalCost.toDoubleOrNull()
    val canSave = (mileage.filter(Char::isDigit).toIntOrNull() != null) &&
        (gallonsValue != null) && (gallonsValue > 0.0) &&
        (totalCostValue != null) && (totalCostValue > 0.0)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcDatePickerMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            date = utcMillis.fromUtcDatePickerMillis()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .imePadding()
                .verticalScroll(sheetScrollState)
                .verticalScrollbar(sheetScrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Log Fuel Fill-Up", style = MaterialTheme.typography.titleLarge)

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = date.toDisplayDate(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = false },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            OutlinedTextField(
                value = gallons,
                onValueChange = { gallons = it.filter { c -> (c.isDigit()) || (c == '.') } },
                label = { Text(if (unitSystem == UnitSystem.METRIC) "Liters" else "Gallons") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = totalCost,
                onValueChange = { totalCost = it.filter { c -> (c.isDigit()) || (c == '.') } },
                label = { Text("Total cost") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                supportingText = {
                    if ((gallonsValue != null) && (gallonsValue > 0.0) && (totalCostValue != null)) {
                        val unitLabel = if (unitSystem == UnitSystem.METRIC) "L" else "gal"
                        Text("%s%.3f / %s".format(currencySymbol, totalCostValue / gallonsValue, unitLabel))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = sanitizeMileageInput(it) },
                label = { Text("Odometer reading (${unitSystem.distanceUnit})") },
                singleLine = true,
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFullTank = !isFullTank },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Filled to full tank", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Turn off if you didn't top off the tank",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isFullTank, onCheckedChange = { isFullTank = it })
            }

            Button(
                onClick = {
                    val inputMileage = mileage.filter(Char::isDigit).toIntOrNull() ?: 0
                    val canonicalMileage = UnitConverter.canonicalMilesFromInput(inputMileage, unitSystem)
                    val inputGallons = gallonsValue ?: 0.0
                    val canonicalGallons = UnitConverter.canonicalGallonsFromInput(inputGallons, unitSystem)
                    val finalTotalCost = totalCostValue ?: 0.0
                    onSave(
                        FuelRecord(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            date = date,
                            mileage = canonicalMileage,
                            gallons = canonicalGallons,
                            totalCost = finalTotalCost,
                            pricePerGallon = if (canonicalGallons > 0.0) finalTotalCost / canonicalGallons else 0.0,
                            isFullTank = isFullTank,
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FuelScreenPreview() {
    GarageTheme {
        FuelContent(
            uiState = FuelUiState(
                records = SampleData.tacomaFuelRecords,
                mpgByRecordId = SampleData.tacomaFuelMpgByRecordId,
                averageMpg = SampleData.tacomaAverageMpg,
                bestMpg = SampleData.tacomaBestMpg,
                worstMpg = SampleData.tacomaWorstMpg,
                totalSpent = SampleData.tacomaFuelRecords.sumOf { it.totalCost },
            ),
            onAddFuelClicked = {},
            onAddChargingClicked = {},
            onDeleteRecord = {},
            onDeleteChargingRecord = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChargingScreenPreview() {
    GarageTheme {
        FuelContent(
            uiState = FuelUiState(
                isEvOrPhev = true,
                isPureEv = true,
                chargingRecords = listOf(
                    ChargingRecord(
                        id = 1,
                        vehicleId = 1,
                        date = System.currentTimeMillis() - 86400000L * 5,
                        mileage = 12000,
                        kwhAdded = 48.5,
                        chargerSpeed = ChargerSpeed.LEVEL_2,
                        batteryPercentStart = 20,
                        batteryPercentEnd = 80,
                        totalCost = 6.30,
                        vendor = "Home",
                        vendorType = ChargerVendorType.HOME,
                        estimatedRangeAt100 = 295,
                    ),
                    ChargingRecord(
                        id = 2,
                        vehicleId = 1,
                        date = System.currentTimeMillis(),
                        mileage = 12220,
                        kwhAdded = 52.0,
                        chargerSpeed = ChargerSpeed.DC_FAST,
                        batteryPercentStart = 15,
                        batteryPercentEnd = 85,
                        totalCost = 14.20,
                        vendor = "Electrify America",
                        vendorType = ChargerVendorType.PUBLIC,
                        estimatedRangeAt100 = 290,
                    ),
                ),
                averageWhPerMi = 236.0,
                averageMpge = 142.8,
                totalChargingSpent = 20.50,
                batteryHealth = BatteryHealthSummary(
                    initialRangeAt100 = 295,
                    currentRangeAt100 = 290,
                    capacityRetentionPercent = 98.3,
                    degradationPercent = 1.7,
                    history = listOf(
                        BatteryHealthPoint(System.currentTimeMillis() - 86400000L * 5, 12000, 295),
                        BatteryHealthPoint(System.currentTimeMillis(), 12220, 290),
                    ),
                ),
            ),
            onAddFuelClicked = {},
            onAddChargingClicked = {},
            onDeleteRecord = {},
            onDeleteChargingRecord = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FuelScreenEmptyPreview() {
    GarageTheme {
        FuelContent(
            uiState = FuelUiState(),
            onAddFuelClicked = {},
            onAddChargingClicked = {},
            onDeleteRecord = {},
            onDeleteChargingRecord = {},
        )
    }
}
