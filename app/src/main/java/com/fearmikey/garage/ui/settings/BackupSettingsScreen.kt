package com.fearmikey.garage.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerSyncStatus
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.util.AppRestarter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    onOpenDuplicates: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showLubeLoggerDialog by remember { mutableStateOf(false) }
    var showLocalBackupDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::exportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }

    val localBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setLocalBackupFolderUri) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_data_sync)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScrollbar(listState),
        ) {
            item {
                SettingsCategoryHeader("Local Backups")
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
                LocalBackupStatusItem(
                    uiState = uiState,
                    onClick = { showLocalBackupDialog = true },
                )
            }
            if (uiState.localBackupFolderUri.isNotBlank()) {
                item {
                    ListItem(
                        headlineContent = { Text("Enable Local Auto-Backup") },
                        supportingContent = { Text("Automatically save backups to your selected local folder.") },
                        trailingContent = {
                            Switch(
                                checked = uiState.localBackupEnabled,
                                onCheckedChange = { viewModel.setLocalBackupEnabled(it) },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLocalBackupEnabled(!uiState.localBackupEnabled) },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Backup to folder now") },
                        supportingContent = { Text("Manually save a backup zip to your local folder.") },
                        leadingContent = { Icon(Icons.Filled.FolderZip, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isBusy) { viewModel.triggerLocalBackupNow() },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader("Cloud Synchronization")
            }
            item {
                WebDavStatusItem(
                    uiState = uiState,
                    onClick = { showCloudBackupDialog = true },
                )
            }
            val webDavConfigured = uiState.webdavUrl.isNotBlank() && uiState.webdavUsername.isNotBlank()
            if (webDavConfigured) {
                item {
                    ListItem(
                        headlineContent = { Text("Enable WebDAV Cloud Sync") },
                        supportingContent = { Text("Automatically upload backups to your WebDAV server.") },
                        trailingContent = {
                            Switch(
                                checked = uiState.cloudSyncEnabled,
                                onCheckedChange = { viewModel.setCloudSyncEnabled(it) },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setCloudSyncEnabled(!uiState.cloudSyncEnabled) },
                    )
                }
                item {
                    val cloudSyncReady = uiState.cloudSyncEnabled && uiState.webdavPasswordSet
                    ListItem(
                        headlineContent = { Text("Sync WebDAV now") },
                        supportingContent = { Text("Manually upload a backup to your WebDAV server.") },
                        leadingContent = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = cloudSyncReady && !uiState.isBusy && !uiState.isSyncing) {
                                viewModel.syncNow()
                            },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader("Integrations")
            }
            item {
                LubeLoggerStatusItem(
                    configured = uiState.lubeLoggerConfigured,
                    serverUrl = uiState.lubeLoggerServerUrl,
                    status = uiState.lubeLoggerStatus,
                    syncEnabled = uiState.lubeLoggerSyncEnabled,
                    onClick = { showLubeLoggerDialog = true },
                )
            }
            if (uiState.lubeLoggerConfigured) {
                item {
                    ListItem(
                        headlineContent = { Text("Enable LubeLogger Sync") },
                        supportingContent = { Text("Automatically exchange records with your LubeLogger server.") },
                        trailingContent = {
                            Switch(
                                checked = uiState.lubeLoggerSyncEnabled,
                                onCheckedChange = { viewModel.setLubeLoggerSyncEnabled(it) },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLubeLoggerSyncEnabled(!uiState.lubeLoggerSyncEnabled) },
                    )
                }
                item {
                    val syncing = uiState.lubeLoggerStatus is LubeLoggerSyncStatus.Syncing
                    ListItem(
                        headlineContent = { Text(if (syncing) "Syncing with LubeLogger…" else "Sync LubeLogger now") },
                        supportingContent = {
                            Text(
                                if (syncing) "Exchanging records with your LubeLogger server."
                                else "Manually trigger a sync with your LubeLogger server.",
                            )
                        },
                        leadingContent = {
                            if (syncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.CloudSync, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !syncing) { viewModel.syncLubeLoggerNow() },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Find duplicate records") },
                        supportingContent = { Text("Review and remove identical fuel or maintenance records.") },
                        leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDuplicates() },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader("Danger Zone")
            }
            item {
                ListItem(
                    headlineContent = { Text("Clear All Data") },
                    supportingContent = { Text("Permanently delete all vehicles, records, reminders, and photos.") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isBusy) { showClearDataDialog = true },
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

    if (showLubeLoggerDialog) {
        LubeLoggerConfigDialog(
            initialUrl = uiState.lubeLoggerServerUrl,
            initialUsername = uiState.lubeLoggerUsername,
            initialApiKey = uiState.lubeLoggerApiKey,
            initialUnitSystem = uiState.lubeLoggerUnitSystem,
            onDismissRequest = { showLubeLoggerDialog = false },
            onSave = { url, username, password, apiKey, unitSystem ->
                viewModel.setLubeLoggerCredentials(url, username, password, apiKey, unitSystem)
                showLubeLoggerDialog = false
            }
        )
    }

    if (showCloudBackupDialog) {
        CloudBackupConfigDialog(
            uiState = uiState,
            onSave = { url, username, password ->
                viewModel.setCloudSyncEnabled(true)
                viewModel.setWebdavCredentials(url, username, password)
                showCloudBackupDialog = false
            },
            onTestConnection = { url, username, password -> viewModel.testConnection(url, username, password) },
            onDismissRequest = { showCloudBackupDialog = false },
        )
    }

    if (showLocalBackupDialog) {
        LocalBackupConfigDialog(
            uiState = uiState,
            onSelectFolder = { localBackupFolderLauncher.launch(null) },
            onDismissRequest = { showLocalBackupDialog = false },
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
}
