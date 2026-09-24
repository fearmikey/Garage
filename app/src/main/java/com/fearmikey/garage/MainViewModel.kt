package com.fearmikey.garage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.vehicle.VehicleTab
import com.fearmikey.garage.widget.WidgetRefresher
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

/**
 * A vehicle detail navigation request triggered from outside normal in-app
 * navigation, currently only the home screen widget's "Log Service"/"Log
 * Fuel" buttons (see [MainActivity]).
 */
data class PendingDeepLink(
    val vehicleId: Long,
    val tab: Int,
    val openAdd: Boolean = false,
    val openDriversLicenseTab: Boolean = false,
    val openOdometerDialog: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val _dismissedBuyMeACoffeeForSession = MutableStateFlow(value = false)

    val allVehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UnitSystem.METRIC,
    )

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
        (openCount >= nextPromptOpenCount) && !dontAskAgain && onboardingCompleted && !dismissedForSession
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

    fun getLatestMileageForVehicle(vehicleId: Long): StateFlow<Int?> =
        maintenanceRepository.getLatestMileageForVehicle(vehicleId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

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
     * ACTION_LOG_SERVICE/ACTION_LOG_FUEL/ACTION_UPDATE_ODOMETER) into a navigable [PendingDeepLink].
     */
    fun handleDeepLinkIntent(action: String?, vehicleIdExtra: Long) {
        if (action == MainActivity.ACTION_OPEN_DRIVERS_LICENSE) {
            _pendingDeepLink.value = PendingDeepLink(
                vehicleId = MainActivity.NO_VEHICLE_ID_EXTRA,
                tab = 0,
                openAdd = false,
                openDriversLicenseTab = true,
            )
            return
        }

        if (action == MainActivity.ACTION_UPDATE_ODOMETER) {
            viewModelScope.launch {
                val vehicles = vehicleRepository.getAllVehicles().first()
                val defaultId = preferencesRepository.defaultVehicleId.first()
                val vehicleId = vehicleIdExtra.takeIf { (it != MainActivity.NO_VEHICLE_ID_EXTRA) && vehicles.any { v -> v.id == it } }
                    ?: defaultId.takeIf { id -> vehicles.any { v -> v.id == id } }
                    ?: vehicles.firstOrNull()?.id
                    ?: MainActivity.NO_VEHICLE_ID_EXTRA
                _pendingDeepLink.value = PendingDeepLink(
                    vehicleId = vehicleId,
                    tab = VehicleTab.TIMELINE.ordinal,
                    openAdd = false,
                    openOdometerDialog = true,
                )
            }
            return
        }

        val (tab, openAdd) = when (action) {
            MainActivity.ACTION_LOG_SERVICE -> VehicleTab.TIMELINE.ordinal to true
            MainActivity.ACTION_LOG_FUEL -> VehicleTab.FUEL.ordinal to true
            MainActivity.ACTION_OPEN_REMINDERS -> VehicleTab.SCHEDULE.ordinal to false
            MainActivity.ACTION_OPEN_DOCUMENTS -> VehicleTab.DOCUMENTS.ordinal to false
            else -> return
        }
        viewModelScope.launch {
            val vehicles = vehicleRepository.getAllVehicles().first()
            val defaultId = preferencesRepository.defaultVehicleId.first()
            val vehicleId = vehicleIdExtra.takeIf { (it != MainActivity.NO_VEHICLE_ID_EXTRA) && vehicles.any { v -> v.id == it } }
                ?: defaultId.takeIf { id -> vehicles.any { v -> v.id == id } }
                ?: vehicles.firstOrNull()?.id
                ?: return@launch
            _pendingDeepLink.value = PendingDeepLink(vehicleId, tab, openAdd = openAdd)
        }
    }

    fun insertOdometerRecord(
        vehicleId: Long,
        canonicalMileage: Int,
        date: Long,
        notes: String,
        context: Context,
    ) {
        viewModelScope.launch {
            maintenanceRepository.saveRecord(
                MaintenanceRecord(
                    vehicleId = vehicleId,
                    date = date,
                    mileage = canonicalMileage,
                    description = notes.ifBlank { "Odometer check-in" },
                    cost = 0.0,
                    category = MaintenanceCategory.INSPECTION,
                    taskName = "Odometer Check-in",
                )
            )
            WidgetRefresher.refresh(context)
            clearPendingDeepLink()
        }
    }

    fun clearPendingDeepLink() {
        _pendingDeepLink.value = null
    }
}
