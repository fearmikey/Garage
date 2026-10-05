package com.fearmikey.garage.ui.obd

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.obd.DtcDescriptions
import com.fearmikey.garage.obd.FreezeFrame
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdAdapterType
import com.fearmikey.garage.obd.MonitorStatus
import com.fearmikey.garage.obd.ObdFormatter
import com.fearmikey.garage.obd.ObdPid
import com.fearmikey.garage.obd.ObdReading
import com.fearmikey.garage.obd.ObdScanResult
import com.fearmikey.garage.obd.ObdUnit
import com.fearmikey.garage.obd.ReadinessMonitor
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

@SuppressLint("MissingPermission")
@Composable
fun ObdScannerScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    unitSystem: UnitSystem,
    /** Invoked (from a coroutine) whenever a scan or code-clear is written to the timeline. */
    onTimelineUpdated: suspend () -> Unit,
    viewModel: ObdViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnTimelineUpdated by rememberUpdatedState(onTimelineUpdated)

    var hasPermissions by remember { mutableStateOf(hasObdBluetoothPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    LaunchedEffect(vehicleId) {
        viewModel.setVehicleId(vehicleId)
    }

    // Wi-Fi adapters don't need any Bluetooth permission.
    val needsBluetooth = uiState.savedAdapter?.type?.requiresBluetooth == true
    LaunchedEffect(uiState.isPreferenceLoaded, needsBluetooth, hasPermissions) {
        if (!uiState.isPreferenceLoaded) return@LaunchedEffect
        if (!needsBluetooth || hasPermissions) {
            viewModel.onPermissionsGranted()
        } else {
            permissionLauncher.launch(obdBluetoothPermissions)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.timelineEvents.collect { currentOnTimelineUpdated() }
    }

    // Don't keep polling the vehicle while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.stopLiveData()
                // Permission may have been granted from the picker dialog or system settings.
                Lifecycle.Event.ON_RESUME -> hasPermissions = hasObdBluetoothPermissions(context)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ObdScannerContent(
        uiState = uiState,
        unitSystem = unitSystem,
        needsPermission = needsBluetooth && !hasPermissions,
        onNavigateBack = onNavigateBack,
        onRequestPermissions = { permissionLauncher.launch(obdBluetoothPermissions) },
        onOpenDevicePicker = viewModel::openDevicePicker,
        onRescan = viewModel::rescan,
        onClearCodes = viewModel::clearCodes,
        onToggleLiveData = viewModel::toggleLiveData,
        onSaveVin = viewModel::saveVinToVehicle,
        onLogAnyway = viewModel::logScanAnyway,
    )

    if (uiState.isDevicePickerOpen) {
        ObdDevicePickerDialog(
            pairedDevices = uiState.devices,
            bleDevices = uiState.bleDevices,
            isBleScanning = uiState.isBleScanning,
            selected = uiState.savedAdapter,
            onAdapterSelected = { config ->
                hasPermissions = hasObdBluetoothPermissions(context)
                viewModel.selectAdapter(config)
            },
            onDismissRequest = viewModel::closeDevicePicker,
            onRefreshPaired = viewModel::loadDevices,
            onStartBleScan = { viewModel.startBleScan() },
            onClearSelection = if (uiState.hasSavedDevice) viewModel::clearSavedDevice else null,
        )
    }
}

/** Stateless scanner UI; `internal` so it can be exercised directly by Compose UI tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObdScannerContent(
    uiState: ObdViewModel.UiState,
    unitSystem: UnitSystem,
    needsPermission: Boolean,
    onNavigateBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDevicePicker: () -> Unit,
    onRescan: () -> Unit,
    onClearCodes: () -> Unit,
    onToggleLiveData: () -> Unit,
    onSaveVin: () -> Unit,
    onLogAnyway: () -> Unit,
) {
    var showClearCodesConfirm by rememberSaveable { mutableStateOf(false) }
    val result = uiState.scanResult
    val clearingCodesMessage = stringResource(R.string.obd_clearing_codes)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.obd_scanner_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.obd_cd_back))
                    }
                },
                actions = {
                    if (!needsPermission && uiState.hasSavedDevice) {
                        IconButton(onClick = onRescan, enabled = !uiState.isBusy) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.obd_rescan))
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        when {
            needsPermission -> PermissionRequiredState(
                modifier = Modifier.padding(innerPadding),
                onRequestPermissions = onRequestPermissions,
            )

            !uiState.isPreferenceLoaded -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !uiState.hasSavedDevice -> NoAdapterState(
                modifier = Modifier.padding(innerPadding),
                onSelectAdapter = onOpenDevicePicker,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AdapterStatusCard(
                        uiState = uiState,
                        onRescan = onRescan,
                        onChangeAdapter = onOpenDevicePicker,
                    )
                }

                uiState.error?.let { message -> item { ErrorBanner(message) } }

                val progress = when {
                    uiState.isClearingCodes -> clearingCodesMessage
                    else -> uiState.progressMessage
                }
                if (progress != null) {
                    item { ScanningIndicator(progress) }
                }

                if (result != null) {
                    uiState.vinCheck?.takeIf { it != ObdViewModel.VinCheck.MATCH }?.let { check ->
                        item {
                            VinCheckBanner(
                                check = check,
                                scannedVin = result.vin.orEmpty(),
                                vehicleVin = uiState.vehicleVin,
                                showLogAnyway = uiState.timelineLogResult == ObdViewModel.TimelineLogResult.SKIPPED_VIN_MISMATCH,
                                onSaveVin = onSaveVin,
                                onLogAnyway = onLogAnyway,
                            )
                        }
                    }

                    result.monitorStatus?.let { status -> item { VehicleHealthCard(status) } }

                    item {
                        DtcCard(
                            result = result,
                            enabled = uiState.isConnected && !uiState.isBusy,
                            onClearCodes = { showClearCodesConfirm = true },
                        )
                    }

                    result.freezeFrame?.let { frame ->
                        item { FreezeFrameCard(frame = frame, unitSystem = unitSystem) }
                    }

                    if (result.odometerKm != null || result.distanceSinceClearedKm != null) {
                        item {
                            DistanceCard(
                                odometerKm = result.odometerKm,
                                distanceSinceClearedKm = result.distanceSinceClearedKm,
                                unitSystem = unitSystem,
                            )
                        }
                    }

                    uiState.timelineLogResult?.let { log -> item { TimelineLogNote(log) } }
                }

                if (uiState.isConnected && result != null && uiState.progressMessage == null) {
                    item {
                        LiveDataCard(
                            isActive = uiState.isLiveDataActive,
                            readings = uiState.liveReadings,
                            unitSystem = unitSystem,
                            enabled = !uiState.isBusy,
                            onToggle = onToggleLiveData,
                        )
                    }
                }

                if (result != null && (result.readings.isNotEmpty() || result.vin != null || result.adapterVoltage != null)) {
                    item { SnapshotCard(result = result, unitSystem = unitSystem) }
                }
            }
        }
    }

    if (showClearCodesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCodesConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.obd_clear_codes_confirm_title)) },
            text = {
                Text(stringResource(R.string.obd_clear_codes_confirm_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearCodesConfirm = false
                    onClearCodes()
                }) {
                    Text(stringResource(R.string.obd_clear_codes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCodesConfirm = false }) { Text(stringResource(R.string.obd_cancel)) }
            },
        )
    }
}

@Composable
private fun PermissionRequiredState(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit,
) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.BluetoothDisabled,
        title = stringResource(R.string.obd_permission_title),
        body = stringResource(R.string.obd_permission_message),
        actionLabel = stringResource(R.string.obd_permission_action),
        onAction = onRequestPermissions,
    )
}

@Composable
private fun NoAdapterState(
    modifier: Modifier = Modifier,
    onSelectAdapter: () -> Unit,
) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.Bluetooth,
        title = stringResource(R.string.obd_no_adapter_title),
        body = stringResource(R.string.obd_no_adapter_message),
        actionLabel = stringResource(R.string.obd_no_adapter_action),
        onAction = onSelectAdapter,
    )
}

@Composable
private fun CenteredMessage(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private val StatusGreen = Color(0xFF2E7D32)
private val StatusAmber = Color(0xFFB26A00)

@Composable
private fun AdapterStatusCard(
    uiState: ObdViewModel.UiState,
    onRescan: () -> Unit,
    onChangeAdapter: () -> Unit,
) {
    val isWifi = uiState.savedAdapter?.type == ObdAdapterType.WIFI
    val (statusLabel, statusColor, statusIcon) = when (uiState.connectionStatus) {
        ObdViewModel.ConnectionStatus.CONNECTED ->
            Triple(stringResource(R.string.obd_status_connected), StatusGreen, if (isWifi) Icons.Filled.Wifi else Icons.Filled.BluetoothConnected)
        ObdViewModel.ConnectionStatus.CONNECTING ->
            Triple(stringResource(R.string.obd_status_connecting), MaterialTheme.colorScheme.primary, if (isWifi) Icons.Filled.Wifi else Icons.Filled.Bluetooth)
        ObdViewModel.ConnectionStatus.DISCONNECTED ->
            Triple(stringResource(R.string.obd_status_disconnected), StatusAmber, if (isWifi) Icons.Filled.WifiOff else Icons.Filled.BluetoothDisabled)
        ObdViewModel.ConnectionStatus.IDLE ->
            Triple(stringResource(R.string.obd_status_not_connected), MaterialTheme.colorScheme.outline, if (isWifi) Icons.Filled.Wifi else Icons.Filled.Bluetooth)
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.obd_adapter_header, uiState.savedAdapter?.type?.displayName.orEmpty()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        uiState.savedAdapter?.name ?: stringResource(R.string.obd_unknown_device),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val detail = uiState.scanResult?.protocolName ?: uiState.savedAdapter?.address
                    detail?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                StatusPill(label = statusLabel, color = statusColor)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onRescan,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (uiState.isConnected) R.string.obd_rescan else R.string.obd_reconnect))
                }
                OutlinedButton(onClick = onChangeAdapter, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.obd_change))
                }
            }
        }
    }
}

@Composable
private fun VinCheckBanner(
    check: ObdViewModel.VinCheck,
    scannedVin: String,
    vehicleVin: String?,
    showLogAnyway: Boolean,
    onSaveVin: () -> Unit,
    onLogAnyway: () -> Unit,
) {
    val isMismatch = check == ObdViewModel.VinCheck.MISMATCH
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isMismatch) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isMismatch) Icons.Filled.Warning else Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(if (isMismatch) R.string.obd_vin_mismatch_title else R.string.obd_vin_save_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (isMismatch) {
                    stringResource(R.string.obd_vin_mismatch_message, scannedVin, vehicleVin.orEmpty())
                } else {
                    stringResource(R.string.obd_vin_save_message, scannedVin)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!isMismatch || showLogAnyway) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (isMismatch) {
                        TextButton(onClick = onLogAnyway) { Text(stringResource(R.string.obd_save_scan_anyway)) }
                    } else {
                        TextButton(onClick = onSaveVin) { Text(stringResource(R.string.obd_save_vin)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeFrameCard(frame: FreezeFrame, unitSystem: UnitSystem) {
    Column {
        SectionTitle(stringResource(R.string.obd_freeze_frame_title))
        Text(
            stringResource(
                R.string.obd_freeze_frame_description,
                frame.dtc ?: stringResource(R.string.obd_freeze_frame_code_fallback),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (frame.readings.isEmpty()) {
            Text(stringResource(R.string.obd_freeze_frame_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            ReadingGrid(readings = frame.readings, unitSystem = unitSystem)
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ScanningIndicator(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(bottom = 8.dp))
}

@Composable
private fun VehicleHealthCard(status: MonitorStatus) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val incomplete = status.incompleteCount
    // Most US emissions programs allow at most one incomplete monitor (two for 1996–2000).
    val readinessOk = !status.milOn && incomplete <= 1

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.obd_vehicle_health_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            HealthRow(
                ok = !status.milOn,
                title = stringResource(if (status.milOn) R.string.obd_mil_on else R.string.obd_mil_off),
                subtitle = if (status.storedDtcCount > 0) {
                    pluralStringResource(
                        R.plurals.obd_emissions_codes_reported,
                        status.storedDtcCount,
                        status.storedDtcCount,
                    )
                } else {
                    null
                },
            )

            if (status.monitors.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HealthRow(
                    ok = readinessOk,
                    title = stringResource(R.string.obd_readiness_summary, status.completeCount, status.monitors.size),
                    subtitle = when {
                        status.milOn -> stringResource(R.string.obd_readiness_mil_on)
                        readinessOk -> stringResource(R.string.obd_readiness_ok)
                        else -> stringResource(R.string.obd_readiness_not_ready)
                    },
                    modifier = Modifier.clickable { expanded = !expanded },
                    trailing = {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.obd_cd_hide_monitors else R.string.obd_cd_show_monitors
                            ),
                        )
                    },
                )
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(start = 36.dp, top = 8.dp)) {
                        status.monitors.forEach { MonitorRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRow(
    ok: Boolean,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) StatusGreen else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun MonitorRow(monitor: ReadinessMonitor) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (monitor.complete) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = stringResource(
                if (monitor.complete) R.string.obd_cd_monitor_complete else R.string.obd_cd_monitor_incomplete
            ),
            tint = if (monitor.complete) StatusGreen else StatusAmber,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(monitor.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            stringResource(if (monitor.complete) R.string.obd_monitor_ready else R.string.obd_monitor_not_ready),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DtcCard(
    result: ObdScanResult,
    enabled: Boolean,
    onClearCodes: () -> Unit,
) {
    if (!result.hasAnyCodes) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusGreen)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.obd_no_codes_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.obd_no_codes_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    val total = (result.storedDtcs + result.pendingDtcs + result.permanentDtcs).distinct().size
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.obd_dtc_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(
                    label = pluralStringResource(R.plurals.obd_code_count, total, total),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DtcSection(
                title = stringResource(R.string.obd_dtc_stored_title),
                explanation = stringResource(R.string.obd_dtc_stored_explanation),
                codes = result.storedDtcs,
                sources = result.dtcSources,
                color = MaterialTheme.colorScheme.error,
            )
            DtcSection(
                title = stringResource(R.string.obd_dtc_pending_title),
                explanation = stringResource(R.string.obd_dtc_pending_explanation),
                codes = result.pendingDtcs,
                sources = result.dtcSources,
                color = StatusAmber,
            )
            DtcSection(
                title = stringResource(R.string.obd_dtc_permanent_title),
                explanation = stringResource(R.string.obd_dtc_permanent_explanation),
                codes = result.permanentDtcs,
                sources = result.dtcSources,
                color = MaterialTheme.colorScheme.error,
            )

            if (result.storedDtcs.isNotEmpty() || result.pendingDtcs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onClearCodes, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.obd_clear_codes))
                }
            }
        }
    }
}

@Composable
private fun DtcSection(
    title: String,
    explanation: String,
    codes: List<String>,
    sources: Map<String, List<String>>,
    color: Color,
) {
    if (codes.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.labelLarge, color = color)
    Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    codes.forEach { code ->
        val description = remember(code) { DtcDescriptions.describe(code) }
        Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    description.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = if (description.isExact) FontStyle.Normal else FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                sources[code]?.takeIf { it.isNotEmpty() }?.let { modules ->
                    Text(
                        stringResource(R.string.obd_dtc_reported_by, modules.joinToString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun DistanceCard(
    odometerKm: Int?,
    distanceSinceClearedKm: Int?,
    unitSystem: UnitSystem,
) {
    Column {
        SectionTitle(stringResource(R.string.obd_distance_title))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                odometerKm?.let { km ->
                    Text(
                        stringResource(R.string.obd_odometer),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        UnitConverter.formatDistance(UnitConverter.kmToMiles(km), unitSystem),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                distanceSinceClearedKm?.let { km ->
                    if (odometerKm != null) HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        stringResource(R.string.obd_since_codes_cleared),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        UnitConverter.formatDistance(UnitConverter.kmToMiles(km), unitSystem),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineLogNote(result: ObdViewModel.TimelineLogResult) {
    val (icon, text) = when (result) {
        ObdViewModel.TimelineLogResult.SAVED ->
            Icons.Filled.CheckCircle to stringResource(R.string.obd_timeline_saved)
        ObdViewModel.TimelineLogResult.UNCHANGED ->
            Icons.Filled.Info to stringResource(R.string.obd_timeline_unchanged)
        ObdViewModel.TimelineLogResult.SKIPPED_VIN_MISMATCH ->
            Icons.Filled.Warning to stringResource(R.string.obd_timeline_vin_mismatch)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LiveDataCard(
    isActive: Boolean,
    readings: List<ObdReading>,
    unitSystem: UnitSystem,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.obd_live_data_title), modifier = Modifier.weight(1f))
            if (isActive) StatusPill(label = stringResource(R.string.obd_live), color = StatusGreen)
        }
        if (isActive && readings.isNotEmpty()) {
            ReadingGrid(readings = readings, unitSystem = unitSystem)
            Spacer(Modifier.height(8.dp))
        } else if (isActive) {
            ScanningIndicator(stringResource(R.string.obd_waiting_for_data))
            Spacer(Modifier.height(8.dp))
        }
        FilledTonalButton(onClick = onToggle, enabled = enabled || isActive, modifier = Modifier.fillMaxWidth()) {
            Icon(if (isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (isActive) R.string.obd_stop_live_data else R.string.obd_start_live_data))
        }
    }
}

@Composable
private fun SnapshotCard(result: ObdScanResult, unitSystem: UnitSystem) {
    Column {
        SectionTitle(stringResource(R.string.obd_snapshot_title))
        val batteryLabel = stringResource(R.string.obd_battery_at_port)
        val vinLabel = stringResource(R.string.obd_vin)
        val tiles = buildList {
            result.adapterVoltage?.let {
                add(batteryLabel to ObdFormatter.format(ObdUnit.VOLTAGE, it, unitSystem))
            }
            result.readings.forEach { add(it.pid.label to ObdFormatter.format(it, unitSystem)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            result.vin?.let { StatTile(vinLabel, it, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun ReadingGrid(readings: List<ObdReading>, unitSystem: UnitSystem) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        readings.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { StatTile(it.pid.label, ObdFormatter.format(it, unitSystem), Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val previewResult = ObdScanResult(
    storedDtcs = listOf("P0420"),
    pendingDtcs = listOf("P0171"),
    dtcSources = mapOf("P0420" to listOf("Engine (ECM)"), "P0171" to listOf("Engine (ECM)")),
    freezeFrame = FreezeFrame(
        dtc = "P0420",
        readings = listOf(
            ObdReading(ObdPid.RPM, 2450.0),
            ObdReading(ObdPid.SPEED, 88.0),
            ObdReading(ObdPid.COOLANT_TEMP, 91.0),
            ObdReading(ObdPid.LONG_FUEL_TRIM_1, 7.8),
        ),
    ),
    monitorStatus = MonitorStatus(
        milOn = true,
        storedDtcCount = 1,
        isDiesel = false,
        monitors = listOf(
            ReadinessMonitor("Misfire", true),
            ReadinessMonitor("Fuel System", true),
            ReadinessMonitor("Components", true),
            ReadinessMonitor("Catalyst", false),
            ReadinessMonitor("Evaporative System", false),
            ReadinessMonitor("Oxygen Sensor", true),
        ),
    ),
    odometerKm = 120_000,
    distanceSinceClearedKm = 850,
    vin = "1HGCM82633A004352",
    readings = listOf(
        ObdReading(ObdPid.COOLANT_TEMP, 88.0),
        ObdReading(ObdPid.RPM, 780.0),
        ObdReading(ObdPid.FUEL_LEVEL, 64.0),
    ),
    adapterVoltage = 14.2,
    protocolName = "ISO 15765-4 CAN (11 bit, 500 kbaud)",
)

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun ObdScannerConnectedPreview() {
    GarageTheme {
        ObdScannerContent(
            uiState = ObdViewModel.UiState(
                connectionStatus = ObdViewModel.ConnectionStatus.CONNECTED,
                isPreferenceLoaded = true,
                savedAdapter = ObdAdapterConfig(ObdAdapterType.CLASSIC, "AA:BB:CC:DD:EE:FF", "Veepeak VP11"),
                scanResult = previewResult,
                timelineLogResult = ObdViewModel.TimelineLogResult.SAVED,
                vinCheck = ObdViewModel.VinCheck.VEHICLE_HAS_NO_VIN,
                isLiveDataActive = true,
                liveReadings = listOf(
                    ObdReading(ObdPid.RPM, 2150.0),
                    ObdReading(ObdPid.SPEED, 72.0),
                    ObdReading(ObdPid.ENGINE_LOAD, 31.0),
                    ObdReading(ObdPid.CONTROL_MODULE_VOLTAGE, 14.1),
                ),
            ),
            unitSystem = UnitSystem.IMPERIAL,
            needsPermission = false,
            onNavigateBack = {},
            onRequestPermissions = {},
            onOpenDevicePicker = {},
            onRescan = {},
            onClearCodes = {},
            onToggleLiveData = {},
            onSaveVin = {},
            onLogAnyway = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ObdScannerNoAdapterPreview() {
    GarageTheme {
        ObdScannerContent(
            uiState = ObdViewModel.UiState(isPreferenceLoaded = true),
            unitSystem = UnitSystem.METRIC,
            needsPermission = false,
            onNavigateBack = {},
            onRequestPermissions = {},
            onOpenDevicePicker = {},
            onRescan = {},
            onClearCodes = {},
            onToggleLiveData = {},
            onSaveVin = {},
            onLogAnyway = {},
        )
    }
}
