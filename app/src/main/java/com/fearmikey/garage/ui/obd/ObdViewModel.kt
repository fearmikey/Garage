package com.fearmikey.garage.ui.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.obd.ObdConnectionManager
import com.fearmikey.garage.obd.ObdParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("MissingPermission")
class ObdViewModel @Inject constructor(
    private val obdConnectionManager: ObdConnectionManager,
) : ViewModel() {

    private val parser = ObdParser()

    data class UiState(
        val isConnecting: Boolean = false,
        val isConnected: Boolean = false,
        val error: String? = null,
        val devices: List<BluetoothDevice> = emptyList(),
        val dtcs: List<String> = emptyList(),
        val odometerKm: Int? = null,
        val distanceSinceClearedKm: Int? = null,
        val selectedDeviceAddress: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadDevices()
    }

    fun loadDevices() {
        val devices = obdConnectionManager.getPairedDevices()
        _uiState.update { it.copy(devices = devices) }
    }

    fun connectAndScan(deviceAddress: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, error = null, selectedDeviceAddress = deviceAddress, dtcs = emptyList(), odometerKm = null, distanceSinceClearedKm = null) }
            val connected = obdConnectionManager.connectToClassicDevice(deviceAddress)
            
            if (connected) {
                _uiState.update { it.copy(isConnecting = false, isConnected = true) }
                performObdScan()
            } else {
                _uiState.update { it.copy(isConnecting = false, isConnected = false, error = "Failed to connect. Make sure the device is on and in range.") }
            }
        }
    }

    private suspend fun performObdScan() {
        val input = obdConnectionManager.getInputStream()
        val output = obdConnectionManager.getOutputStream()
        
        if (input == null || output == null) {
            _uiState.update { it.copy(error = "Connection lost.", isConnected = false) }
            return
        }

        try {
            parser.init(input, output)
            
            val dtcs = parser.getDtcs(input, output)
            _uiState.update { it.copy(dtcs = dtcs) }
            
            val odometer = parser.getOdometerKm(input, output)
            _uiState.update { it.copy(odometerKm = odometer) }
            
            val distanceCleared = parser.getDistanceSinceCodesClearedKm(input, output)
            _uiState.update { it.copy(distanceSinceClearedKm = distanceCleared) }
            
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(error = "Error communicating with OBD adapter: ${e.message}") }
        } finally {
            obdConnectionManager.disconnect()
            _uiState.update { it.copy(isConnected = false) }
        }
    }
    
    fun clearCodes(deviceAddress: String) {
        viewModelScope.launch {
             _uiState.update { it.copy(isConnecting = true, error = null) }
            val connected = obdConnectionManager.connectToClassicDevice(deviceAddress)
            
            if (connected) {
                 _uiState.update { it.copy(isConnecting = false, isConnected = true) }
                val input = obdConnectionManager.getInputStream()
                val output = obdConnectionManager.getOutputStream()
                
                if (input != null && output != null) {
                    try {
                        parser.init(input, output)
                        parser.clearDtcs(input, output)
                        // Wait a bit, then rescan
                        performObdScan()
                    } catch (e: Exception) {
                        e.printStackTrace()
                         _uiState.update { it.copy(error = "Failed to clear codes: ${e.message}") }
                    }
                }
            } else {
                 _uiState.update { it.copy(isConnecting = false, isConnected = false, error = "Failed to connect to clear codes.") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        obdConnectionManager.disconnect()
    }
}
