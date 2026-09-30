package com.fearmikey.garage.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObdConnectionManager @Inject constructor(
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

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var socket: BluetoothSocket? = null

    // Standard SPP UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

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

    @SuppressLint("MissingPermission")
    suspend fun connectToClassicDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext false
        
        try {
            if (!adapter.isEnabled) return@withContext false
            val device = adapter.getRemoteDevice(deviceAddress) ?: return@withContext false
            
            try {
                adapter.cancelDiscovery()
            } catch (e: Exception) {
                // Ignore cancel discovery failure
            }

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            
            return@withContext inputStream != null && outputStream != null
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            return@withContext false
        }
    }

    fun getInputStream(): InputStream? = inputStream
    fun getOutputStream(): OutputStream? = outputStream

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
