package com.fearmikey.garage.ui.settings

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.BuildConfig
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.AppRestarter
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStartup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnitsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::exportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            // Preferences Section
            item {
                SettingsCategoryHeader("User Preferences")
            }
            item {
                ListItem(
                    headlineContent = { Text("Units of Measurement") },
                    supportingContent = { Text(uiState.units.replaceFirstChar { it.uppercase() }) },
                    leadingContent = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUnitsDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(uiState.theme.replaceFirstChar { it.uppercase() }) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Data & Backup Section
            item {
                SettingsCategoryHeader("Data & Privacy")
            }
            item {
                ListItem(
                    headlineContent = { Text("Export backup") },
                    supportingContent = { Text("Save all vehicles, maintenance history, reminders and photos to a zip file.") },
                    leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isBusy) { exportLauncher.launch("garage-backup.zip") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Import backup") },
                    supportingContent = { Text("Restore vehicles, maintenance history, reminders and photos from a backup zip. This replaces all current data.") },
                    leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isBusy) { importLauncher.launch(arrayOf("application/zip")) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Auto-Backup Configuration") },
                    supportingContent = { Text(cloudBackupStatusText(uiState)) },
                    leadingContent = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCloudBackupDialog = true },
                )
            }
            item {
                val cloudSyncReady = uiState.cloudSyncEnabled &&
                    uiState.webdavUrl.isNotBlank() &&
                    uiState.webdavUsername.isNotBlank() &&
                    uiState.webdavPasswordSet
                ListItem(
                    headlineContent = { Text("Sync now") },
                    supportingContent = { Text("Manually upload a backup to your WebDAV server.") },
                    leadingContent = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = cloudSyncReady && !uiState.isBusy && !uiState.isSyncing) {
                            viewModel.syncNow()
                        },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Clear All Data") },
                    supportingContent = { Text("Permanently delete all vehicles, records, reminders, and photos.") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isBusy) { showClearDataDialog = true },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // App Information Section
            item {
                SettingsCategoryHeader("App Information")
            }
            item {
                ListItem(
                    headlineContent = { Text("About Garage") },
                    supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Open Source Licenses") },
                    supportingContent = { Text("Third-party software notices and licenses.") },
                    leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, OssLicensesMenuActivity::class.java)
                            context.startActivity(intent)
                        },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Report a Bug / Feedback") },
                    supportingContent = { Text("Send feedback or bug reports to the developer.") },
                    leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@garageapp.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Garage App Feedback (v${BuildConfig.VERSION_NAME})")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                context.startActivity(Intent.createChooser(intent, "Send Email"))
                            }
                        },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Support & Onboarding Section
            item {
                SettingsCategoryHeader("Support & Legal")
            }
            item {
                ListItem(
                    headlineContent = { Text("Help / FAQ") },
                    supportingContent = { Text("Get help with using the app and common questions.") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fearmikey/Garage") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    leadingContent = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fearmikey/Garage") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Terms of Service") },
                    leadingContent = { Icon(Icons.Filled.Gavel, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fearmikey/Garage/blob/main/TERMS.md") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Re-run Setup") },
                    supportingContent = { Text("Re-configure app permissions and measurement units") },
                    leadingContent = { Icon(Icons.Filled.School, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStartup() },
                )
            }

            if (uiState.isBusy) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Dialogs
    if (showUnitsDialog) {
        SingleChoiceDialog(
            title = "Units of Measurement",
            options = listOf("metric", "imperial"),
            selectedOption = uiState.units,
            onOptionSelected = { viewModel.setUnits(it) },
            onDismissRequest = { showUnitsDialog = false }
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Theme",
            options = listOf("light", "dark", "system"),
            selectedOption = uiState.theme,
            onOptionSelected = { viewModel.setTheme(it) },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all vehicles, maintenance records, reminders, and photos stored in Garage. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Garage") },
            text = {
                Column {
                    Text(text = "Garage", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = "A simple, powerful vehicle management app to track maintenance, service history, fuel economy, and reminders.")
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = "Developed by fearmikey", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (uiState.importSucceeded) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restart required") },
            text = { Text("Your data has been restored. Garage needs to restart to load it.") },
            confirmButton = {
                TextButton(onClick = { AppRestarter.restart(context) }) {
                    Text("Restart now")
                }
            },
        )
    }

    if (showCloudBackupDialog) {
        CloudBackupConfigDialog(
            uiState = uiState,
            onSave = { enabled, url, username, password ->
                viewModel.setCloudSyncEnabled(enabled)
                viewModel.setWebdavCredentials(url, username, password)
                showCloudBackupDialog = false
            },
            onTestConnection = { url, username, password -> viewModel.testConnection(url, username, password) },
            onDismissRequest = { showCloudBackupDialog = false },
        )
    }
}

private fun cloudBackupStatusText(uiState: SettingsUiState): String {
    if (!uiState.cloudSyncEnabled) return "Disabled"
    val error = uiState.lastSyncError
    if (error != null) return "Enabled — sync failed: $error"
    val lastSync = uiState.lastSyncTimestamp
    return if (lastSync != null) {
        val relative = DateUtils.getRelativeTimeSpanString(lastSync, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        "Enabled — last synced $relative"
    } else {
        "Enabled — not yet synced"
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = option.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CloudBackupConfigDialog(
    uiState: SettingsUiState,
    onSave: (enabled: Boolean, url: String, username: String, password: String) -> Unit,
    onTestConnection: (url: String, username: String, password: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var enabled by remember { mutableStateOf(uiState.cloudSyncEnabled) }
    var url by remember { mutableStateOf(uiState.webdavUrl) }
    var username by remember { mutableStateOf(uiState.webdavUsername) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Auto-Backup Configuration") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable cloud sync", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (uiState.webdavPasswordSet) "Password (unchanged)" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                TextButton(
                    onClick = { onTestConnection(url, username, password) },
                    enabled = url.isNotBlank() && username.isNotBlank(),
                ) {
                    Text("Test connection")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(enabled, url, username, password) },
                enabled = url.isNotBlank() && username.isNotBlank(),
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    GarageTheme {
        // The interactive screen requires a real Activity for its document
        // picker launchers, so this preview just verifies the theme compiles.
    }
}
