package com.fearmikey.garage.ui.obd

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdAdapterException
import com.fearmikey.garage.obd.ObdAdapterType
import com.fearmikey.garage.obd.ObdConnectionManager
import com.fearmikey.garage.obd.ObdDevice
import com.fearmikey.garage.obd.ObdParser
import com.fearmikey.garage.obd.ObdPid
import com.fearmikey.garage.obd.ObdReading
import com.fearmikey.garage.obd.ObdScanResult
import com.fearmikey.garage.obd.ObdScanSummary
import com.fearmikey.garage.obd.ObdTimeoutException
import com.fearmikey.garage.ui.util.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
@SuppressLint("MissingPermission")
class ObdViewModel @Inject constructor(
    private val obdConnectionManager: ObdConnectionManager,
    private val maintenanceRepository: MaintenanceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    enum class ConnectionStatus { IDLE, CONNECTING, CONNECTED, DISCONNECTED }

    /** What happened to the scan with respect to the maintenance timeline. */
    enum class TimelineLogResult { SAVED, UNCHANGED, SKIPPED_VIN_MISMATCH }

    /** How the VIN read from the car compares with the one saved for this vehicle. */
    enum class VinCheck { MATCH, MISMATCH, VEHICLE_HAS_NO_VIN }

    data class UiState(
        val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
        val error: String? = null,
        val devices: List<ObdDevice> = emptyList(),
        val bleDevices: List<ObdDevice> = emptyList(),
        val isBleScanning: Boolean = false,
        val scanResult: ObdScanResult? = null,
        /** Human readable step shown while connecting/scanning, e.g. "Reading trouble codes…". */
        val progressMessage: String? = null,
        val timelineLogResult: TimelineLogResult? = null,
        val vinCheck: VinCheck? = null,
        /** The VIN currently saved on the vehicle, for display when it doesn't match. */
        val vehicleVin: String? = null,
        val isLiveDataActive: Boolean = false,
        val liveReadings: List<ObdReading> = emptyList(),
        /** True once the saved-adapter preference has been read from DataStore. */
        val isPreferenceLoaded: Boolean = false,
        val savedAdapter: ObdAdapterConfig? = null,
        val isDevicePickerOpen: Boolean = false,
        val isClearingCodes: Boolean = false,
    ) {
        val isConnecting: Boolean get() = connectionStatus == ConnectionStatus.CONNECTING
        val isConnected: Boolean get() = connectionStatus == ConnectionStatus.CONNECTED
        val hasSavedDevice: Boolean get() = savedAdapter != null
        val isBusy: Boolean get() = isConnecting || (progressMessage != null) || isClearingCodes
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _timelineEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits each time a record is written to the maintenance timeline (e.g. to refresh widgets). */
    val timelineEvents: SharedFlow<Unit> = _timelineEvents.asSharedFlow()

    private val preferenceLoaded = CompletableDeferred<Unit>()
    private var hasAttemptedAutoConnect = false
    private var vehicleId: Long = -1L
    private var session: ObdParser? = null
    private var scanJob: Job? = null
    private var liveJob: Job? = null
    private var bleScanJob: Job? = null

    /** The last scan that wasn't logged because of a VIN mismatch, kept so the user can log it anyway. */
    private var unloggedScan: ObdScanResult? = null

    init {
        viewModelScope.launch {
            preferencesRepository.savedObdAdapter.collect { adapter ->
                _uiState.update { it.copy(savedAdapter = adapter, isPreferenceLoaded = true) }
                preferenceLoaded.complete(Unit)
            }
        }
    }

    fun setVehicleId(vehicleId: Long) {
        this.vehicleId = vehicleId
    }

    fun loadDevices() {
        _uiState.update { it.copy(devices = obdConnectionManager.getPairedObdDevices()) }
    }

    /**
     * Called by the UI once the permissions needed for the saved adapter are available.
     * Refreshes the paired device list and, the first time, connects to the saved adapter.
     */
    fun onPermissionsGranted() {
        loadDevices()
        if (hasAttemptedAutoConnect) return
        hasAttemptedAutoConnect = true
        viewModelScope.launch {
            preferenceLoaded.await()
            val adapter = _uiState.value.savedAdapter
            if (adapter != null && _uiState.value.connectionStatus == ConnectionStatus.IDLE) {
                connectAndScan(adapter)
            }
        }
    }

    fun openDevicePicker() {
        loadDevices()
        _uiState.update { it.copy(isDevicePickerOpen = true) }
    }

    fun closeDevicePicker() {
        stopBleScan()
        _uiState.update { it.copy(isDevicePickerOpen = false) }
    }

    /** Scans for nearby Bluetooth LE adapters for [durationMs]. Requires BLUETOOTH_SCAN. */
    fun startBleScan(durationMs: Long = BLE_SCAN_DURATION_MS) {
        bleScanJob?.cancel()
        _uiState.update { it.copy(isBleScanning = true, bleDevices = emptyList()) }
        bleScanJob = viewModelScope.launch {
            try {
                withTimeoutOrNull(durationMs) {
                    obdConnectionManager.scanBleDevices().collect { device ->
                        _uiState.update { state ->
                            if (state.bleDevices.any { it.address == device.address }) {
                                state
                            } else {
                                state.copy(
                                    bleDevices = (state.bleDevices + device).sortedWith(
                                        compareByDescending<ObdDevice> { it.looksLikeObdAdapter }.thenBy { it.name.lowercase() },
                                    ),
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Bluetooth LE scan failed. Make sure Bluetooth is on.") }
            } finally {
                _uiState.update { it.copy(isBleScanning = false) }
            }
        }
    }

    fun stopBleScan() {
        bleScanJob?.cancel()
        bleScanJob = null
        _uiState.update { it.copy(isBleScanning = false) }
    }

    /** Persists [config] as the default adapter and connects to it immediately. */
    fun selectAdapter(config: ObdAdapterConfig) {
        stopBleScan()
        _uiState.update { it.copy(isDevicePickerOpen = false, savedAdapter = config) }
        viewModelScope.launch { preferencesRepository.setSavedObdAdapter(config) }
        connectAndScan(config)
    }

    /** Disconnects and forgets the default adapter. */
    fun clearSavedDevice() {
        disconnect()
        stopBleScan()
        _uiState.update { it.copy(isDevicePickerOpen = false, savedAdapter = null, error = null) }
        viewModelScope.launch { preferencesRepository.setSavedObdAdapter(null) }
    }

    /** Re-scans over the open connection, or reconnects first if the connection was lost. */
    fun rescan() {
        val adapter = _uiState.value.savedAdapter ?: return
        val existing = session
        if (existing != null && _uiState.value.isConnected) {
            stopLiveData()
            scanJob?.cancel()
            scanJob = viewModelScope.launch { runScan(existing) }
        } else {
            connectAndScan(adapter)
        }
    }

    fun connectAndScan(config: ObdAdapterConfig) {
        stopLiveData()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    error = null,
                    scanResult = null,
                    timelineLogResult = null,
                    vinCheck = null,
                    liveReadings = emptyList(),
                    progressMessage = "Connecting to adapter…",
                )
            }
            val newSession = openSession(config) { _uiState.update { s -> s.copy(progressMessage = it) } }
            if (newSession == null) return@launch
            runScan(newSession)
        }
    }

    /**
     * Connects the transport and initializes the ELM327. On failure, reports the error and
     * returns null.
     */
    private suspend fun openSession(config: ObdAdapterConfig, onProgress: (String) -> Unit): ObdParser? {
        closeConnection()
        val newSession = if (obdConnectionManager.connect(config)) obdConnectionManager.createSession() else null
        if (newSession == null) {
            failConnection(connectFailureMessage(config))
            return null
        }
        onProgress("Detecting vehicle protocol…")
        try {
            newSession.init()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failConnection(connectionErrorMessage(e))
            return null
        }
        session = newSession
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
        return newSession
    }

    private suspend fun runScan(parser: ObdParser) {
        var failures = 0
        var result = ObdScanResult(protocolName = parser.protocolName)

        /** Runs one request; a timeout or bad reply only skips that item. */
        suspend fun <T> step(label: String, block: suspend () -> T): T? {
            _uiState.update { it.copy(progressMessage = label) }
            return try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: ObdTimeoutException) {
                failures++
                null
            } catch (e: ObdAdapterException) {
                throw e
            } catch (e: IOException) {
                throw e
            } catch (_: Exception) {
                failures++
                null
            }
        }

        try {
            _uiState.update { it.copy(error = null, timelineLogResult = null, vinCheck = null) }
            unloggedScan = null
            step("Checking battery voltage…") { parser.readAdapterVoltage() }
                ?.let { result = result.copy(adapterVoltage = it) }
            step("Checking engine light & readiness…") { parser.readMonitorStatus() }
                ?.let { result = result.copy(monitorStatus = it) }

            val sources = mutableMapOf<String, List<String>>()
            step("Reading stored trouble codes…") { parser.readStoredDtcs() }
                ?.let { result = result.copy(storedDtcs = it.codes); sources.mergeFrom(it.sources) }
            step("Reading pending trouble codes…") { parser.readPendingDtcs() }
                ?.let { result = result.copy(pendingDtcs = it.codes); sources.mergeFrom(it.sources) }
            step("Reading permanent trouble codes…") { parser.readPermanentDtcs() }
                ?.let { result = result.copy(permanentDtcs = it.codes); sources.mergeFrom(it.sources) }
            result = result.copy(dtcSources = sources)
            // Publish codes as soon as we have them; the rest fills in progressively.
            _uiState.update { it.copy(scanResult = result) }

            if (result.storedDtcs.isNotEmpty()) {
                step("Reading freeze frame…") { parser.readFreezeFrame() }
                    ?.let { result = result.copy(freezeFrame = it) }
            }
            step("Reading odometer…") { parser.readOdometerKm() }
                ?.let { result = result.copy(odometerKm = it) }
            step("Reading distance since codes cleared…") { parser.readDistanceSinceClearedKm() }
                ?.let { result = result.copy(distanceSinceClearedKm = it) }
            step("Reading VIN…") { parser.readVin() }
                ?.let { result = result.copy(vin = it) }
            _uiState.update { it.copy(scanResult = result) }

            val readings = mutableListOf<ObdReading>()
            for (pid in ObdPid.SNAPSHOT) {
                if (!parser.isSupported(pid)) continue
                step("Reading ${pid.label.lowercase()}…") { parser.readPid(pid) }?.let(readings::add)
            }
            result = result.copy(readings = readings, failedRequests = failures)
            _uiState.update { it.copy(scanResult = result, progressMessage = null) }

            val vinCheck = checkVin(result.vin)
            if (vinCheck == VinCheck.MISMATCH) {
                unloggedScan = result
                _uiState.update { it.copy(timelineLogResult = TimelineLogResult.SKIPPED_VIN_MISMATCH) }
            } else {
                logScanToTimeline(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(scanResult = result.copy(failedRequests = failures)) }
            failConnection(connectionErrorMessage(e))
        }
    }

    private fun MutableMap<String, List<String>>.mergeFrom(other: Map<String, List<String>>) {
        other.forEach { (code, modules) -> this[code] = (this[code].orEmpty() + modules).distinct() }
    }

    /** Compares the VIN read from the car with the vehicle's saved VIN. */
    private suspend fun checkVin(vin: String?): VinCheck? {
        if (vin == null || vehicleId <= 0) return null
        val vehicleVin = runCatching { vehicleRepository.getVehicleByIdOnce(vehicleId)?.vin }.getOrNull()
            ?.trim()?.uppercase().orEmpty()
        val check = when {
            vehicleVin.isBlank() -> VinCheck.VEHICLE_HAS_NO_VIN
            vehicleVin == vin.uppercase() -> VinCheck.MATCH
            else -> VinCheck.MISMATCH
        }
        _uiState.update { it.copy(vinCheck = check, vehicleVin = vehicleVin.ifBlank { null }) }
        return check
    }

    /** Saves the VIN read from the car onto the vehicle (when the vehicle has none). */
    fun saveVinToVehicle() {
        val vin = _uiState.value.scanResult?.vin ?: return
        if (vehicleId <= 0) return
        viewModelScope.launch {
            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId) ?: return@launch
            vehicleRepository.saveVehicle(vehicle.copy(vin = vin))
            _uiState.update { it.copy(vinCheck = VinCheck.MATCH, vehicleVin = vin) }
        }
    }

    /** Logs a scan that was held back because the VIN didn't match this vehicle. */
    fun logScanAnyway() {
        val result = unloggedScan ?: return
        unloggedScan = null
        viewModelScope.launch { logScanToTimeline(result) }
    }

    /**
     * Records the scan in the maintenance timeline, skipping it when nothing changed since the
     * previous scan and the odometer hasn't advanced (so repeated rescans don't spam history).
     */
    private suspend fun logScanToTimeline(result: ObdScanResult) {
        if (vehicleId <= 0) return
        val description = ObdScanSummary.describe(result)
        val odometerMiles = result.odometerKm?.let(UnitConverter::kmToMiles)
        val latestMileage = maintenanceRepository.getLatestMileageForVehicle(vehicleId).first()
        val lastScan = maintenanceRepository.getRecordsForVehicle(vehicleId).first()
            .filter { it.taskName == ObdScanSummary.SCAN_TASK_NAME }
            .maxByOrNull { it.date }

        val odometerAdvanced = (odometerMiles != null) && (odometerMiles > (latestMileage ?: -1))
        val changed = (lastScan == null) || (lastScan.description != description)
        if (!odometerAdvanced && !changed) {
            _uiState.update { it.copy(timelineLogResult = TimelineLogResult.UNCHANGED) }
            return
        }

        saveTimelineRecord(
            mileage = odometerMiles ?: latestMileage ?: 0,
            description = description,
            taskName = ObdScanSummary.SCAN_TASK_NAME,
        )
        _uiState.update { it.copy(timelineLogResult = TimelineLogResult.SAVED) }
    }

    private suspend fun saveTimelineRecord(mileage: Int, description: String, taskName: String) {
        maintenanceRepository.saveRecord(
            MaintenanceRecord(
                vehicleId = vehicleId,
                date = System.currentTimeMillis(),
                mileage = mileage,
                description = description,
                cost = 0.0,
                category = MaintenanceCategory.INSPECTION,
                taskName = taskName,
            )
        )
        _timelineEvents.tryEmit(Unit)
    }

    fun clearCodes() {
        val parser = session ?: return
        stopLiveData()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val previous = _uiState.value.scanResult
            val cleared = (previous?.storedDtcs.orEmpty() + previous?.pendingDtcs.orEmpty()).distinct()
            _uiState.update { it.copy(isClearingCodes = true, error = null) }
            try {
                val acknowledged = parser.clearDtcs()
                _uiState.update { it.copy(isClearingCodes = false) }
                if (!acknowledged) {
                    _uiState.update {
                        it.copy(error = "The vehicle didn't confirm the clear. Turn the engine off (ignition on) and try again.")
                    }
                    return@launch
                }
                if (vehicleId > 0 && _uiState.value.vinCheck != VinCheck.MISMATCH) {
                    val latestMileage = maintenanceRepository.getLatestMileageForVehicle(vehicleId).first()
                    val mileage = previous?.odometerKm?.let(UnitConverter::kmToMiles) ?: latestMileage ?: 0
                    saveTimelineRecord(mileage, ObdScanSummary.describeClear(cleared), ObdScanSummary.CLEAR_TASK_NAME)
                }
                runScan(parser)
            } catch (e: CancellationException) {
                throw e
            } catch (_: ObdTimeoutException) {
                _uiState.update { it.copy(isClearingCodes = false, error = "The adapter didn't respond to the clear request.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isClearingCodes = false) }
                failConnection(connectionErrorMessage(e))
            }
        }
    }

    fun toggleLiveData() {
        if (_uiState.value.isLiveDataActive) stopLiveData() else startLiveData()
    }

    fun startLiveData() {
        val initialParser = session ?: return
        if (liveJob?.isActive == true || _uiState.value.isBusy) return
        if (ObdPid.LIVE.none(initialParser::isSupported)) {
            _uiState.update { it.copy(error = "This vehicle doesn't report any live data PIDs.") }
            return
        }
        _uiState.update { it.copy(isLiveDataActive = true, error = null) }
        liveJob = viewModelScope.launch {
            var parser = initialParser
            var reconnectAttempted = false
            while (isActive) {
                try {
                    pollLiveData(parser)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One silent reconnect attempt before giving up, so a brief dropout
                    // (e.g. adapter browning out during engine crank) doesn't end the session.
                    val adapter = _uiState.value.savedAdapter
                    if (reconnectAttempted || adapter == null) {
                        _uiState.update { it.copy(isLiveDataActive = false) }
                        failConnection(connectionErrorMessage(e))
                        return@launch
                    }
                    reconnectAttempted = true
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING, progressMessage = "Reconnecting…") }
                    val reopened = openSession(adapter) { message -> _uiState.update { it.copy(progressMessage = message) } }
                    if (reopened == null) {
                        _uiState.update { it.copy(isLiveDataActive = false) }
                        return@launch
                    }
                    _uiState.update { it.copy(progressMessage = null) }
                    parser = reopened
                }
            }
        }
    }

    /** Polls live PIDs until cancelled; throws when the adapter stops responding. */
    private suspend fun pollLiveData(parser: ObdParser) {
        val pids = ObdPid.LIVE.filter(parser::isSupported)
        var consecutiveTimeouts = 0
        while (true) {
            val readings = mutableListOf<ObdReading>()
            for (pid in pids) {
                try {
                    parser.readPid(pid)?.let(readings::add)
                    consecutiveTimeouts = 0
                } catch (e: ObdTimeoutException) {
                    if (++consecutiveTimeouts >= MAX_LIVE_TIMEOUTS) throw IOException("Adapter stopped responding", e)
                }
            }
            _uiState.update { it.copy(liveReadings = readings) }
            delay(LIVE_REFRESH_DELAY_MS)
        }
    }

    fun stopLiveData() {
        liveJob?.cancel()
        liveJob = null
        _uiState.update { it.copy(isLiveDataActive = false) }
    }

    fun disconnect() {
        stopLiveData()
        scanJob?.cancel()
        closeConnection()
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.IDLE,
                scanResult = null,
                progressMessage = null,
                timelineLogResult = null,
                vinCheck = null,
                liveReadings = emptyList(),
                isClearingCodes = false,
            )
        }
    }

    private fun failConnection(message: String) {
        stopLiveData()
        closeConnection()
        _uiState.update {
            it.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                progressMessage = null,
                isClearingCodes = false,
                error = message,
            )
        }
    }

    private fun closeConnection() {
        session = null
        obdConnectionManager.disconnect()
    }

    private fun connectFailureMessage(config: ObdAdapterConfig): String = when (config.type) {
        ObdAdapterType.WIFI ->
            "Couldn't reach the Wi-Fi adapter at ${config.address}. Make sure your phone is connected to the adapter's Wi-Fi network."
        ObdAdapterType.BLE ->
            "Couldn't connect to the Bluetooth LE adapter. Make sure it's plugged in, the ignition is on, and it isn't connected to another phone."
        ObdAdapterType.CLASSIC ->
            "Couldn't reach the adapter. Make sure Bluetooth is on, the adapter is plugged in, the ignition is on, and it's in range."
    }

    private fun connectionErrorMessage(e: Exception): String = when (e) {
        is ObdAdapterException -> e.message ?: "The adapter couldn't talk to the vehicle."
        is ObdTimeoutException -> "The adapter stopped responding. Check that it's powered and the ignition is on."
        is IOException -> "Lost connection to the adapter."
        else -> "Error communicating with the OBD adapter: ${e.message}"
    }

    override fun onCleared() {
        super.onCleared()
        bleScanJob?.cancel()
        liveJob?.cancel()
        scanJob?.cancel()
        closeConnection()
    }

    private companion object {
        const val LIVE_REFRESH_DELAY_MS = 250L
        const val MAX_LIVE_TIMEOUTS = 3
        const val BLE_SCAN_DURATION_MS = 12_000L
    }
}
