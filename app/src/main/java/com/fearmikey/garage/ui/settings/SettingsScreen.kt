package com.fearmikey.garage.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.AppRestarter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("Export backup") },
                supportingContent = { Text("Save all vehicles, maintenance history, reminders and photos to a zip file.") },
                leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isBusy) { exportLauncher.launch("garage-backup.zip") },
            )
            ListItem(
                headlineContent = { Text("Import backup") },
                supportingContent = { Text("Restore vehicles, maintenance history, reminders and photos from a backup zip. This replaces all current data.") },
                leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isBusy) { importLauncher.launch(arrayOf("application/zip")) },
            )

            if (uiState.isBusy) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator()
                }
            }
        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    GarageTheme {
        // The interactive screen requires a real Activity for its document
        // picker launchers, so this preview just verifies the theme compiles.
    }
}
