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
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
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
            title = { Text(stringResource(R.string.settings_insecure_title)) },
            text = {
                Text(stringResource(R.string.settings_insecure_lubelogger_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        doSave()
                    }
                ) {
                    Text(stringResource(R.string.common_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarning = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.ll_config_title)) },
        text = {
            Column {
                Text(stringResource(R.string.ll_config_description))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.common_server_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ll_server_units),
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
                        Text(stringResource(R.string.ll_units_imperial))
                    }
                    SegmentedButton(
                        selected = unitSystem.equals("metric", ignoreCase = true),
                        onClick = { unitSystem = "metric" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.ll_units_metric))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ll_auth_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.common_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.common_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.ll_api_key_optional)) },
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
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
