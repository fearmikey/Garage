package com.fearmikey.garage.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.BackupResult
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
    /** True once an import has completed; the UI should prompt the user to restart the app. */
    val importSucceeded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
