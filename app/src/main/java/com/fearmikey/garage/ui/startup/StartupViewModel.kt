package com.fearmikey.garage.ui.startup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.obd.hasObdBluetoothPermissions
import com.fearmikey.garage.ui.vehicle.VehicleTab
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The ordered pages of the guided onboarding flow. */
enum class OnboardingStep {
    WELCOME,
    REGION,
    APPEARANCE,
    SCREENS,
    PERMISSIONS,
    FINISH,
    ;

    val isFirst: Boolean get() = ordinal == 0
    val isLast: Boolean get() = ordinal == entries.lastIndex
}

/** Tabs kept visible by the "Essentials only" preset on the onboarding Screens step. */
val ESSENTIAL_VEHICLE_TABS: Set<VehicleTab> = setOf(
    VehicleTab.TIMELINE,
    VehicleTab.SCHEDULE,
    VehicleTab.FUEL,
    VehicleTab.EXPENSES,
    VehicleTab.DOCUMENTS,
)

data class StartupUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val selectedUnits: String = "metric",
    val selectedCurrency: String = "USD",
    val selectedTheme: String = "system",
    val selectedDynamicColor: Boolean = true,
    val selectedPalette: String = "default",
    val selectedAmoledDark: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val bluetoothPermissionGranted: Boolean = false,
    val affiliateLinksEnabled: Boolean = false,
    val showFuelTrendGraph: Boolean = true,
    val showFleetOverview: Boolean = false,
    /** Vehicle Detail tabs hidden by default on newly added vehicles. */
    val defaultHiddenTabs: Set<VehicleTab> = emptySet(),
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
            val currentTheme = preferencesRepository.themeType.first()
            val currentDynamic = preferencesRepository.dynamicColor.first()
            val currentPalette = preferencesRepository.appPalette.first()
            val currentAmoled = preferencesRepository.amoledDark.first()
            val affiliateLinks = preferencesRepository.affiliateLinksEnabled.first()
            val fuelTrendGraph = preferencesRepository.showFuelTrendGraph.first()
            val fleetOverview = preferencesRepository.showFleetOverview.first()
            val hiddenTabs = VehicleTab.parseHidden(preferencesRepository.defaultHiddenTabs.first())
            _uiState.update {
                it.copy(
                    selectedUnits = currentUnits,
                    selectedCurrency = currentCurrency,
                    selectedTheme = currentTheme,
                    selectedDynamicColor = currentDynamic,
                    selectedPalette = currentPalette,
                    selectedAmoledDark = currentAmoled,
                    affiliateLinksEnabled = affiliateLinks,
                    showFuelTrendGraph = fuelTrendGraph,
                    showFleetOverview = fleetOverview,
                    defaultHiddenTabs = hiddenTabs,
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

        val hasLocationPermission = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        val hasBluetoothPermission = try {
            hasObdBluetoothPermissions(context)
        } catch (_: Throwable) {
            false
        }

        _uiState.update {
            it.copy(
                notificationPermissionGranted = hasNotificationPermission,
                cameraPermissionGranted = hasCameraPermission,
                locationPermissionGranted = hasLocationPermission,
                bluetoothPermissionGranted = hasBluetoothPermission,
            )
        }
    }

    // region Step navigation

    fun nextStep() {
        _uiState.update { state ->
            val next = OnboardingStep.entries.getOrNull(state.currentStep.ordinal + 1) ?: state.currentStep
            state.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val previous = OnboardingStep.entries.getOrNull(state.currentStep.ordinal - 1) ?: state.currentStep
            state.copy(currentStep = previous)
        }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    /** Jumps straight to the final page, keeping current (default) choices for the skipped steps. */
    fun skipToFinish() = goToStep(OnboardingStep.FINISH)

    // endregion

    fun selectUnits(units: String) {
        _uiState.update { it.copy(selectedUnits = units) }
    }

    fun selectCurrency(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun selectTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
        viewModelScope.launch { preferencesRepository.setThemeType(theme) }
    }

    fun selectPalette(palette: String) {
        _uiState.update { it.copy(selectedPalette = palette) }
        viewModelScope.launch { preferencesRepository.setAppPalette(palette) }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(selectedDynamicColor = enabled) }
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun toggleAmoledDark(enabled: Boolean) {
        _uiState.update { it.copy(selectedAmoledDark = enabled) }
        viewModelScope.launch { preferencesRepository.setAmoledDark(enabled) }
    }

    fun toggleAffiliateLinks(enabled: Boolean) {
        _uiState.update { it.copy(affiliateLinksEnabled = enabled) }
    }

    fun toggleFuelTrendGraph(enabled: Boolean) {
        _uiState.update { it.copy(showFuelTrendGraph = enabled) }
    }

    fun toggleFleetOverview(enabled: Boolean) {
        _uiState.update { it.copy(showFleetOverview = enabled) }
    }

    /** Shows or hides [tab] by default for new vehicles. Tabs that can't be hidden are ignored. */
    fun setTabVisible(tab: VehicleTab, visible: Boolean) {
        if (!tab.canHide) return
        _uiState.update { state ->
            val hidden = if (visible) state.defaultHiddenTabs - tab else state.defaultHiddenTabs + tab
            state.copy(defaultHiddenTabs = hidden)
        }
    }

    /** Replaces the whole default-hidden set (used by the "Show all" / "Essentials only" presets). */
    fun setDefaultHiddenTabs(hidden: Set<VehicleTab>) {
        _uiState.update { it.copy(defaultHiddenTabs = hidden.filter { tab -> tab.canHide }.toSet()) }
    }

    fun completeStartup(onFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            preferencesRepository.setUnitsType(state.selectedUnits)
            preferencesRepository.setCurrencyCode(state.selectedCurrency)
            preferencesRepository.setThemeType(state.selectedTheme)
            preferencesRepository.setDynamicColor(state.selectedDynamicColor)
            preferencesRepository.setAppPalette(state.selectedPalette)
            preferencesRepository.setAmoledDark(state.selectedAmoledDark)
            preferencesRepository.setAffiliateLinksEnabled(state.affiliateLinksEnabled)
            preferencesRepository.setShowFuelTrendGraph(state.showFuelTrendGraph)
            preferencesRepository.setShowFleetOverview(state.showFleetOverview)
            preferencesRepository.setDefaultHiddenTabs(VehicleTab.serializeHidden(state.defaultHiddenTabs))
            preferencesRepository.setOnboardingCompleted(completed = true)
            onFinished()
        }
    }
}
