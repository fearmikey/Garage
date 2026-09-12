package com.fearmikey.garage.ui.startup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartupUiState(
    val selectedUnits: String = "metric",
    val selectedCurrency: String = "USD",
    val notificationPermissionGranted: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val termsAccepted: Boolean = false,
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupUiState())
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUnits = preferencesRepository.unitsType.first()
            val currentCurrency = preferencesRepository.currencyCode.first()
            val initialTermsAccepted = preferencesRepository.termsAccepted.first()
            _uiState.update {
                it.copy(
                    selectedUnits = currentUnits,
                    selectedCurrency = currentCurrency,
                    termsAccepted = initialTermsAccepted,
                )
            }
        }
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val hasNotificationPermission = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        val hasCameraPermission = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        _uiState.update {
            it.copy(
                notificationPermissionGranted = hasNotificationPermission,
                cameraPermissionGranted = hasCameraPermission,
            )
        }
    }

    fun selectUnits(units: String) {
        _uiState.update { it.copy(selectedUnits = units) }
    }

    fun selectCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun setTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun completeStartup(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setUnitsType(_uiState.value.selectedUnits)
            preferencesRepository.setCurrencyCode(_uiState.value.selectedCurrency)
            preferencesRepository.setTermsAccepted(_uiState.value.termsAccepted)
            preferencesRepository.setOnboardingCompleted(completed = true)
            onFinished()
        }
    }
}
