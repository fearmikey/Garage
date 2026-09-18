package com.fearmikey.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.fearmikey.garage.ui.vehicle.VehicleTab

/**
 * A vehicle detail navigation request triggered from outside normal in-app
 * navigation, currently only the home screen widget's "Log Service"/"Log
 * Fuel" buttons (see [MainActivity]).
 */
data class PendingDeepLink(val vehicleId: Long, val tab: Int, val openAdd: Boolean)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _dismissedBuyMeACoffeeForSession = MutableStateFlow(false)

    val isOnboardingCompleted: StateFlow<Boolean?> = preferencesRepository.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val themeType: StateFlow<String> = preferencesRepository.themeType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val showBuyMeACoffeePrompt: StateFlow<Boolean> = combine(
        preferencesRepository.appOpenCount,
        preferencesRepository.buyMeACoffeeDontAskAgain,
        preferencesRepository.buyMeACoffeeNextPromptOpenCount,
        preferencesRepository.onboardingCompleted,
        _dismissedBuyMeACoffeeForSession,
    ) { openCount, dontAskAgain, nextPromptOpenCount, onboardingCompleted, dismissedForSession ->
        openCount >= nextPromptOpenCount && !dontAskAgain && onboardingCompleted && !dismissedForSession
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    private val _pendingDeepLink = MutableStateFlow<PendingDeepLink?>(null)
    val pendingDeepLink: StateFlow<PendingDeepLink?> = _pendingDeepLink.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.incrementAppOpenCount()
        }
    }

    fun onBuyMeACoffeeClicked() {
        _dismissedBuyMeACoffeeForSession.value = true
        viewModelScope.launch {
            preferencesRepository.setBuyMeACoffeeDontAskAgain(true)
        }
    }

    fun onBuyMeACoffeeDontAskAgain() {
        _dismissedBuyMeACoffeeForSession.value = true
        viewModelScope.launch {
            preferencesRepository.setBuyMeACoffeeDontAskAgain(true)
        }
    }

    fun onBuyMeACoffeeMaybeLater() {
        _dismissedBuyMeACoffeeForSession.value = true
        viewModelScope.launch {
            val currentOpenCount = preferencesRepository.appOpenCount.first()
            preferencesRepository.setBuyMeACoffeeNextPromptOpenCount(currentOpenCount + 4)
        }
    }

    /**
     * Resolves a widget-launched [action]/[vehicleIdExtra] (see [MainActivity]'s
     * ACTION_LOG_SERVICE/ACTION_LOG_FUEL) into a navigable [PendingDeepLink]. When
     * no vehicle id was supplied, falls back to the first known vehicle; if there
     * are no vehicles at all, this is a no-op (the Dashboard opens as normal).
     */
    fun handleDeepLinkIntent(action: String?, vehicleIdExtra: Long) {
        val tab = when (action) {
            MainActivity.ACTION_LOG_SERVICE -> VehicleTab.TIMELINE.ordinal
            MainActivity.ACTION_LOG_FUEL -> VehicleTab.FUEL.ordinal
            else -> return
        }
        viewModelScope.launch {
            val vehicles = vehicleRepository.getAllVehicles().first()
            val defaultId = preferencesRepository.defaultVehicleId.first()
            val vehicleId = vehicleIdExtra.takeIf { it != MainActivity.NO_VEHICLE_ID_EXTRA && vehicles.any { v -> v.id == it } }
                ?: defaultId.takeIf { id -> vehicles.any { v -> v.id == id } }
                ?: vehicles.firstOrNull()?.id
                ?: return@launch
            _pendingDeepLink.value = PendingDeepLink(vehicleId, tab, openAdd = true)
        }
    }

    fun clearPendingDeepLink() {
        _pendingDeepLink.value = null
    }
}
