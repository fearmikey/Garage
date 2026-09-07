package com.fearmikey.garage.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
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
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.StatusChip
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.toDisplayDate

@Composable
fun MaintenanceSuggestionsScreen(
    viewModel: MaintenanceSuggestionsViewModel = hiltViewModel(),
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val customRules by viewModel.customRules.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    var logSheetSuggestion by remember { mutableStateOf<MaintenanceSuggestion?>(null) }
    var showAddRuleSheet by remember { mutableStateOf(value = false) }

    MaintenanceSuggestionsContent(
        suggestions = suggestions,
        customRules = customRules,
        unitSystem = unitSystem,
        onAddReminder = viewModel::addAsReminder,
        onLogNow = { logSheetSuggestion = it },
        onAddCustomRule = { showAddRuleSheet = true },
        onDeleteCustomRule = viewModel::deleteCustomRule,
    )

    logSheetSuggestion?.let { suggestion ->
        AddEditMaintenanceRecordSheet(
            unitSystem = unitSystem,
            onDismiss = { logSheetSuggestion = null },
            onSave = { record ->
                viewModel.logMaintenance(record)
                logSheetSuggestion = null
            },
            // Pre-fills category + task name so this record is tracked as fulfilling this
            // exact suggestion, not just "something in this category".
            initial = MaintenanceRecord(
                vehicleId = 0,
                date = System.currentTimeMillis(),
                mileage = 0,
                description = suggestion.rule.taskName,
                cost = 0.0,
                category = suggestion.rule.category,
                taskName = suggestion.rule.taskName,
            ),
        )
    }

    if (showAddRuleSheet) {
        AddCustomMaintenanceRuleSheet(
            unitSystem = unitSystem,
            onDismiss = { showAddRuleSheet = false },
        ) { rule ->
            viewModel.saveCustomRule(rule)
            showAddRuleSheet = false
        }
    }
}

@Composable
private fun MaintenanceSuggestionsContent(
    suggestions: List<MaintenanceSuggestion>,
    customRules: List<CustomMaintenanceRule>,
    unitSystem: UnitSystem,
    onAddReminder: (MaintenanceSuggestion) -> Unit,
    onLogNow: (MaintenanceSuggestion) -> Unit,
    onAddCustomRule: () -> Unit,
    onDeleteCustomRule: (CustomMaintenanceRule) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCustomRule) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom rule")
            }
        },
    ) { innerPadding ->
        if (suggestions.isEmpty() && customRules.isEmpty()) {
            EmptyState(
                message = "No suggestions yet.\nLog some maintenance and mileage to see what's due.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (customRules.isNotEmpty()) {
                    item(key = "custom_rules_header") {
                        Text(
                            "Your custom rules",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(customRules, key = { "custom_${it.id}" }) { rule ->
                        CustomMaintenanceRuleRow(
                            rule = rule,
                            unitSystem = unitSystem,
                            onDelete = { onDeleteCustomRule(rule) },
                        )
                    }
                }
                items(suggestions, key = { it.rule.taskName }) { suggestion ->
                    MaintenanceSuggestionRow(
                        suggestion = suggestion,
                        unitSystem = unitSystem,
                        onAddReminder = { onAddReminder(suggestion) },
                        onLogNow = { onLogNow(suggestion) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomMaintenanceRuleRow(
    rule: CustomMaintenanceRule,
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
                Text(rule.category.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(rule.taskName, style = MaterialTheme.typography.titleMedium)

                val interval = listOfNotNull(
                    rule.intervalMiles?.let { UnitConverter.formatDistance(it, unitSystem) },
                    rule.intervalMonths?.let { "$it mo" },
                ).joinToString(" / ")
                Text("Every $interval", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                rule.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete custom rule")
            }
        }
    }
}

@Composable
private fun MaintenanceSuggestionRow(
    suggestion: MaintenanceSuggestion,
    unitSystem: UnitSystem,
    onAddReminder: () -> Unit,
    onLogNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                suggestion.rule.category.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(suggestion.rule.taskName, style = MaterialTheme.typography.titleMedium)

            val details = listOfNotNull(
                suggestion.lastServiceMileage?.let { "last done at ${UnitConverter.formatDistance(it, unitSystem)}" },
                suggestion.nextDueMileage?.let { "due at ${UnitConverter.formatDistance(it, unitSystem)}" },
                suggestion.nextDueDate?.let { "due by ${it.toDisplayDate()}" },
            ).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            suggestion.rule.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(status = suggestion.status)
                Row {
                    TextButton(onClick = onAddReminder) {
                        Text("Add reminder")
                    }
                    TextButton(onClick = onLogNow) {
                        Text("Log now")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomMaintenanceRuleSheet(
    onDismiss: () -> Unit,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    onSave: (CustomMaintenanceRule) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taskName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MaintenanceCategory.OTHER) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var intervalMiles by remember { mutableStateOf("") }
    var intervalMonths by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val canSave = taskName.isNotBlank() && (intervalMiles.isNotBlank() || intervalMonths.isNotBlank())

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
            Text("Add Custom Rule", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

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
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = intervalMiles,
                onValueChange = { intervalMiles = it.filter(Char::isDigit) },
                label = { Text("Interval (${unitSystem.distanceUnit})") },
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
                value = intervalMonths,
                onValueChange = { intervalMonths = it.filter(Char::isDigit) },
                label = { Text("Interval (months)") },
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
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val inputMiles = intervalMiles.toIntOrNull()
                    val canonicalIntervalMiles = inputMiles?.let { UnitConverter.canonicalMilesFromInput(it, unitSystem) }
                    onSave(
                        CustomMaintenanceRule(
                            vehicleId = 0,
                            taskName = taskName,
                            category = category,
                            intervalMiles = canonicalIntervalMiles,
                            intervalMonths = intervalMonths.toIntOrNull(),
                            notes = notes.ifBlank { null },
                        ),
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
private fun MaintenanceSuggestionsScreenPreview() {
    GarageTheme {
        MaintenanceSuggestionsContent(
            suggestions = SampleData.tacomaMaintenanceSuggestions,
            customRules = SampleData.tacomaCustomRules,
            unitSystem = UnitSystem.IMPERIAL,
            onAddReminder = {},
            onLogNow = {},
            onAddCustomRule = {},
            onDeleteCustomRule = {},
        )
    }
}
