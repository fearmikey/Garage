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
import com.fearmikey.garage.data.repository.AutoBackupManager
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.BackupResult
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.repository.WebDavBackupRepository
import com.fearmikey.garage.notification.CloudBackupScheduler
import com.fearmikey.garage.notification.ReminderNotifier
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val units: String = "metric",
    val currency: String = "USD",
    val theme: String = "system",
    val defaultVehicleId: Long? = null,
    val vehicles: List<Vehicle> = emptyList(),
    /** True once an import has completed; the UI should prompt the user to restart the app. */
    val importSucceeded: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPasswordSet: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val lastSyncError: String? = null,
    val localBackupEnabled: Boolean = false,
    val localBackupFolderUri: String = "",
    val lastLocalBackupTimestamp: Long? = null,
    val lastLocalBackupError: String? = null,
    val notificationPermissionGranted: Boolean = false,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
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

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
