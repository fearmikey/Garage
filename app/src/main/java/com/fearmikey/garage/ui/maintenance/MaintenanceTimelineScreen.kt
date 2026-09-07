package com.fearmikey.garage.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toDisplayMileage

@Composable
fun MaintenanceTimelineScreen(
    viewModel: MaintenanceTimelineViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    MaintenanceTimelineContent(
        records = records,
        sortOrder = sortOrder,
        onSortOrderChanged = viewModel::setSortOrder,
        onAddClicked = { showAddSheet = true },
        onDeleteRecord = viewModel::deleteRecord,
    )

    if (showAddSheet) {
        AddEditMaintenanceRecordSheet(
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
    onSortOrderChanged: (MaintenanceSortOrder) -> Unit,
    onAddClicked: () -> Unit,
    onDeleteRecord: (MaintenanceRecord) -> Unit,
) {
    Scaffold(
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(records, key = { it.id }) { record ->
                        MaintenanceRecordRow(
                            record = record,
                            onDelete = { onDeleteRecord(record) },
                            modifier = Modifier.padding(bottom = 12.dp),
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.category.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(record.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.date.toDisplayDate()} · ${record.mileage.toDisplayMileage()} mi · $${"%.2f".format(record.cost)}",
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
) {
    val sheetState = rememberModalBottomSheetState()
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var mileage by remember { mutableStateOf(initial?.mileage?.toString().orEmpty()) }
    var cost by remember { mutableStateOf(initial?.cost?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: MaintenanceCategory.OTHER) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var taskName by remember { mutableStateOf(initial?.taskName) }
    var taskMenuExpanded by remember { mutableStateOf(false) }
    val date = remember { mutableStateOf(initial?.date ?: System.currentTimeMillis()) }

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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Maintenance Record", style = MaterialTheme.typography.titleLarge)

            Text(
                "Date: ${date.value.toDisplayDate()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                DropdownMenu(
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
                        label = { Text("Specific task") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    DropdownMenu(
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
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it.filter(Char::isDigit) },
                label = { Text("Mileage") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Cost") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        MaintenanceRecord(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            date = date.value,
                            mileage = mileage.toIntOrNull() ?: 0,
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
            onSortOrderChanged = {},
            onAddClicked = {},
            onDeleteRecord = {},
        )
    }
}
