package com.fearmikey.garage.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Lightweight, UI-friendly snapshot of an adapter found in pairing lists or BLE scans. */
data class ObdDevice(
    val name: String,
    val address: String,
    val type: ObdAdapterType = ObdAdapterType.CLASSIC,
) {
    fun toConfig() = ObdAdapterConfig(type = type, address = address, name = name)

    /** Heuristic so likely OBD adapters can be listed first. */
    val looksLikeObdAdapter: Boolean
        get() = OBD_NAME_HINTS.any { name.contains(it, ignoreCase = true) }

    companion object {
        private val OBD_NAME_HINTS = listOf(
            "obd", "elm", "vlink", "vgate", "icar", "veepeak", "konnwei", "carista", "obdlink", "v-link", "scan",
        )
    }
}

@Singleton
open class ObdConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } catch (e: Exception) {
            null
        }
    }

    private var transport: ObdTransport? = null

    /** Whether Bluetooth is present and switched on. */
    open val isBluetoothEnabled: Boolean
        get() = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            val adapter = bluetoothAdapter ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns paired Bluetooth Classic devices as [ObdDevice]s, resolving names safely (name
     * lookup can throw a SecurityException if BLUETOOTH_CONNECT was revoked). BLE-only
     * devices are excluded since they can't be used over RFCOMM.
     */
    @SuppressLint("MissingPermission")
    open fun getPairedObdDevices(): List<ObdDevice> {
        return getPairedDevices()
            .filter { device -> runCatching { device.type != BluetoothDevice.DEVICE_TYPE_LE }.getOrDefault(true) }
            .map { device ->
                val name = runCatching { device.name }.getOrNull()
                ObdDevice(name = name?.takeIf { it.isNotBlank() } ?: "Unknown Device", address = device.address)
            }
            .sortedWith(compareByDescending<ObdDevice> { it.looksLikeObdAdapter }.thenBy { it.name.lowercase() })
    }

    /**
     * Scans for nearby BLE devices that advertise a name. Requires BLUETOOTH_SCAN. The flow
     * runs until cancelled; collectors should apply their own time limit.
     */
    @SuppressLint("MissingPermission")
    open fun scanBleDevices(): Flow<ObdDevice> {
        val scanner = runCatching { bluetoothAdapter?.takeIf { it.isEnabled }?.bluetoothLeScanner }.getOrNull()
            ?: return emptyFlow()
        return callbackFlow {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val name = result.scanRecord?.deviceName
                        ?: runCatching { result.device.name }.getOrNull()
                    if (!name.isNullOrBlank()) {
                        trySend(ObdDevice(name, result.device.address, ObdAdapterType.BLE))
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    close(IllegalStateException("BLE scan failed ($errorCode)"))
                }
            }
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            runCatching { scanner.startScan(null, settings, callback) }
                .onFailure { close(it) }
            awaitClose { runCatching { scanner.stopScan(callback) } }
        }
    }

    /**
     * Opens a connection to [config]. Any previous connection is closed first.
     *
     * @return true when the transport is open (the adapter may still fail to talk to the car).
     */
    open suspend fun connect(config: ObdAdapterConfig): Boolean {
        disconnect()
        val opened = when (config.type) {
            ObdAdapterType.CLASSIC -> bluetoothAdapter?.let { connectClassic(it, config.address, CLASSIC_CONNECT_TIMEOUT_MS) }
            ObdAdapterType.BLE -> bluetoothAdapter?.takeIf { it.isEnabled }
                ?.let { BleTransport.connect(context, it, config.address, BLE_CONNECT_TIMEOUT_MS) }
            ObdAdapterType.WIFI -> config.wifiHostPort?.let { (host, port) ->
                connectWifi(context, host, port, WIFI_CONNECT_TIMEOUT_MS)
            }
        }
        transport = opened
        return opened != null
    }

    open fun getInputStream(): InputStream? = transport?.input
    open fun getOutputStream(): OutputStream? = transport?.output

    /** Creates an ELM327 session over the current connection, or null if not connected. */
    open fun createSession(): ObdParser? {
        val input = getInputStream() ?: return null
        val output = getOutputStream() ?: return null
        return ObdParser(input, output)
    }

    open fun disconnect() {
        runCatching { transport?.close() }
        transport = null
    }

    companion object {
        const val CLASSIC_CONNECT_TIMEOUT_MS = 8_000L
        const val BLE_CONNECT_TIMEOUT_MS = 15_000L
        const val WIFI_CONNECT_TIMEOUT_MS = 5_000L
    }
}
