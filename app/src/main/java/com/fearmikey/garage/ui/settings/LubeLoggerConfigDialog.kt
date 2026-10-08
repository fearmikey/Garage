package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showHttpWarning by remember { mutableStateOf(false) }

    fun doSave() {
        onSave(url, username, password.takeIf { it.isNotBlank() }, apiKey.takeIf { it.isNotBlank() }, unitSystem)
    }

    fun handleSaveClick() {
        val isHttp = !url.trim().startsWith("https://", ignoreCase = true)
        if (isHttp) {
            showHttpWarning = true
        } else {
            doSave()
        }
    }

    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false },
            title = { Text("Insecure Connection Warning") },
            text = {
                Text("You are connecting over an unencrypted HTTP connection. To keep your login credentials and vehicle data secure, ensure you are using a secure private VPN connection (such as Tailscale or WireGuard) to your server.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        doSave()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "LubeLogger Server Units",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = unitSystem.equals("imperial", ignoreCase = true),
                        onClick = { unitSystem = "imperial" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Imperial (Mi/Gal)")
                    }
                    SegmentedButton(
                        selected = unitSystem.equals("metric", ignoreCase = true),
                        onClick = { unitSystem = "metric" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Metric (KM/L)")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Authentication (Provide either Username/Password OR an API Key)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::handleSaveClick,
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
