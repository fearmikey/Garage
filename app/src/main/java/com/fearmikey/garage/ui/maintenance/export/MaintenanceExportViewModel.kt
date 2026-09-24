package com.fearmikey.garage.ui.maintenance.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.ChargingRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.RecallLookupResult
import com.fearmikey.garage.data.repository.RecallRepository
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.notification.PdfExportNotifier
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val vehicle: Vehicle? = null,
    val specs: VehicleSpecs? = null,
    val parts: VehiclePartsInfo? = null,
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val modificationRecords: List<ModificationRecord> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val chargingRecords: List<ChargingRecord> = emptyList(),
    val recalls: List<VehicleRecall> = emptyList(),
    val openRecallCampaignNumbers: Set<String> = emptySet(),
    val isCheckingRecalls: Boolean = false,
    val currencySymbol: String = "$",
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val includeMods: Boolean = true,
    val includePartsSpecs: Boolean = true,
    val includeRecalls: Boolean = true,
    val includeFuel: Boolean = false,
    val vehiclePhotoFile: File? = null,
) {
    val openRecalls: List<VehicleRecall>
        get() = recalls.filter { it.campaignNumber in openRecallCampaignNumbers }

    val totalMaintenanceCost: Double
        get() = maintenanceRecords.sumOf { it.cost }

    val totalModificationCost: Double
        get() = if (includeMods) modificationRecords.sumOf { it.cost } else 0.0

    val totalFuelCost: Double
        get() = if (includeFuel) fuelRecords.sumOf { it.totalCost } + chargingRecords.sumOf { it.totalCost } else 0.0

    val totalCareInvestment: Double
        get() = totalMaintenanceCost + totalModificationCost
}

private data class VehicleDetails(
    val vehicle: Vehicle?,
    val specs: VehicleSpecs?,
    val parts: VehiclePartsInfo?,
)

private data class RecordDetails(
    val maintenance: List<MaintenanceRecord>,
    val mods: List<ModificationRecord>,
    val fuel: List<FuelRecord>,
    val charging: List<ChargingRecord>,
)

private data class OptionDetails(
    val currency: AppCurrency,
    val unitSystem: UnitSystem,
    val includeMods: Boolean,
    val includePartsSpecs: Boolean,
    val includeRecalls: Boolean,
    val includeFuel: Boolean,
)

