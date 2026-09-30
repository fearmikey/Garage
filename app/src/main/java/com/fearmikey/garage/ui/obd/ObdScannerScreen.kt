package com.fearmikey.garage.ui.obd

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ObdScannerScreen(
    vehicleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ObdViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    var hasPermissions by remember {
        mutableStateOf(
            bluetoothPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            viewModel.loadDevices()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.loadDevices()
        } else {
            permissionLauncher.launch(bluetoothPermissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OBD2 Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (!hasPermissions) {
                Text("Bluetooth permissions are required to scan for OBD2 devices.")
                Button(onClick = { permissionLauncher.launch(bluetoothPermissions) }) {
                    Text("Grant Permissions")
                }
                return@Scaffold
            }

            if (uiState.isConnecting || uiState.isConnected) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (uiState.isConnecting) {
                        CircularProgressIndicator()
                        Text("Connecting & Scanning...", modifier = Modifier.padding(top = 64.dp))
                    } else {
                        Text("Connected", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            uiState.error?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (!uiState.isConnecting && uiState.dtcs.isNotEmpty()) {
                Text("Diagnostic Trouble Codes (DTCs)", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.dtcs.forEach { dtc ->
                            Text(dtc, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { 
                            uiState.selectedDeviceAddress?.let { viewModel.clearCodes(it) } 
                        }) {
                            Text("Clear Codes")
                        }
                    }
                }
            } else if (!uiState.isConnecting && uiState.dtcs.isEmpty() && uiState.error == null && uiState.selectedDeviceAddress != null) {
                Text("No Diagnostic Trouble Codes Found.", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.odometerKm != null || uiState.distanceSinceClearedKm != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Vehicle Data", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.odometerKm?.let {
                            Text("Odometer: $it km")
                        }
                        uiState.distanceSinceClearedKm?.let {
                            Text("Distance since codes cleared: $it km")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Paired Bluetooth Devices", style = MaterialTheme.typography.titleMedium)
            
            if (uiState.devices.isEmpty()) {
                Text(
                    text = "No paired Bluetooth devices found. Please ensure your OBD2 adapter is powered on and paired in your phone's Bluetooth settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(uiState.devices) { device ->
                        val deviceName = remember(device) {
                            try {
                                device.name ?: "Unknown Device"
                            } catch (e: Exception) {
                                "Unknown Device"
                            }
                        }
                        ListItem(
                            headlineContent = { Text(deviceName) },
                            supportingContent = { Text(device.address) },
                            leadingContent = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.connectAndScan(device.address) }
                        )
                    }
                }
            }
        }
    }
}
