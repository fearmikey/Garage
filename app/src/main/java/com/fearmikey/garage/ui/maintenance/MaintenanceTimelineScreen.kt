package com.fearmikey.garage.ui.maintenance

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.repository.MaintenanceSortOrder
import com.fearmikey.garage.data.schedule.MaintenanceScheduleRules
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
fun MaintenanceTimelineScreen(
    autoOpenAddSheet: Boolean = false,
    viewModel: MaintenanceTimelineViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (autoOpenAddSheet) showAddSheet = true
    }

    MaintenanceTimelineContent(
        records = records,
        sortOrder = sortOrder,
        unitSystem = unitSystem,
        onSortOrderChanged = viewModel::setSortOrder,
        onAddClicked = { showAddSheet = true },
        onDeleteRecord = viewModel::deleteRecord,
    )

    if (showAddSheet) {
        AddEditMaintenanceRecordSheet(
            unitSystem = unitSystem,
            onDismiss = { showAddSheet = false },
            onSave = { record ->
                viewModel.saveRecord(record)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun MaintenanceTimelineContent(
    records: List<MaintenanceRecord>,
    sortOrder: MaintenanceSortOrder,
    unitSystem: UnitSystem,
    onSortOrderChanged: (MaintenanceSortOrder) -> Unit,
    onAddClicked: () -> Unit,
    onDeleteRecord: (MaintenanceRecord) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Add maintenance record")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortOrder == MaintenanceSortOrder.DATE,
                    onClick = { onSortOrderChanged(MaintenanceSortOrder.DATE) },
                    label = { Text("By date") },
                )
                FilterChip(
                    selected = sortOrder == MaintenanceSortOrder.MILEAGE,
                    onClick = { onSortOrderChanged(MaintenanceSortOrder.MILEAGE) },
                    label = { Text("By mileage") },
                )
            }

            if (records.isEmpty()) {
                EmptyState(message = "No maintenance logged yet.\nTap + to add your first record.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        MaintenanceRecordRow(
                            record = record,
                            unitSystem = unitSystem,
                            onDelete = { onDeleteRecord(record) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceRecordRow(
    record: MaintenanceRecord,
    unitSystem: UnitSystem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.category.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(record.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.date.toDisplayDate()} · ${UnitConverter.formatDistance(record.mileage, unitSystem)} · $${"%.2f".format(record.cost)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete record")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditMaintenanceRecordSheet(
    onDismiss: () -> Unit,
    onSave: (MaintenanceRecord) -> Unit,
    initial: MaintenanceRecord? = null,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var mileage by remember {
        mutableStateOf(
            initial?.let { UnitConverter.displayDistanceValue(it.mileage, unitSystem).toString() }.orEmpty()
        )
    }
    var cost by remember { mutableStateOf(initial?.cost?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: MaintenanceCategory.OTHER) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf(initial?.taskName) }
    var taskMenuExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // The known task names for the selected category, e.g. "Brake fluid flush" vs.
    // "Coolant flush" both under Fluids. Picking one of these (rather than "Other / custom")
    // is what lets MaintenanceScheduleEngine track each task's due date independently instead
    // of conflating every task in the same category.
    val taskOptions = remember(category) {
        MaintenanceScheduleRules.rules
            .filter { it.category == category }
            .map { it.taskName }
            .distinctBy { it.lowercase() }
            .sorted()
    }

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
            Text("Add Maintenance Record", style = MaterialTheme.typography.titleLarge)

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

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .focusProperties { canFocus = false },
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    MaintenanceCategory.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.displayName) },
                            onClick = {
                                category = entry
                                taskName = null
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }

            if (taskOptions.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = taskMenuExpanded,
                    onExpandedChange = { taskMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = taskName ?: "Other / custom",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Specific task") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .focusProperties { canFocus = false },
                    )
                    ExposedDropdownMenu(
                        expanded = taskMenuExpanded,
                        onDismissRequest = { taskMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Other / custom") },
                            onClick = {
                                taskName = null
                                taskMenuExpanded = false
                            },
                        )
                        taskOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    taskName = option
                                    if (description.isBlank()) description = option
                                    taskMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it.filter(Char::isDigit) },
                label = { Text("Mileage (${unitSystem.distanceUnit})") },
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
                value = cost,
                onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Cost") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val inputMileage = mileage.toIntOrNull() ?: 0
                    val canonicalMileage = UnitConverter.canonicalMilesFromInput(inputMileage, unitSystem)
                    onSave(
                        MaintenanceRecord(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            date = date,
                            mileage = canonicalMileage,
                            description = description,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            category = category,
                            taskName = taskName,
                        )
                    )
                },
                enabled = description.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MaintenanceTimelineScreenPreview() {
    GarageTheme {
        MaintenanceTimelineContent(
            records = SampleData.tacomaMaintenanceRecords,
            sortOrder = MaintenanceSortOrder.DATE,
            unitSystem = UnitSystem.IMPERIAL,
            onSortOrderChanged = {},
            onAddClicked = {},
            onDeleteRecord = {},
        )
    }
}
