package com.fearmikey.garage.ui.fuel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.components.EmptyState
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
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.formatMileageInput
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
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
    var showAddSheet by remember { mutableStateOf(value = false) }

    LaunchedEffect(autoOpenAddSheet) {
        if (autoOpenAddSheet) {
            showAddSheet = true
            onAddSheetConsumed()
        }
    }

    FuelContent(
        uiState = uiState,
        onAddClicked = { showAddSheet = true },
        onDeleteRecord = viewModel::deleteRecord,
    )

    if (showAddSheet) {
        AddEditFuelRecordSheet(
            unitSystem = uiState.unitSystem,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddSheet = false },
            onSave = { record ->
                viewModel.saveRecord(record)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun FuelContent(
    uiState: FuelUiState,
    onAddClicked: () -> Unit,
    onDeleteRecord: (FuelRecord) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Add fuel fill-up")
            }
        },
    ) { innerPadding ->
        if (uiState.records.isEmpty()) {
            EmptyState(
                message = "No fuel fill-ups logged yet.\nTap + to log your first one.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScrollbar(listState),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
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
            initial?.let { formatMileageInput(UnitConverter.displayDistanceValue(it.mileage, unitSystem).toString()) }.orEmpty()
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
                onValueChange = { mileage = formatMileageInput(it) },
                label = { Text("Odometer reading (${unitSystem.distanceUnit})") },
                singleLine = true,
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
            onAddClicked = {},
            onDeleteRecord = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FuelScreenEmptyPreview() {
    GarageTheme {
        FuelContent(
            uiState = FuelUiState(),
            onAddClicked = {},
            onDeleteRecord = {},
        )
    }
}
