package com.fearmikey.garage.ui.obd

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fearmikey.garage.R
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdAdapterType
import com.fearmikey.garage.obd.ObdDevice
import com.fearmikey.garage.ui.theme.GarageTheme

/** Runtime permission needed to list and connect to paired Bluetooth devices. */
val obdBluetoothPermissions: Array<String> = arrayOf(Manifest.permission.BLUETOOTH_CONNECT)

/** Only needed (and only requested) when the user scans for Bluetooth LE adapters. */
val obdBleScanPermissions: Array<String> =
    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)

fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun hasObdBluetoothPermissions(context: Context): Boolean = hasPermissions(context, obdBluetoothPermissions)

/** Opens the system Bluetooth settings so the user can pair a new adapter. */
fun openSystemBluetoothSettings(context: Context) = openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)

/** Opens the system Wi-Fi settings so the user can join the adapter's network. */
fun openSystemWifiSettings(context: Context) = openSettings(context, Settings.ACTION_WIFI_SETTINGS)

private fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

object ObdPickerTestTags {
    const val TAB_CLASSIC = "obd_picker_tab_classic"
    const val TAB_BLE = "obd_picker_tab_ble"
    const val TAB_WIFI = "obd_picker_tab_wifi"
    const val WIFI_HOST = "obd_picker_wifi_host"
    const val WIFI_PORT = "obd_picker_wifi_port"
    const val WIFI_SAVE = "obd_picker_wifi_save"
}

/**
 * Modal dialog for choosing an OBD2 adapter: paired Bluetooth Classic devices, nearby
 * Bluetooth LE devices (scanned on demand), or a Wi-Fi adapter by host/port.
 *
 * @param onClearSelection when non-null, a "Forget adapter" action is shown.
 */
@Composable
fun ObdDevicePickerDialog(
    pairedDevices: List<ObdDevice>,
    bleDevices: List<ObdDevice>,
    isBleScanning: Boolean,
    selected: ObdAdapterConfig?,
    onAdapterSelected: (ObdAdapterConfig) -> Unit,
    onDismissRequest: () -> Unit,
    onRefreshPaired: () -> Unit,
    onStartBleScan: () -> Unit,
    onClearSelection: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf((selected?.type ?: ObdAdapterType.CLASSIC).ordinal) }
    var hasConnectPermission by rememberSaveable { mutableStateOf(hasObdBluetoothPermissions(context)) }

    val connectPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasConnectPermission = results.values.all { it }
        if (hasConnectPermission) onRefreshPaired()
    }
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            hasConnectPermission = true
            onStartBleScan()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        // The default dialog width (~280dp of content) is too narrow for three tabs plus
        // explanatory text, especially with larger system font sizes.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        title = { Text(stringResource(R.string.obd_picker_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SecondaryTabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                ) {
                    PickerTab(
                        selected = tab == ObdAdapterType.CLASSIC.ordinal,
                        onClick = { tab = ObdAdapterType.CLASSIC.ordinal },
                        label = stringResource(R.string.obd_picker_tab_bluetooth),
                        testTag = ObdPickerTestTags.TAB_CLASSIC,
                    )
                    PickerTab(
                        selected = tab == ObdAdapterType.BLE.ordinal,
                        onClick = { tab = ObdAdapterType.BLE.ordinal },
                        label = stringResource(R.string.obd_picker_tab_ble),
                        testTag = ObdPickerTestTags.TAB_BLE,
                    )
                    PickerTab(
                        selected = tab == ObdAdapterType.WIFI.ordinal,
                        onClick = { tab = ObdAdapterType.WIFI.ordinal },
                        label = stringResource(R.string.obd_picker_tab_wifi),
                        testTag = ObdPickerTestTags.TAB_WIFI,
                    )
                }
                Spacer(Modifier.height(12.dp))
                when (ObdAdapterType.entries[tab]) {
                    ObdAdapterType.CLASSIC -> ClassicTab(
                        hasPermission = hasConnectPermission,
                        devices = pairedDevices,
                        selected = selected,
                        onRequestPermission = { connectPermissionLauncher.launch(obdBluetoothPermissions) },
                        onSelect = { onAdapterSelected(it.toConfig()) },
                        onPairNew = { openSystemBluetoothSettings(context) },
                        onRefresh = onRefreshPaired,
                    )
                    ObdAdapterType.BLE -> BleTab(
                        devices = bleDevices,
                        isScanning = isBleScanning,
                        selected = selected,
                        onScan = {
                            if (hasPermissions(context, obdBleScanPermissions)) {
                                onStartBleScan()
                            } else {
                                scanPermissionLauncher.launch(obdBleScanPermissions)
                            }
                        },
                        onSelect = { onAdapterSelected(it.toConfig()) },
                    )
                    ObdAdapterType.WIFI -> WifiTab(
                        selected = selected?.takeIf { it.type == ObdAdapterType.WIFI },
                        onSave = onAdapterSelected,
                        onOpenWifiSettings = { openSystemWifiSettings(context) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.obd_cancel)) }
        },
        dismissButton = onClearSelection?.let { clear ->
            {
                TextButton(onClick = clear) {
                    Text(stringResource(R.string.obd_picker_forget_adapter), color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

@Composable
private fun PickerTab(selected: Boolean, onClick: () -> Unit, label: String, testTag: String) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun ClassicTab(
    hasPermission: Boolean,
    devices: List<ObdDevice>,
    selected: ObdAdapterConfig?,
    onRequestPermission: () -> Unit,
    onSelect: (ObdDevice) -> Unit,
    onPairNew: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column {
        if (!hasPermission) {
            HintText(stringResource(R.string.obd_picker_classic_permission_hint))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.obd_picker_allow_bluetooth))
            }
            return@Column
        }
        if (devices.isEmpty()) {
            EmptyHint(
                text = stringResource(R.string.obd_picker_classic_empty),
            )
        } else {
            HintText(stringResource(R.string.obd_picker_classic_hint))
            Spacer(Modifier.height(8.dp))
            DeviceList(devices = devices, selected = selected, onSelect = onSelect)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        OutlinedButton(onClick = onPairNew, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.obd_picker_pair_new))
        }
        TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.obd_picker_refresh_list))
        }
    }
}

