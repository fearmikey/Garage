package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LubeLoggerConfigDialog(
    initialUrl: String,
    initialUsername: String,
    initialApiKey: String = "",
    initialUnitSystem: String = "imperial",
    onDismissRequest: () -> Unit,
    onSave: (url: String, username: String, password: String?, apiKey: String?, unitSystem: String) -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var unitSystem by remember { mutableStateOf(initialUnitSystem) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("LubeLogger Connection") },
        text = {
            Column {
                Text("Connect Garage to your self-hosted LubeLogger instance to automatically sync records.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LubeLogger Unit System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    FilterChip(
                        selected = unitSystem.equals("imperial", ignoreCase = true),
                        onClick = { unitSystem = "imperial" },
                        label = { Text("Imperial (Miles / Gal)") },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterChip(
                        selected = unitSystem.equals("metric", ignoreCase = true),
                        onClick = { unitSystem = "metric" },
                        label = { Text("Metric (KM / Liters)") },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authentication (Provide either an API Key OR a Username/Password)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, username, password.takeIf { it.isNotBlank() }, apiKey.takeIf { it.isNotBlank() }, unitSystem) },
                enabled = url.isNotBlank() && (apiKey.isNotBlank() || username.isNotBlank()),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
