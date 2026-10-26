package com.fearmikey.garage.ui.knowledge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote

@Composable
fun AddTorqueSpecDialog(
    initialSpec: TorqueSpec? = null,
    onDismiss: () -> Unit,
    onSave: (TorqueSpec) -> Unit
) {
    var component by remember { mutableStateOf(initialSpec?.component ?: "") }
    var torqueNm by remember { mutableStateOf(initialSpec?.torqueNm?.toString() ?: "") }
    var torqueFtLbs by remember { mutableStateOf(initialSpec?.torqueFtLbs?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialSpec?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSpec == null) "Add Torque Spec" else "Edit Torque Spec") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = component,
                    onValueChange = { component = it },
                    label = { Text("Component (e.g. Lug Nuts)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = torqueNm,
                    onValueChange = { torqueNm = it },
                    label = { Text("Torque (Nm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = torqueFtLbs,
                    onValueChange = { torqueFtLbs = it },
                    label = { Text("Torque (ft-lbs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val spec = TorqueSpec(
                    id = initialSpec?.id ?: 0,
                    vehicleId = initialSpec?.vehicleId ?: 0,
                    component = component,
                    torqueNm = torqueNm.toFloatOrNull(),
                    torqueFtLbs = torqueFtLbs.toFloatOrNull(),
                    notes = notes
                )
                onSave(spec)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddVehicleNoteDialog(
    initialNote: VehicleNote? = null,
    onDismiss: () -> Unit,
    onSave: (VehicleNote) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var category by remember { mutableStateOf(initialNote?.category ?: "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNote == null) "Add Repair Note" else "Edit Repair Note") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown supported)") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val note = VehicleNote(
                    id = initialNote?.id ?: 0,
                    vehicleId = initialNote?.vehicleId ?: 0,
                    title = title,
                    content = content,
                    category = category,
                    lastUpdated = System.currentTimeMillis()
                )
                onSave(note)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
