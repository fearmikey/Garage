package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AttachMoney

import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.fearmikey.garage.ui.util.rememberSafeUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.BuildConfig
import com.fearmikey.garage.config.FlavorConfig
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.CurrencySelectionDialog
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.obd.ObdDevicePickerDialog
import com.fearmikey.garage.ui.obd.hasObdBluetoothPermissions
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUserPreferences: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenStartup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = rememberSafeUriHandler()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showBugReportFeedbackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            // General Preferences
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_user_preferences))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_user_preferences)) },
                    supportingContent = { Text(stringResource(R.string.settings_user_prefs_summary)) },
                    leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUserPreferences() },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Appearance & Customization
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_header_appearance))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_appearance_theme)) },
                    supportingContent = { Text(stringResource(R.string.settings_appearance_summary)) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAppearance() },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Reminders & Notifications
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_reminders_notifications))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_reminders_notifications)) },
                    supportingContent = { Text(stringResource(R.string.settings_reminders_summary)) },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReminders() },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Data & Synchronization
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_header_data))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_data_sync)) },
                    supportingContent = { Text(stringResource(R.string.settings_data_summary)) },
                    leadingContent = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBackup() },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // App Information Section
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_header_app_info))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_license)) },
                    supportingContent = { Text(stringResource(R.string.settings_license_mit)) },
                    leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            FlavorConfig.launchOssLicenses(context)
                        },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_report_bug)) },
                    supportingContent = { Text(stringResource(R.string.settings_report_bug_summary)) },
                    leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBugReportFeedbackDialog = true },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Support & Legal Section
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_header_support))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_bmac)) },
                    supportingContent = { Text(stringResource(R.string.settings_bmac_summary, "buymeacoffee.com/XimW7nXI1j")) },
                    leadingContent = { Icon(Icons.Filled.LocalCafe, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://buymeacoffee.com/XimW7nXI1j") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_affiliate_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_affiliate_summary)) },
                    leadingContent = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.affiliateLinksEnabled,
                            onCheckedChange = { viewModel.setAffiliateLinksEnabled(it) }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAffiliateLinksEnabled(!uiState.affiliateLinksEnabled) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_help)) },
                    supportingContent = { Text(stringResource(R.string.settings_help_summary)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fearmikey/Garage/discussions") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rerun_setup)) },
                    supportingContent = { Text(stringResource(R.string.settings_rerun_setup_summary)) },
                    leadingContent = { Icon(Icons.Filled.School, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStartup() },
                )
            }
        }
    }

    // Dialogs


    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_about_title)) },
            text = {
                Column {
                    Text(text = "Garage", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(text = stringResource(R.string.settings_version_full, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = stringResource(R.string.settings_about_description))
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = stringResource(R.string.settings_about_license), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(text = stringResource(R.string.settings_about_developer), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (showBugReportFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showBugReportFeedbackDialog = false },
            title = { Text(stringResource(R.string.settings_report_bug)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_feedback_prompt))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            showBugReportFeedbackDialog = false
                            uriHandler.openUri("https://github.com/fearmikey/Garage/issues")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.settings_report_issue))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showBugReportFeedbackDialog = false
                            uriHandler.openUri("https://github.com/fearmikey/Garage/discussions")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.settings_share_feedback))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBugReportFeedbackDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}


