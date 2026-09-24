package com.fearmikey.garage.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.sanitizeMileageInput
import com.fearmikey.garage.ui.util.toDisplayDate

private fun Vehicle.displayName(): String =
    listOfNotNull(year?.toString(), make, model).joinToString(" ").ifBlank { vin }

@Composable
fun UpdateOdometerDialog(
    vehicles: List<Vehicle>,
    initialVehicleId: Long?,
    getLatestMileage: (vehicleId: Long) -> Int?,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (vehicleId: Long, canonicalMileage: Int, date: Long, notes: String) -> Unit,
) {
    if (vehicles.isEmpty()) return

    var selectedVehicle by remember(initialVehicleId, vehicles) {
        mutableStateOf(vehicles.find { it.id == initialVehicleId } ?: vehicles.first())
    }

    val currentMileage = getLatestMileage(selectedVehicle.id)
    val displayCurrentMileage = currentMileage?.let {
        UnitConverter.displayDistanceValue(it, unitSystem)
    }

    var mileageInput by remember(selectedVehicle.id, displayCurrentMileage) {
        mutableStateOf(displayCurrentMileage?.toString().orEmpty())
    }

    var notesInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Vehicle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedVehicle.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select vehicle",
                                modifier = Modifier.clickable { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vehicleDropdownExpanded = !vehicleDropdownExpanded },
                    )
                    DropdownMenu(
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
