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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                SettingsCategoryHeader(stringResource(R.string.backup_header_local))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_export)) },
                    supportingContent = { Text(stringResource(R.string.backup_export_summary)) },
                    leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isBusy) { exportLauncher.launch("garage-backup.zip") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_import)) },
                    supportingContent = { Text(stringResource(R.string.backup_import_summary)) },
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
                        headlineContent = { Text(stringResource(R.string.backup_local_enable)) },
                        supportingContent = { Text(stringResource(R.string.backup_local_enable_summary)) },
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
                        headlineContent = { Text(stringResource(R.string.backup_local_now)) },
                        supportingContent = { Text(stringResource(R.string.backup_local_now_summary)) },
                        leadingContent = { Icon(Icons.Filled.FolderZip, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isBusy) { viewModel.triggerLocalBackupNow() },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader(stringResource(R.string.backup_header_cloud))
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
                        headlineContent = { Text(stringResource(R.string.backup_webdav_enable)) },
                        supportingContent = { Text(stringResource(R.string.backup_webdav_enable_summary)) },
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
                        headlineContent = { Text(stringResource(R.string.backup_webdav_now)) },
                        supportingContent = { Text(stringResource(R.string.backup_webdav_now_summary)) },
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
                SettingsCategoryHeader(stringResource(R.string.backup_header_integrations))
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
                        headlineContent = { Text(stringResource(R.string.backup_ll_enable)) },
                        supportingContent = { Text(stringResource(R.string.backup_ll_enable_summary)) },
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
                        headlineContent = { Text(stringResource(if (syncing) R.string.backup_ll_syncing else R.string.backup_ll_now)) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (syncing) R.string.backup_ll_syncing_summary else R.string.backup_ll_now_summary
                                ),
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
                        headlineContent = { Text(stringResource(R.string.backup_duplicates)) },
                        supportingContent = { Text(stringResource(R.string.backup_duplicates_summary)) },
                        leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDuplicates() },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader(stringResource(R.string.backup_header_danger))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_clear)) },
                    supportingContent = { Text(stringResource(R.string.backup_clear_summary)) },
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
            title = { Text(stringResource(R.string.backup_clear_title)) },
            text = { Text(stringResource(R.string.backup_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text(stringResource(R.string.backup_clear_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val importFailedRestartMessage = uiState.importFailedRestartMessage
    if (uiState.importSucceeded || (importFailedRestartMessage != null)) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.backup_restart_title)) },
            text = { Text(importFailedRestartMessage ?: stringResource(R.string.backup_restart_message)) },
            confirmButton = {
                TextButton(onClick = { AppRestarter.restart(context) }) {
                    Text(stringResource(R.string.backup_restart_now))
                }
            },
        )
    }
}
