package com.fearmikey.garage.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerSyncStatus
import com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker
import com.fearmikey.garage.data.repository.AutoBackupManager
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.BackupResult
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.repository.WebDavBackupRepository
import com.fearmikey.garage.notification.CloudBackupScheduler
import com.fearmikey.garage.notification.ReminderNotifier
import com.fearmikey.garage.notification.WorkScheduler
import com.fearmikey.garage.obd.ObdConnectionManager
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdDevice
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SettingsUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val units: String = "metric",
    val currency: String = "USD",
    val theme: String = "system",
    val defaultVehicleId: Long? = null,
    val maintenanceMileageWindow: Int = 500,
    val maintenanceDaysWindow: Int = 10,
    val documentExpirationRemindersEnabled: Boolean = true,
    val documentExpirationDaysWindow: Int = 30,
    val affiliateLinksEnabled: Boolean = false,
    val showFuelTrendGraph: Boolean = true,
    val showFleetOverview: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    /** True once an import has completed; the UI should prompt the user to restart the app. */
    val importSucceeded: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPasswordSet: Boolean = false,
    val lubeLoggerConfigured: Boolean = false,
    val lubeLoggerServerUrl: String = "",
    val lubeLoggerUsername: String = "",
    val lubeLoggerApiKey: String = "",
    val lubeLoggerUnitSystem: String = "imperial",
    /** What actually happened the last time Garage talked to the LubeLogger server. */
    val lubeLoggerStatus: LubeLoggerSyncStatus = LubeLoggerSyncStatus.NeverSynced,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val lastSyncError: String? = null,
    val localBackupEnabled: Boolean = false,
    val localBackupFolderUri: String = "",
    val lastLocalBackupTimestamp: Long? = null,
    val lastLocalBackupError: String? = null,
    val notificationPermissionGranted: Boolean = false,
    val savedObdAdapter: ObdAdapterConfig? = null,
    val pairedObdDevices: List<ObdDevice> = emptyList(),
    val bleObdDevices: List<ObdDevice> = emptyList(),
    val isObdBleScanning: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val preferencesRepository: PreferencesRepository,
    private val vehicleRepository: VehicleRepository,
    private val webDavBackupRepository: WebDavBackupRepository,
    private val cloudBackupPreferencesManager: CloudBackupPreferencesManager,
    private val autoBackupManager: AutoBackupManager,
    private val reminderNotifier: ReminderNotifier,
    private val obdConnectionManager: ObdConnectionManager,
    private val lubeLoggerCredentialsManager: com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { 
            it.copy(
                lubeLoggerConfigured = lubeLoggerCredentialsManager.isConfigured(),
                lubeLoggerServerUrl = lubeLoggerCredentialsManager.getServerUrl() ?: "",
                lubeLoggerUsername = lubeLoggerCredentialsManager.getUsername() ?: "",
                lubeLoggerApiKey = lubeLoggerCredentialsManager.getApiKey() ?: "",
                lubeLoggerUnitSystem = lubeLoggerCredentialsManager.getUnitSystem(),
            ) 
        }
        viewModelScope.launch {
            lubeLoggerCredentialsManager.syncStatus().collect { status ->
                _uiState.update { it.copy(lubeLoggerStatus = status) }
                // Confirm the outcome of a sync the user started from Settings.
                if (awaitingManualLubeLoggerSync) {
                    when (status) {
                        is LubeLoggerSyncStatus.Success -> {
                            awaitingManualLubeLoggerSync = false
                            showMessage("LubeLogger sync completed.")
                        }
                        is LubeLoggerSyncStatus.Failed -> {
                            awaitingManualLubeLoggerSync = false
                            showMessage("LubeLogger sync failed: ${status.message}")
                        }
                        else -> Unit
                    }
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.unitsType.collect { units ->
                _uiState.update { it.copy(units = units) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.currencyCode.collect { currency ->
                _uiState.update { it.copy(currency = currency) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.themeType.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.defaultVehicleId.collect { id ->
                _uiState.update { it.copy(defaultVehicleId = id) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.maintenanceMileageWindow.collect { miles ->
                _uiState.update { it.copy(maintenanceMileageWindow = miles) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.maintenanceDaysWindow.collect { days ->
                _uiState.update { it.copy(maintenanceDaysWindow = days) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.affiliateLinksEnabled.collect { enabled ->
                _uiState.update { it.copy(affiliateLinksEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.showFuelTrendGraph.collect { enabled ->
                _uiState.update { it.copy(showFuelTrendGraph = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.showFleetOverview.collect { enabled ->
                _uiState.update { it.copy(showFleetOverview = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.documentExpirationRemindersEnabled.collect { enabled ->
                _uiState.update { it.copy(documentExpirationRemindersEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.documentExpirationDaysWindow.collect { days ->
                _uiState.update { it.copy(documentExpirationDaysWindow = days) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.savedObdAdapter.collect { adapter ->
                _uiState.update { it.copy(savedObdAdapter = adapter) }
            }
        }
        viewModelScope.launch {
            vehicleRepository.getAllVehicles().collect { vehicles ->
                _uiState.update { it.copy(vehicles = vehicles) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.cloudSyncEnabled.collect { enabled ->
                _uiState.update { it.copy(cloudSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.webdavUrl.collect { url ->
                _uiState.update { it.copy(webdavUrl = url) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.webdavUsername.collect { username ->
                _uiState.update { it.copy(webdavUsername = username) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.lastSyncTimestamp.collect { timestamp ->
                _uiState.update { it.copy(lastSyncTimestamp = timestamp) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.lastSyncError.collect { error ->
                _uiState.update { it.copy(lastSyncError = error) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.localBackupEnabled.collect { enabled ->
                _uiState.update { it.copy(localBackupEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.localBackupFolderUri.collect { uri ->
                _uiState.update { it.copy(localBackupFolderUri = uri) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.lastLocalBackupTimestamp.collect { timestamp ->
                _uiState.update { it.copy(lastLocalBackupTimestamp = timestamp) }
            }
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.lastLocalBackupError.collect { error ->
                _uiState.update { it.copy(lastLocalBackupError = error) }
            }
        }
        _uiState.update { it.copy(webdavPasswordSet = !cloudBackupPreferencesManager.getWebdavPassword().isNullOrBlank()) }
        refreshNotificationPermissionState()
    }

    fun refreshNotificationPermissionState() {
        val granted = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun sendTestNotification() {
        refreshNotificationPermissionState()
        val sent = reminderNotifier.notifyTest()
        _uiState.update {
            it.copy(
                message = if (sent) {
                    "Test notification sent."
                } else {
                    "Notifications are disabled for Garage. Enable them to receive test notifications and reminders."
                },
            )
        }
    }

    fun setUnits(units: String) {
        viewModelScope.launch {
            preferencesRepository.setUnitsType(units)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrencyCode(currency)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeType(theme)
        }
    }

    fun setDefaultVehicleId(vehicleId: Long?) {
        viewModelScope.launch {
            preferencesRepository.setDefaultVehicleId(vehicleId)
            WidgetRefresher.refresh(context)
        }
    }

    fun setMaintenanceMileageWindow(miles: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaintenanceMileageWindow(miles)
            WorkScheduler.triggerImmediateReminderCheck(context)
        }
    }

    fun setMaintenanceDaysWindow(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaintenanceDaysWindow(days)
            WorkScheduler.triggerImmediateReminderCheck(context)
        }
    }

    fun setDocumentExpirationRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDocumentExpirationRemindersEnabled(enabled)
            WorkScheduler.triggerImmediateReminderCheck(context)
        }
    }

    fun setDocumentExpirationDaysWindow(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setDocumentExpirationDaysWindow(days)
            WorkScheduler.triggerImmediateReminderCheck(context)
        }
    }

    fun setAffiliateLinksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAffiliateLinksEnabled(enabled)
        }
    }

    fun setShowFuelTrendGraph(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowFuelTrendGraph(enabled)
        }
    }

    fun setShowFleetOverview(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowFleetOverview(enabled)
        }
    }

    /** Refreshes the list of paired Bluetooth devices. Requires BLUETOOTH_CONNECT. */
    fun loadPairedObdDevices() {
        _uiState.update { it.copy(pairedObdDevices = obdConnectionManager.getPairedObdDevices()) }
    }

    fun setSavedObdAdapter(config: ObdAdapterConfig) {
        stopObdBleScan()
        viewModelScope.launch {
            preferencesRepository.setSavedObdAdapter(config)
        }
    }

    fun clearSavedObdAdapter() {
        stopObdBleScan()
        viewModelScope.launch {
            preferencesRepository.setSavedObdAdapter(null)
        }
    }

    private var obdBleScanJob: Job? = null

    /** Scans for nearby Bluetooth LE OBD2 adapters. Requires BLUETOOTH_SCAN. */
    fun startObdBleScan() {
        obdBleScanJob?.cancel()
        _uiState.update { it.copy(isObdBleScanning = true, bleObdDevices = emptyList()) }
        obdBleScanJob = viewModelScope.launch {
            try {
                withTimeoutOrNull(OBD_BLE_SCAN_DURATION_MS) {
                    obdConnectionManager.scanBleDevices().collect { device ->
                        _uiState.update { state ->
                            if (state.bleObdDevices.any { it.address == device.address }) {
                                state
                            } else {
                                state.copy(
                                    bleObdDevices = (state.bleObdDevices + device).sortedWith(
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
                _uiState.update { it.copy(message = "Bluetooth LE scan failed. Make sure Bluetooth is on.") }
            } finally {
                _uiState.update { it.copy(isObdBleScanning = false) }
            }
        }
    }

    fun stopObdBleScan() {
        obdBleScanJob?.cancel()
        obdBleScanJob = null
        _uiState.update { it.copy(isObdBleScanning = false) }
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = backupRepository.exportBackup(destination)) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Backup exported successfully.")
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, message = "Export failed: ${result.message}")
                }
            }
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = backupRepository.importBackup(source)) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isBusy = false, importSucceeded = true)
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, message = "Import failed: ${result.message}")
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = backupRepository.clearAllData()) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "All data cleared successfully.")
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, message = "Failed to clear data: ${result.message}")
                }
            }
        }
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            cloudBackupPreferencesManager.setCloudSyncEnabled(enabled)
            CloudBackupScheduler.scheduleOrCancel(context, enabled)
        }
    }

    fun setLocalBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            cloudBackupPreferencesManager.setLocalBackupEnabled(enabled)
            if (enabled) {
                autoBackupManager.performAutoBackup()
            }
        }
    }

    fun setLocalBackupFolderUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Ignored if taking persistable permission is not supported
        }
        viewModelScope.launch {
            cloudBackupPreferencesManager.setLocalBackupFolderUri(uri.toString())
            cloudBackupPreferencesManager.setLocalBackupEnabled(true)
            autoBackupManager.performAutoBackup()
        }
    }

    fun triggerLocalBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = autoBackupManager.performAutoBackup()) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Local backup saved successfully.")
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, message = "Local backup failed: ${result.message}")
                }
            }
        }
    }

    fun setLubeLoggerCredentials(url: String, username: String, password: String?, apiKey: String?, unitSystem: String = "imperial") {
        lubeLoggerCredentialsManager.saveCredentials(url, username, password, apiKey, unitSystem)
        _uiState.update { 
            it.copy(
                lubeLoggerConfigured = lubeLoggerCredentialsManager.isConfigured(),
                lubeLoggerServerUrl = url,
                lubeLoggerUsername = username,
                lubeLoggerApiKey = apiKey ?: "",
                lubeLoggerUnitSystem = unitSystem,
            ) 
        }
        
        // Enqueue an immediate sync if they just configured it
        if (lubeLoggerCredentialsManager.isConfigured()) {
            syncLubeLoggerNow()
        }
    }

    private var awaitingManualLubeLoggerSync = false

    fun syncLubeLoggerNow() {
        if (_uiState.value.lubeLoggerStatus is LubeLoggerSyncStatus.Syncing) return
        awaitingManualLubeLoggerSync = true
        // Show progress right away; the worker confirms success or failure when it finishes.
        lubeLoggerCredentialsManager.markSyncStarted()
        val request = androidx.work.OneTimeWorkRequestBuilder<LubeLoggerSyncWorker>()
            .addTag(LubeLoggerSyncWorker.TAG_MANUAL)
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            LubeLoggerSyncWorker.MANUAL_WORK_NAME,
            androidx.work.ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun setWebdavCredentials(url: String, username: String, password: String) {
        viewModelScope.launch {
            cloudBackupPreferencesManager.setWebdavUrl(url)
            cloudBackupPreferencesManager.setWebdavUsername(username)
            if (password.isNotEmpty()) {
                cloudBackupPreferencesManager.setWebdavPassword(password)
            }
            _uiState.update {
                it.copy(webdavPasswordSet = !cloudBackupPreferencesManager.getWebdavPassword().isNullOrBlank())
            }
            CloudBackupScheduler.scheduleOrCancel(context, _uiState.value.cloudSyncEnabled)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = null) }
            when (val result = webDavBackupRepository.syncNow()) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isSyncing = false, message = "Cloud sync completed successfully.")
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isSyncing = false, message = "Cloud sync failed: ${result.message}")
                }
            }
        }
    }

    fun testConnection(url: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = webDavBackupRepository.testConnection(url, username, password)) {
                BackupResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Connection successful.")
                }
                is BackupResult.Failure -> _uiState.update {
                    it.copy(isBusy = false, message = "Connection failed: ${result.message}")
                }
            }
        }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val OBD_BLE_SCAN_DURATION_MS = 12_000L
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
