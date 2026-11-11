package com.fearmikey.garage.ui.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showMaintenanceMileageDialog by remember { mutableStateOf(false) }
    var showMaintenanceDaysDialog by remember { mutableStateOf(false) }
    var showDocumentDaysDialog by remember { mutableStateOf(false) }

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
                title = { Text(stringResource(R.string.settings_reminders_notifications)) },
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
                SettingsCategoryHeader(stringResource(R.string.reminders_header_documents))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminders_doc_title)) },
                    supportingContent = { Text(stringResource(R.string.reminders_doc_summary)) },
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
                    val days = uiState.documentExpirationDaysWindow
                    val daysText = pluralStringResource(R.plurals.common_days, days, days)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_doc_window_title)) },
                        supportingContent = { Text(stringResource(R.string.reminders_doc_window_summary, daysText)) },
                        leadingContent = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDocumentDaysDialog = true },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader(stringResource(R.string.reminders_header_service))
            }
            item {
                val unitSystem = UnitSystem.fromString(uiState.units)
                val distanceText = UnitConverter.formatDistance(uiState.maintenanceMileageWindow, unitSystem)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminders_mileage_title)) },
                    supportingContent = { Text(stringResource(R.string.reminders_service_due_within, distanceText)) },
                    leadingContent = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMaintenanceMileageDialog = true },
                )
            }
            item {
                val days = uiState.maintenanceDaysWindow
                val daysText = pluralStringResource(R.plurals.common_days, days, days)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminders_time_title)) },
                    supportingContent = { Text(stringResource(R.string.reminders_service_due_within, daysText)) },
                    leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMaintenanceDaysDialog = true },
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader(stringResource(R.string.reminders_header_push))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminders_permission_title)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (uiState.notificationPermissionGranted) {
                                    R.string.reminders_permission_granted
                                } else {
                                    R.string.reminders_permission_denied
                                }
                            )
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
                    headlineContent = { Text(stringResource(R.string.reminders_test_title)) },
                    supportingContent = { Text(stringResource(R.string.reminders_test_summary)) },
                    leadingContent = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.sendTestNotification() },
                )
            }
        }
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
}
