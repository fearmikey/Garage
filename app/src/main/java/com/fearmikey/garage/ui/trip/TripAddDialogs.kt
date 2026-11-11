package com.fearmikey.garage.ui.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.ui.util.label
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.ui.settings.parseReimbursementRate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripDialog(
    initialTrip: TripRecord? = null,
    defaultRate: Double,
    currencySymbol: String,
    distanceUnit: String,
    onDismiss: () -> Unit,
    onSave: (TripRecord) -> Unit
) {
    var category by remember { mutableStateOf(initialTrip?.category ?: TripCategory.BUSINESS) }
    var distance by remember { mutableStateOf(initialTrip?.calculatedDistance?.toString() ?: "") }
    // New trips start from the user's configured default rate (Settings > Mileage reimbursement rate).
    var rate by remember { mutableStateOf((initialTrip?.reimbursementRate ?: defaultRate).toString()) }
    var notes by remember { mutableStateOf(initialTrip?.notes ?: "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialTrip == null) R.string.trip_log_title else R.string.trip_edit_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = category.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.mods_category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(
                            type = androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        TripCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label()) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = { Text(stringResource(R.string.trip_distance, distanceUnit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.trip_rate, currencySymbol, distanceUnit)) },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.trip_notes)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val dist = distance.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                val trip = TripRecord(
                    id = initialTrip?.id ?: 0,
                    vehicleId = initialTrip?.vehicleId ?: 0, // Assigned by ViewModel
                    startDate = initialTrip?.startDate ?: System.currentTimeMillis(),
                    endDate = initialTrip?.endDate ?: System.currentTimeMillis(),
                    startOdometer = 0.0,
                    endOdometer = 0.0,
                    calculatedDistance = dist,
                    category = category,
                    reimbursementRate = parseReimbursementRate(rate) ?: 0.0,
                    tollExpense = 0.0,
                    parkingExpense = 0.0,
                    notes = notes,
                    isReimbursed = initialTrip?.isReimbursed ?: false
                )
                onSave(trip)
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
