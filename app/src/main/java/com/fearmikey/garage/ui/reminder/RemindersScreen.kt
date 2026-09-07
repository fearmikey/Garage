package com.fearmikey.garage.ui.reminder

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.StatusChip
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toDisplayMileage
import java.util.concurrent.TimeUnit

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    RemindersContent(
        reminders = reminders,
        unitSystem = unitSystem,
        onAddClicked = { showAddSheet = true },
        onToggleCompleted = { reminder, completed -> viewModel.setCompleted(reminder, completed) },
        onDelete = viewModel::deleteReminder,
    )

    if (showAddSheet) {
        AddEditReminderSheet(
            unitSystem = unitSystem,
            onDismiss = { showAddSheet = false },
            onSave = { reminder ->
                viewModel.saveReminder(reminder)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun RemindersContent(
    reminders: List<ReminderListItem>,
    unitSystem: UnitSystem,
    onAddClicked: () -> Unit,
    onToggleCompleted: (Reminder, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Add reminder")
            }
        },
    ) { innerPadding ->
        if (reminders.isEmpty()) {
            EmptyState(
                message = "No reminders yet.\nTap + to add an upcoming task.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reminders, key = { it.reminder.id }) { item ->
                    ReminderRow(
                        item = item,
                        unitSystem = unitSystem,
                        onToggleCompleted = { completed -> onToggleCompleted(item.reminder, completed) },
                        onDelete = { onDelete(item.reminder) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    item: ReminderListItem,
    unitSystem: UnitSystem,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Checkbox(checked = item.reminder.isCompleted, onCheckedChange = onToggleCompleted)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        item.reminder.taskName,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (item.reminder.isCompleted) TextDecoration.LineThrough else null,
                    )
                    val details = listOfNotNull(
                        item.reminder.dueDate?.let { "by ${it.toDisplayDate()}" },
                        item.reminder.dueMileage?.let { "at ${UnitConverter.formatDistance(it, unitSystem)}" },
                    ).joinToString(" · ")
                    if (details.isNotBlank()) {
                        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusChip(status = item.status, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete reminder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditReminderSheet(
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    initial: Reminder? = null,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var taskName by remember { mutableStateOf(initial?.taskName.orEmpty()) }
    var dueDateDays by remember { mutableStateOf("") }
    var dueMileage by remember {
        mutableStateOf(
            initial?.dueMileage?.let { UnitConverter.displayDistanceValue(it, unitSystem).toString() }.orEmpty()
        )
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
            Text("Add Reminder", style = MaterialTheme.typography.titleLarge)

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
            OutlinedTextField(
                value = dueDateDays,
                onValueChange = { dueDateDays = it.filter(Char::isDigit) },
                label = { Text("Due in how many days? (optional)") },
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
                value = dueMileage,
                onValueChange = { dueMileage = it.filter(Char::isDigit) },
                label = { Text("Due mileage (${unitSystem.distanceUnit}) (optional)") },
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

            Button(
                onClick = {
                    val days = dueDateDays.toLongOrNull()
                    val inputDueMileage = dueMileage.toIntOrNull()
                    val canonicalDueMileage = inputDueMileage?.let { UnitConverter.canonicalMilesFromInput(it, unitSystem) }
                    onSave(
                        Reminder(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            taskName = taskName,
                            dueDate = days?.let { System.currentTimeMillis() + TimeUnit.DAYS.toMillis(it) },
                            dueMileage = canonicalDueMileage,
                            isCompleted = initial?.isCompleted ?: false,
                        )
                    )
                },
                enabled = taskName.isNotBlank() && (dueDateDays.isNotBlank() || dueMileage.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RemindersScreenPreview() {
    GarageTheme {
        RemindersContent(
            reminders = SampleData.tacomaReminders.map {
                ReminderListItem(it, ReminderRepository.computeStatus(it, SampleData.TACOMA_LATEST_MILEAGE))
            },
            unitSystem = UnitSystem.IMPERIAL,
            onAddClicked = {},
            onToggleCompleted = { _, _ -> },
            onDelete = {},
        )
    }
}
