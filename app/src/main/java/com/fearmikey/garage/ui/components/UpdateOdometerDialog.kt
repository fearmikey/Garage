package com.fearmikey.garage.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.sanitizeMileageInput
import com.fearmikey.garage.ui.util.toDisplayDate
import kotlinx.coroutines.flow.Flow

private fun Vehicle.displayName(): String =
    listOfNotNull(year?.toString(), make, model).joinToString(" ").ifBlank { vin }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateOdometerDialog(
    vehicles: List<Vehicle>,
    initialVehicleId: Long?,
    getLatestMileageFlow: (vehicleId: Long) -> Flow<Int?>,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (vehicleId: Long, canonicalMileage: Int, date: Long, notes: String) -> Unit,
) {
    if (vehicles.isEmpty()) return

    var selectedVehicle by remember(initialVehicleId, vehicles) {
        mutableStateOf(vehicles.find { it.id == initialVehicleId } ?: vehicles.first())
    }

    val latestMileageFlow = remember(selectedVehicle.id) { getLatestMileageFlow(selectedVehicle.id) }
    val currentMileage by latestMileageFlow.collectAsStateWithLifecycle(initialValue = null)

    val displayCurrentMileage = currentMileage?.let {
        UnitConverter.displayDistanceValue(it, unitSystem)
    }

    var mileageInput by remember(selectedVehicle.id, displayCurrentMileage) {
        mutableStateOf(displayCurrentMileage?.toString().orEmpty())
    }

    var notesInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val numericInput = mileageInput.filter(Char::isDigit).toIntOrNull()
    val canSave = (numericInput != null) && (numericInput > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_update_odometer_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (vehicles.size > 1) {
                    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = vehicleDropdownExpanded,
                        onExpandedChange = { vehicleDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleDropdownExpanded,
                            onDismissRequest = { vehicleDropdownExpanded = false },
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.displayName()) },
                                    onClick = {
                                        selectedVehicle = vehicle
                                        vehicleDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                currentMileage?.let { miles ->
                    Text(
                        text = "Current: ${UnitConverter.formatDistance(miles, unitSystem)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = mileageInput,
                    onValueChange = { mileageInput = sanitizeMileageInput(it) },
                    label = { Text("New Odometer Reading (${unitSystem.distanceUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = com.fearmikey.garage.ui.util.ThousandsSeparatorVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Weekly odometer log") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Date: ${selectedDate.toDisplayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = mileageInput.filter(Char::isDigit).toIntOrNull() ?: 0
                    val canonicalMileage = UnitConverter.canonicalMilesFromInput(input, unitSystem)
                    onSave(
                        selectedVehicle.id,
                        canonicalMileage,
                        selectedDate,
                        notesInput.ifBlank { "Odometer check-in" },
                    )
                },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
