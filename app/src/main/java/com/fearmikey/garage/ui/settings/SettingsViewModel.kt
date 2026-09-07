package com.fearmikey.garage.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.BackupResult
import com.fearmikey.garage.data.repository.PreferencesRepository
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
    val theme: String = "system",
    /** True once an import has completed; the UI should prompt the user to restart the app. */
    val importSucceeded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val preferencesRepository: PreferencesRepository,
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
            preferencesRepository.themeType.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
    }

    fun setUnits(units: String) {
        viewModelScope.launch {
            preferencesRepository.setUnitsType(units)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeType(theme)
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

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
