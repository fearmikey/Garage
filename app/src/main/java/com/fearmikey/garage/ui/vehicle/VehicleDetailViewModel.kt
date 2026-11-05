package com.fearmikey.garage.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.CustomDocumentRepository
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    imageStorageManager: ImageStorageManager,
    private val preferencesRepository: PreferencesRepository,
    maintenanceRepository: MaintenanceRepository,
    customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    customDocumentRepository: CustomDocumentRepository? = null,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    /** Initial tab/openAdd come from a deep link (e.g. the home screen widget's "Log
     * Service"/"Log Fuel" buttons); see [Destinations.vehicleDetailRoute]. */
    val initialTab: Int = savedStateHandle[Destinations.VEHICLE_DETAIL_TAB_ARG] ?: 0

    private val _shouldOpenAddSheet = MutableStateFlow(
        savedStateHandle.get<Boolean>(Destinations.VEHICLE_DETAIL_OPEN_ADD_ARG) ?: false
    )
    val shouldOpenAddSheet: StateFlow<Boolean> = _shouldOpenAddSheet.asStateFlow()

    fun consumeAddSheet() {
        _shouldOpenAddSheet.value = false
        savedStateHandle[Destinations.VEHICLE_DETAIL_OPEN_ADD_ARG] = false
    }

    val vehicle: StateFlow<Vehicle?> = vehicleRepository.getVehicleById(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tabs the user has hidden for this vehicle. */
    val hiddenTabs: StateFlow<Set<VehicleTab>> = vehicle
        .map { VehicleTab.parseHidden(it?.hiddenTabs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** User's custom tab order for this vehicle. */
    val tabOrder: StateFlow<List<VehicleTab>> = preferencesRepository.getTabOrder(vehicleId)
        .map { VehicleTab.parseOrder(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleTab.entriesList)

    /** Persists hidden tabs and custom tab order for this vehicle. */
    fun setTabCustomization(orderedTabs: List<VehicleTab>, hidden: Set<VehicleTab>) {
        viewModelScope.launch {
            val current = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val serializedHidden = VehicleTab.serializeHidden(hidden)
            if (current != null && current.hiddenTabs != serializedHidden) {
                vehicleRepository.saveVehicle(current.copy(hiddenTabs = serializedHidden))
            }
            preferencesRepository.setTabOrder(vehicleId, VehicleTab.serializeOrder(orderedTabs))
        }
    }

    private val maintenanceSuggestions = combine(
        combine(
            vehicle,
            vehicleRepository.getVehicleSpecs(vehicleId),
            maintenanceRepository.getRecordsForVehicle(vehicleId),
            maintenanceRepository.getLatestMileageForVehicle(vehicleId),
        ) { v, specs, records, mileage -> SuggestionInputs(v, v?.isPureEv(specs) == true, records, mileage) },
        customMaintenanceRuleRepository.getRulesForVehicle(vehicleId),
        maintenanceRepository.getIgnoredRulesForVehicle(vehicleId),
        preferencesRepository.maintenanceMileageWindow,
        preferencesRepository.maintenanceDaysWindow,
    ) { inputs, customRules, ignoredRules, mileageWindow, daysWindow ->
        val v = inputs.vehicle ?: return@combine emptyList<MaintenanceSuggestion>()
        MaintenanceScheduleEngine.suggestionsFor(
            vehicle = v,
            isPureEv = inputs.isPureEv,
            latestMileage = inputs.latestMileage,
            records = inputs.records,
            customRules = customRules.map { it.toMaintenanceRule() },
            ignoredTaskNames = ignoredRules.map { it.taskName }.toSet(),
            upcomingWindowMiles = mileageWindow,
            upcomingWindowDays = daysWindow,
        )
    }

    /** Per-tab attention indicators (maintenance due soon/overdue, documents expiring/expired). */
    val tabAlerts: StateFlow<Map<VehicleTab, ReminderStatus>> = combine(
        maintenanceSuggestions,
        vehicleRepository.getRegistrationInsurance(vehicleId),
        customDocumentRepository?.getDocumentsForVehicle(vehicleId) ?: flowOf(emptyList()),
        preferencesRepository.documentExpirationDaysWindow,
    ) { suggestions, registration, customDocuments, docDaysWindow ->
        VehicleTabAlerts.compute(suggestions, registration, docDaysWindow, customDocuments = customDocuments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private data class SuggestionInputs(
        val vehicle: Vehicle?,
        val isPureEv: Boolean,
        val records: List<com.fearmikey.garage.data.local.entity.MaintenanceRecord>,
        val latestMileage: Int?,
    )

    val vehicleThemingEnabled: StateFlow<Boolean> = preferencesRepository.vehicleThemingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val themeType: StateFlow<String> = preferencesRepository.themeType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    val amoledDark: StateFlow<Boolean> = preferencesRepository.amoledDark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Backs the shared-element vehicle photo header in [VehicleDetailScreen]. */
    val imageFile: StateFlow<File?> = vehicle
        .map { it?.imageUri?.let { uri -> imageStorageManager.imageFile(uri) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** All available image files and their vertical offsets for swipeable photo viewing. */
    val imageFiles: StateFlow<List<Pair<File, Float>>> = vehicle
        .map { v ->
            v?.photos?.mapNotNull { photo ->
                val file = imageStorageManager.imageFile(photo.uri)
                if (file.exists()) Pair(file, photo.offsetY) else null
            } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
