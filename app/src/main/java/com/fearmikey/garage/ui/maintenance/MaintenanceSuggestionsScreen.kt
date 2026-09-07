package com.fearmikey.garage.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.StatusChip
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.toDisplayMileage

@Composable
fun MaintenanceSuggestionsScreen(
    viewModel: MaintenanceSuggestionsViewModel = hiltViewModel(),
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    var logSheetSuggestion by remember { mutableStateOf<MaintenanceSuggestion?>(null) }

    MaintenanceSuggestionsContent(
        suggestions = suggestions,
        onAddReminder = viewModel::addAsReminder,
        onLogNow = { logSheetSuggestion = it },
    )

    logSheetSuggestion?.let { suggestion ->
        AddEditMaintenanceRecordSheet(
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
}

@Composable
private fun MaintenanceSuggestionsContent(
    suggestions: List<MaintenanceSuggestion>,
    onAddReminder: (MaintenanceSuggestion) -> Unit,
    onLogNow: (MaintenanceSuggestion) -> Unit,
) {
    Scaffold { innerPadding ->
        if (suggestions.isEmpty()) {
            EmptyState(
                message = "No suggestions yet.\nLog some maintenance and mileage to see what's due.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(suggestions, key = { it.rule.taskName }) { suggestion ->
                    MaintenanceSuggestionRow(
                        suggestion = suggestion,
                        onAddReminder = { onAddReminder(suggestion) },
                        onLogNow = { onLogNow(suggestion) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceSuggestionRow(
    suggestion: MaintenanceSuggestion,
    onAddReminder: () -> Unit,
    onLogNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                suggestion.rule.category.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(suggestion.rule.taskName, style = MaterialTheme.typography.titleMedium)

            val details = listOfNotNull(
                suggestion.lastServiceMileage?.let { "last done at ${it.toDisplayMileage()} mi" },
                "due at ${suggestion.nextDueMileage.toDisplayMileage()} mi",
            ).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            suggestion.rule.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MaintenanceSuggestionsScreenPreview() {
    GarageTheme {
        MaintenanceSuggestionsContent(
            suggestions = SampleData.tacomaMaintenanceSuggestions,
            onAddReminder = {},
            onLogNow = {},
        )
    }
}
