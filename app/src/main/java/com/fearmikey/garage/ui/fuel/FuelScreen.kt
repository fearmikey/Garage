package com.fearmikey.garage.ui.fuel

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.toDisplayDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun FuelScreen(
    autoOpenAddSheet: Boolean = false,
    viewModel: FuelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (autoOpenAddSheet) showAddSheet = true
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
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FuelSummaryCard(
                        averageMpg = uiState.averageMpg,
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
                        onDelete = { onDeleteRecord(record) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelSummaryCard(
    averageMpg: Double?,
    totalSpent: Double,
    unitSystem: UnitSystem,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryStat(
                label = "Avg. economy",
                value = UnitConverter.formatFuelEconomy(averageMpg, unitSystem),
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                label = "Total spent",
                value = "%s%.2f".format(currencySymbol, totalSpent),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FuelRecordRow(
    record: FuelRecord,
    segmentMpg: Double?,
    unitSystem: UnitSystem,
    currencySymbol: String = "$",
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                segmentMpg?.let {
                    Text(
                        UnitConverter.formatSegmentFuelEconomy(it, unitSystem),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
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
    val canSave = (mileage.toIntOrNull() != null) &&
        (gallonsValue != null) && (gallonsValue > 0.0) &&
        (totalCostValue != null) && (totalCostValue > 0.0)

    if (showDatePicker) {
        val initialUtcMillis = remember(date) {
            val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val selectedUtcDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            date = selectedUtcDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
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
                value = mileage,
                onValueChange = { mileage = it.filter(Char::isDigit) },
                label = { Text("Odometer reading (${unitSystem.distanceUnit})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = gallons,
                onValueChange = { gallons = it.filter { c -> c.isDigit() || c == '.' } },
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
                onValueChange = { totalCost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Total cost") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                supportingText = {
                    if ((gallonsValue != null) && (gallonsValue > 0.0) && (totalCostValue != null)) {
                        val unitLabel = if (unitSystem == UnitSystem.METRIC) "L" else "gal"
                        Text("%s%.3f / %s".format(currencySymbol, totalCostValue / gallonsValue, unitLabel))
                    }
                },
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
                    val inputMileage = mileage.toIntOrNull() ?: 0
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
