@file:Suppress("DEPRECATION")

package com.fearmikey.garage.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerSyncStatus
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingUp
import com.fearmikey.garage.ui.components.CurrencySelectionDialog
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import androidx.documentfile.provider.DocumentFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.BuildConfig
import com.fearmikey.garage.R
import com.fearmikey.garage.config.FlavorConfig
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.obd.ObdDevicePickerDialog
import com.fearmikey.garage.ui.obd.hasObdBluetoothPermissions
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.AppRestarter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStartup: () -> Unit = {},
    onOpenDuplicates: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnitsDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDefaultVehicleDialog by remember { mutableStateOf(false) }
    var showMaintenanceMileageDialog by remember { mutableStateOf(false) }
    var showMaintenanceDaysDialog by remember { mutableStateOf(false) }
    var showDocumentDaysDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showLubeLoggerDialog by remember { mutableStateOf(false) }
    var showLocalBackupDialog by remember { mutableStateOf(false) }
    var showBugReportFeedbackDialog by remember { mutableStateOf(false) }
    var showObdAdapterDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::exportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }

    val localBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setLocalBackupFolderUri) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshNotificationPermissionState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScrollbar(listState),
        ) {
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
                val selectedCurrencyObj = AppCurrency.fromCode(uiState.currency)
                ListItem(
                    headlineContent = { Text("Currency") },
                    supportingContent = { Text(selectedCurrencyObj.displayName) },
                    leadingContent = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true },
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
            item {
                val selectedVehicleName = uiState.vehicles.find { it.id == uiState.defaultVehicleId }?.let { vehicle ->
                    listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }
                } ?: "First vehicle added (default)"

                ListItem(
                    headlineContent = { Text("Default Vehicle") },
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

            // Reminders & Expirations Section
            item {
                SettingsCategoryHeader("Reminders & Expirations")
            }
            item {
                ListItem(
                    headlineContent = { Text("Document Renewal Reminders") },
                    supportingContent = { Text("Notify for expiring vehicle registrations, smog tests, inspections, insurance, permits, and driver's licenses.") },
                    leadingContent = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.documentExpirationRemindersEnabled,
                            onCheckedChange = { viewModel.setDocumentExpirationRemindersEnabled(it) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDocumentExpirationRemindersEnabled(!uiState.documentExpirationRemindersEnabled) },
                )
            }
            if (uiState.documentExpirationRemindersEnabled) {
                item {
                    val daysText = if (uiState.documentExpirationDaysWindow == 1) "1 day" else "${uiState.documentExpirationDaysWindow} days"
                    ListItem(
                        headlineContent = { Text("Document Expiration Notice Window") },
                        supportingContent = { Text("Notify when document expiration is due within $daysText") },
                        leadingContent = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDocumentDaysDialog = true },
                    )
                }
            }
            item {
                val unitSystem = UnitSystem.fromString(uiState.units)
                val distanceText = UnitConverter.formatDistance(uiState.maintenanceMileageWindow, unitSystem)
                ListItem(
                    headlineContent = { Text("Maintenance Mileage Notice Window") },
                    supportingContent = { Text("Notify when service is due within $distanceText") },
                    leadingContent = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMaintenanceMileageDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Show Fuel Trend Graph") },
                    supportingContent = { Text("Display the fuel trend sparkline graph on vehicle cards") },
                    leadingContent = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFuelTrendGraph,
                            onCheckedChange = { viewModel.setShowFuelTrendGraph(it) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowFuelTrendGraph(!uiState.showFuelTrendGraph) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Show Fleet Overview") },
                    supportingContent = { Text("Display the summary card with fleet averages on the dashboard") },
                    leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFleetOverview,
                            onCheckedChange = { viewModel.setShowFleetOverview(it) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowFleetOverview(!uiState.showFleetOverview) },
                )
            }
            item {
                val daysText = if (uiState.maintenanceDaysWindow == 1) "1 day" else "${uiState.maintenanceDaysWindow} days"
                ListItem(
                    headlineContent = { Text("Maintenance Time Notice Window") },
                    supportingContent = { Text("Notify when service is due within $daysText") },
                    leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMaintenanceDaysDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Notification Permission") },
                    supportingContent = {
                        Text(
                            if (uiState.notificationPermissionGranted) {
                                "Granted — reminders and expiration alerts can be delivered."
                            } else {
                                "Not granted — tap to allow Garage to send notifications."
                            }
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!uiState.notificationPermissionGranted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Send Test Notification") },
                    supportingContent = { Text("Send a sample notification to verify push notifications are working correctly.") },
                    leadingContent = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.sendTestNotification() },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // Integrations Section
            item {
                SettingsCategoryHeader("Integrations")
            }
            item {
                LubeLoggerStatusItem(
                    configured = uiState.lubeLoggerConfigured,
                    serverUrl = uiState.lubeLoggerServerUrl,
                    status = uiState.lubeLoggerStatus,
                    onClick = { showLubeLoggerDialog = true },
                )
            }
            if (uiState.lubeLoggerConfigured) {
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
                    headlineContent = { Text("Local Folder Auto-Backup") },
                    supportingContent = { Text(localBackupStatusText(uiState, context)) },
                    leadingContent = { Icon(Icons.Filled.FolderZip, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLocalBackupDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("WebDAV Cloud Sync") },
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

            // Support & Onboarding Section
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

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            selectedCurrencyCode = uiState.currency,
            onCurrencySelected = { viewModel.setCurrency(it) },
            onDismissRequest = { showCurrencyDialog = false },
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

    if (showMaintenanceMileageDialog) {
        MaintenanceMileageDialog(
            currentMilesWindow = uiState.maintenanceMileageWindow,
            unitSystem = UnitSystem.fromString(uiState.units),
            onOptionSelected = { viewModel.setMaintenanceMileageWindow(it) },
            onDismissRequest = { showMaintenanceMileageDialog = false },
        )
    }

    if (showMaintenanceDaysDialog) {
        MaintenanceDaysDialog(
            currentDaysWindow = uiState.maintenanceDaysWindow,
            onOptionSelected = { viewModel.setMaintenanceDaysWindow(it) },
            onDismissRequest = { showMaintenanceDaysDialog = false },
        )
    }

    if (showDocumentDaysDialog) {
        DocumentDaysDialog(
            currentDaysWindow = uiState.documentExpirationDaysWindow,
            onOptionSelected = { viewModel.setDocumentExpirationDaysWindow(it) },
            onDismissRequest = { showDocumentDaysDialog = false },
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
            onSave = { enabled, url, username, password ->
                viewModel.setCloudSyncEnabled(enabled)
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
            onSetEnabled = viewModel::setLocalBackupEnabled,
            onSelectFolder = { localBackupFolderLauncher.launch(null) },
            onBackupNow = viewModel::triggerLocalBackupNow,
            onDismissRequest = { showLocalBackupDialog = false },
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

private fun localBackupStatusText(uiState: SettingsUiState, context: Context): String {
    if (!uiState.localBackupEnabled) return "Disabled"
    if (uiState.localBackupFolderUri.isBlank()) return "Enabled — no folder selected"
    val folderName = formatFolderUriForDisplay(context, uiState.localBackupFolderUri)
    val error = uiState.lastLocalBackupError
    if (error != null) return "Folder: $folderName — Failed: $error"
    val lastBackup = uiState.lastLocalBackupTimestamp
    return if (lastBackup != null) {
        val relative = DateUtils.getRelativeTimeSpanString(
            lastBackup,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        "Folder: $folderName — Last saved $relative"
    } else {
        "Folder: $folderName — Not yet backed up"
    }
}

fun formatFolderUriForDisplay(context: Context, uriString: String): String {
    if (uriString.isBlank()) return "No folder selected"
    return try {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val name = docFile?.name
        if (!name.isNullOrBlank()) {
            name
        } else {
            val decoded = Uri.decode(uriString)
            val treePart = decoded.substringAfter("/tree/").substringAfter(":")
            if (treePart.isNotBlank() && treePart != uriString) {
                treePart
            } else {
                "Selected Folder"
            }
        }
    } catch (_: Exception) {
        "Selected Folder"
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
private fun DefaultVehicleDialog(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    onVehicleSelected: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Default Vehicle") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleSelected(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedVehicleId == null,
                        onClick = { onVehicleSelected(null) },
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(text = "First vehicle added (default)")
                }

                vehicles.forEach { vehicle ->
                    val vehicleName = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVehicleSelected(vehicle.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = vehicle.id == selectedVehicleId,
                            onClick = { onVehicleSelected(vehicle.id) },
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = vehicleName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
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

@Composable
private fun LocalBackupConfigDialog(
    uiState: SettingsUiState,
    onSetEnabled: (Boolean) -> Unit,
    onSelectFolder: () -> Unit,
    onBackupNow: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(uiState.localBackupEnabled) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Local Folder Auto-Backup") },
        text = {
            Column {
                Text(
                    text = "Automatically saves a backup zip (garage-backup.zip) to a folder on your phone whenever changes are made in Garage. You can sync this folder with custom or 3rd-party cloud storage apps.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable auto-backup", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            onSetEnabled(it)
                        },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Backup Folder:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFolderUriForDisplay(context, uiState.localBackupFolderUri),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.localBackupFolderUri.isBlank()) "Select Folder" else "Change Folder")
                }
                if (uiState.localBackupEnabled && uiState.localBackupFolderUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBackupNow,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Backup to Folder Now")
                    }
                }
                val status = uiState.lastLocalBackupError?.let { "Status: $it" }
                    ?: uiState.lastLocalBackupTimestamp?.let {
                        val relative = DateUtils.getRelativeTimeSpanString(
                            it,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )
                        "Last saved: $relative"
                    }
                if (status != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.lastLocalBackupError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun MaintenanceMileageDialog(
    currentMilesWindow: Int,
    unitSystem: UnitSystem,
    onOptionSelected: (miles: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val options = if (unitSystem == UnitSystem.IMPERIAL) {
        listOf(
            250 to "250 mi",
            500 to "500 mi",
            1000 to "1,000 mi",
        )
    } else {
        listOf(
            UnitConverter.kmToMiles(250) to "250 km",
            UnitConverter.kmToMiles(500) to "500 km",
            UnitConverter.kmToMiles(800) to "800 km (~500 mi equivalent)",
            UnitConverter.kmToMiles(1000) to "1,000 km",
            UnitConverter.kmToMiles(1600) to "1,600 km (~1,000 mi equivalent)",
        )
    }

    val selectedOptionMiles = options.minByOrNull { abs(it.first - currentMilesWindow) }?.first ?: 500

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Maintenance Mileage Notification") },
        text = {
            Column {
                Text(
                    text = "Notify when maintenance is due within:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { (miles, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(miles)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = miles == selectedOptionMiles,
                            onClick = {
                                onOptionSelected(miles)
                                onDismissRequest()
                            },
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MaintenanceDaysDialog(
    currentDaysWindow: Int,
    onOptionSelected: (days: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var selectedDays by remember { mutableFloatStateOf(currentDaysWindow.coerceIn(1, 30).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Maintenance Time Notification") },
        text = {
            Column {
                Text(
                    text = "Notify when maintenance is due within:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                val currentDaysInt = selectedDays.roundToInt()
                val daysLabel = if (currentDaysInt == 1) "1 day" else "$currentDaysInt days"
                Text(
                    text = daysLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = selectedDays,
                    onValueChange = { selectedDays = it },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionSelected(selectedDays.roundToInt())
                    onDismissRequest()
                }
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

@Composable
private fun DocumentDaysDialog(
    currentDaysWindow: Int,
    onOptionSelected: (days: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val options = listOf(7, 14, 30, 60, 90)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Document Expiration Notice Window") },
        text = {
            Column {
                options.forEach { days ->
                    val label = if (days == 1) "1 day before" else "$days days before"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(days)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (days == currentDaysWindow),
                            onClick = {
                                onOptionSelected(days)
                                onDismissRequest()
                            },
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
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
