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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenStartup: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDefaultVehicleDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBugReportFeedbackDialog by remember { mutableStateOf(false) }
    var showObdAdapterDialog by remember { mutableStateOf(false) }

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
            // General Preferences
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_user_preferences))
            }
            item {
                val currentLocales = remember(showLanguageDialog) { context.getSystemService(android.app.LocaleManager::class.java).applicationLocales }
                val currentLang = if (currentLocales.isEmpty) "" else currentLocales.get(0)?.language ?: ""
                val languageName = when (currentLang) {
                    "en" -> stringResource(R.string.language_english)
                    "fr" -> stringResource(R.string.language_french)
                    "de" -> stringResource(R.string.language_german)
                    "pl" -> stringResource(R.string.language_polish)
                    "es" -> stringResource(R.string.language_spanish)
                    "it" -> stringResource(R.string.language_italian)
                    "pt" -> stringResource(R.string.language_portuguese)
                    "nl" -> stringResource(R.string.language_dutch)
                    "ru" -> stringResource(R.string.language_russian)
                    "tr" -> stringResource(R.string.language_turkish)
                    else -> stringResource(R.string.language_system_default)
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_selection)) },
                    supportingContent = { Text(languageName) },
                    leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_units)) },
                    supportingContent = { Text(uiState.units.replaceFirstChar { it.uppercase() }) },
                    leadingContent = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUnitsDialog = true },
                )
            }
            item {
                val selectedCurrencyObj = AppCurrency.fromCode(uiState.currency)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_currency)) },
                    supportingContent = { Text(selectedCurrencyObj.displayName) },
                    leadingContent = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true },
                )
            }
            item {
                val selectedVehicleName = uiState.vehicles.find { it.id == uiState.defaultVehicleId }?.let { vehicle ->
                    listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }
                } ?: "First vehicle added (default)"

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_vehicle)) },
                    supportingContent = { Text("Used for widget shortcuts ($selectedVehicleName)") },
                    leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDefaultVehicleDialog = true },
                )
            }
            item {
                val adapterText = uiState.savedObdAdapter?.let { adapter ->
                    stringResource(
                        R.string.settings_obd_adapter_summary,
                        adapter.name,
                        adapter.type.displayName,
                        adapter.address,
                    )
                } ?: stringResource(R.string.settings_obd_adapter_not_set)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_obd_adapter_title)) },
                    supportingContent = { Text(adapterText) },
                    leadingContent = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (hasObdBluetoothPermissions(context)) viewModel.loadPairedObdDevices()
                            showObdAdapterDialog = true
                        },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Appearance & Customization
            item {
                SettingsCategoryHeader("Appearance & Customization")
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_appearance_theme)) },
                    supportingContent = { Text("App theme, Pure Black AMOLED, Dynamic Color, accent colors, and vehicle tinting") },
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
                SettingsCategoryHeader("Reminders & Notifications")
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_reminders_notifications)) },
                    supportingContent = { Text("Document expirations, maintenance notice windows, and notification permissions") },
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
                SettingsCategoryHeader("Data & Synchronization")
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_data_sync)) },
                    supportingContent = { Text("Export/import backups, local auto-backup, WebDAV cloud sync, LubeLogger, clear data") },
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
                    headlineContent = { Text("License") },
                    supportingContent = { Text("MIT License") },
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
                    headlineContent = { Text("Report a Bug / Feedback") },
                    supportingContent = { Text("Report issues or share feedback on GitHub.") },
                    leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBugReportFeedbackDialog = true },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Support & Legal Section
            item {
                SettingsCategoryHeader("Support & Legal")
            }
            item {
                ListItem(
                    headlineContent = { Text("Buy Me a Coffee") },
                    supportingContent = { Text("Support development: buymeacoffee.com/XimW7nXI1j") },
                    leadingContent = { Icon(Icons.Filled.LocalCafe, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://buymeacoffee.com/XimW7nXI1j") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Enable Affiliate Links") },
                    supportingContent = { Text("Show optional Amazon affiliate links for tools/parts to help support the project.") },
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
                    headlineContent = { Text("Help / FAQ") },
                    supportingContent = { Text("Get help with using the app and common questions.") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/fearmikey/Garage/discussions") },
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
        }
    }

    // Dialogs
    if (showLanguageDialog) {
        val currentLocales = remember { context.getSystemService(android.app.LocaleManager::class.java).applicationLocales }
        val currentLang = if (currentLocales.isEmpty) "" else currentLocales.get(0)?.language ?: ""

        LanguageSelectionDialog(
            selectedLangCode = currentLang,
            onLanguageSelected = { langCode ->
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                localeManager.applicationLocales = LocaleListCompat.forLanguageTags(langCode).unwrap() as android.os.LocaleList
                showLanguageDialog = false
            },
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    if (showUnitsDialog) {
        SingleChoiceDialog(
            title = "Units of Measurement",
            options = listOf("metric", "imperial"),
            selectedOption = uiState.units,
            onOptionSelected = { viewModel.setUnits(it) },
            onDismissRequest = { showUnitsDialog = false }
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            selectedCurrencyCode = uiState.currency,
            onCurrencySelected = { viewModel.setCurrency(it) },
            onDismissRequest = { showCurrencyDialog = false },
        )
    }

    if (showDefaultVehicleDialog) {
        DefaultVehicleDialog(
            vehicles = uiState.vehicles,
            selectedVehicleId = uiState.defaultVehicleId,
            onVehicleSelected = { vehicleId ->
                viewModel.setDefaultVehicleId(vehicleId)
                showDefaultVehicleDialog = false
            },
            onDismissRequest = { showDefaultVehicleDialog = false },
        )
    }

    if (showObdAdapterDialog) {
        ObdDevicePickerDialog(
            pairedDevices = uiState.pairedObdDevices,
            bleDevices = uiState.bleObdDevices,
            isBleScanning = uiState.isObdBleScanning,
            selected = uiState.savedObdAdapter,
            onAdapterSelected = { config ->
                viewModel.setSavedObdAdapter(config)
                showObdAdapterDialog = false
            },
            onDismissRequest = {
                viewModel.stopObdBleScan()
                showObdAdapterDialog = false
            },
            onRefreshPaired = viewModel::loadPairedObdDevices,
            onStartBleScan = viewModel::startObdBleScan,
            onClearSelection = if (uiState.savedObdAdapter != null) {
                {
                    viewModel.clearSavedObdAdapter()
                    showObdAdapterDialog = false
                }
            } else {
                null
            },
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
                    Text(text = "Licensed under the MIT License", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
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

    if (showBugReportFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showBugReportFeedbackDialog = false },
            title = { Text("Report a Bug / Feedback") },
            text = {
                Column {
                    Text("Select an option below to open GitHub in your browser:")
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
                        Text("Report an Issue")
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
                        Text("Share Feedback")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBugReportFeedbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