@Composable
private fun BleTab(
    devices: List<ObdDevice>,
    isScanning: Boolean,
    selected: ObdAdapterConfig?,
    onScan: () -> Unit,
    onSelect: (ObdDevice) -> Unit,
) {
    Column {
        HintText(stringResource(R.string.obd_picker_ble_hint))
        Spacer(Modifier.height(8.dp))
        if (devices.isNotEmpty()) {
            DeviceList(devices = devices, selected = selected, onSelect = onSelect)
            Spacer(Modifier.height(8.dp))
        } else if (!isScanning) {
            EmptyHint(stringResource(R.string.obd_picker_ble_empty))
        }
        Button(onClick = onScan, enabled = !isScanning, modifier = Modifier.fillMaxWidth()) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.obd_picker_scanning))
            } else {
                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (devices.isEmpty()) R.string.obd_picker_scan_for_adapters else R.string.obd_picker_scan_again
                    )
                )
            }
        }
    }
}

@Composable
private fun WifiTab(
    selected: ObdAdapterConfig?,
    onSave: (ObdAdapterConfig) -> Unit,
    onOpenWifiSettings: () -> Unit,
) {
    val current = selected?.wifiHostPort
    var host by rememberSaveable { mutableStateOf(current?.first ?: ObdAdapterConfig.DEFAULT_WIFI_HOST) }
    var port by rememberSaveable { mutableStateOf((current?.second ?: ObdAdapterConfig.DEFAULT_WIFI_PORT).toString()) }
    val portNumber = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val hostValid = host.isNotBlank() && host.none { it.isWhitespace() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HintText(stringResource(R.string.obd_picker_wifi_hint))
        OutlinedButton(onClick = onOpenWifiSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.obd_picker_open_wifi_settings))
        }
        OutlinedTextField(
            value = host,
            onValueChange = { host = it.trim() },
            label = { Text(stringResource(R.string.obd_picker_wifi_host_label)) },
            singleLine = true,
            isError = !hostValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().testTag(ObdPickerTestTags.WIFI_HOST),
        )
        OutlinedTextField(
            value = port,
            onValueChange = { value -> port = value.filter(Char::isDigit).take(5) },
            label = { Text(stringResource(R.string.obd_picker_wifi_port_label)) },
            singleLine = true,
            isError = portNumber == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag(ObdPickerTestTags.WIFI_PORT),
        )
        Button(
            onClick = { onSave(ObdAdapterConfig.wifi(host, portNumber ?: return@Button)) },
            enabled = hostValid && portNumber != null,
            modifier = Modifier.fillMaxWidth().testTag(ObdPickerTestTags.WIFI_SAVE),
        ) {
            Text(stringResource(R.string.obd_picker_use_wifi_adapter))
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<ObdDevice>,
    selected: ObdAdapterConfig?,
    onSelect: (ObdDevice) -> Unit,
) {
    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
        items(devices, key = { it.type.name + it.address }) { device ->
            val isSelected = device.address == selected?.address && device.type == selected.type
            ListItem(
                headlineContent = { Text(device.name) },
                supportingContent = { Text(device.address) },
                leadingContent = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                trailingContent = {
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.obd_picker_cd_current_adapter), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.clickable { onSelect(device) },
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyHint(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun ObdDevicePickerDialogPreview() {
    GarageTheme {
        ObdDevicePickerDialog(
            pairedDevices = listOf(
                ObdDevice("OBDII", "00:1D:A5:68:98:8B"),
                ObdDevice("Veepeak VP11", "AA:BB:CC:DD:EE:FF"),
            ),
            bleDevices = emptyList(),
            isBleScanning = false,
            selected = ObdAdapterConfig(ObdAdapterType.CLASSIC, "AA:BB:CC:DD:EE:FF", "Veepeak VP11"),
            onAdapterSelected = {},
            onDismissRequest = {},
            onRefreshPaired = {},
            onStartBleScan = {},
            onClearSelection = {},
        )
    }
}