@HiltViewModel
class MaintenanceExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    maintenanceRepository: MaintenanceRepository,
    modificationRepository: ModificationRepository,
    fuelRepository: FuelRepository,
    chargingRepository: ChargingRepository,
    private val recallRepository: RecallRepository,
    preferencesRepository: PreferencesRepository,
    private val imageStorageManager: ImageStorageManager,
    val pdfExportNotifier: PdfExportNotifier,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _includeMods = MutableStateFlow(true)
    private val _includePartsSpecs = MutableStateFlow(true)
    private val _includeRecalls = MutableStateFlow(true)
    private val _includeFuel = MutableStateFlow(false)

    private val _recallsState = MutableStateFlow<List<VehicleRecall>>(emptyList())
    private val _unaddressedCampaigns = MutableStateFlow<Set<String>>(emptySet())
    private val _isCheckingRecalls = MutableStateFlow(false)
    private var recallCheckAttempted = false

    val vehicle: StateFlow<Vehicle?> = vehicleRepository.getVehicleById(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records: StateFlow<List<MaintenanceRecord>> = maintenanceRepository.getRecordsForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currencySymbol: StateFlow<String> = preferencesRepository.appCurrency
        .map { it.symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "$")

    private val vehicleDetailsFlow = combine(
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
        vehicleRepository.getVehicleParts(vehicleId),
    ) { v, s, p ->
        VehicleDetails(v, s, p)
    }

    private val recordDetailsFlow = combine(
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        modificationRepository.getModsForVehicle(vehicleId),
        fuelRepository.getRecordsForVehicle(vehicleId),
        chargingRepository.getRecordsForVehicle(vehicleId),
    ) { m, mods, f, c ->
        RecordDetails(m, mods, f, c)
    }

    private val optionsFlow = combine(
        preferencesRepository.appCurrency,
        preferencesRepository.unitSystem,
        _includeMods,
        _includePartsSpecs,
        _includeRecalls,
        _includeFuel,
    ) { array ->
        OptionDetails(
            currency = array[0] as AppCurrency,
            unitSystem = array[1] as UnitSystem,
            includeMods = array[2] as Boolean,
            includePartsSpecs = array[3] as Boolean,
            includeRecalls = array[4] as Boolean,
            includeFuel = array[5] as Boolean,
        )
    }

    val uiState: StateFlow<ExportUiState> = combine(
        vehicleDetailsFlow,
        recordDetailsFlow,
        optionsFlow,
        _recallsState,
        _unaddressedCampaigns,
        _isCheckingRecalls,
    ) { array ->
        val vDetails = array[0] as VehicleDetails
        val rDetails = array[1] as RecordDetails
        val options = array[2] as OptionDetails
        @Suppress("UNCHECKED_CAST")
        val recalls = array[3] as List<VehicleRecall>
        @Suppress("UNCHECKED_CAST")
        val unaddressed = array[4] as Set<String>
        val isCheckingRecalls = array[5] as Boolean

        val v = vDetails.vehicle
        if ((v != null) && !recallCheckAttempted && (v.year != null) && v.make.isNotBlank() && v.model.isNotBlank()) {
            recallCheckAttempted = true
            fetchRecalls(v.year, v.make, v.model)
        }

        val photoFile = v?.imageUri?.let { imageStorageManager.imageFile(it) }?.takeIf { it.exists() }

        ExportUiState(
            vehicle = v,
            specs = vDetails.specs,
            parts = vDetails.parts,
            maintenanceRecords = rDetails.maintenance,
            modificationRecords = rDetails.mods,
            fuelRecords = rDetails.fuel,
            chargingRecords = rDetails.charging,
            recalls = recalls,
            openRecallCampaignNumbers = unaddressed,
            isCheckingRecalls = isCheckingRecalls,
            currencySymbol = options.currency.symbol,
            unitSystem = options.unitSystem,
            includeMods = options.includeMods,
            includePartsSpecs = options.includePartsSpecs,
            includeRecalls = options.includeRecalls,
            includeFuel = options.includeFuel,
            vehiclePhotoFile = photoFile,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState())

    fun toggleIncludeMods(enabled: Boolean) {
        _includeMods.value = enabled
    }

    fun toggleIncludePartsSpecs(enabled: Boolean) {
        _includePartsSpecs.value = enabled
    }

    fun toggleIncludeRecalls(enabled: Boolean) {
        _includeRecalls.value = enabled
    }

    fun toggleIncludeFuel(enabled: Boolean) {
        _includeFuel.value = enabled
    }

    fun toggleRecallCampaignOpen(campaignNumber: String, isOpen: Boolean) {
        _unaddressedCampaigns.value = if (isOpen) {
            _unaddressedCampaigns.value + campaignNumber
        } else {
            _unaddressedCampaigns.value - campaignNumber
        }
    }

    fun markAllRecallsResolved() {
        _unaddressedCampaigns.value = emptySet()
    }

    fun markAllRecallsOpen() {
        _unaddressedCampaigns.value = _recallsState.value.map { it.campaignNumber }.toSet()
    }

    private fun fetchRecalls(year: Int, make: String, model: String) {
        viewModelScope.launch {
            _isCheckingRecalls.value = true
            when (val result = recallRepository.getRecalls(year, make, model)) {
                is RecallLookupResult.Success -> {
                    _recallsState.value = result.recalls
                    _unaddressedCampaigns.value = emptySet()
                }
                is RecallLookupResult.Error -> {
                    _recallsState.value = emptyList()
                    _unaddressedCampaigns.value = emptySet()
                }
            }
            _isCheckingRecalls.value = false
        }
    }
}
