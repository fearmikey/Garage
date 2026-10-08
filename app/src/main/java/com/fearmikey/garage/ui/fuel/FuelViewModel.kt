package com.fearmikey.garage.ui.fuel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.fuel.BatteryHealthSummary
import com.fearmikey.garage.data.fuel.ChargingCalculator
import com.fearmikey.garage.data.fuel.ChargingEfficiencyEntry
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.ChargingRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.notification.WorkScheduler
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelUiState(
    val records: List<FuelRecord> = emptyList(),
    /** Segment MPG for the fill-up that *completed* it, keyed by [FuelRecord.id]. */
    val mpgByRecordId: Map<Long, Double> = emptyMap(),
    /** Full-tank economy segments, oldest first; drives the trend chart. */
    val fuelEntries: List<FuelEconomyEntry> = emptyList(),
    val averageMpg: Double? = null,
    val bestMpg: Double? = null,
    val worstMpg: Double? = null,
    val totalSpent: Double = 0.0,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val currencySymbol: String = "$",
    // EV & PHEV Support fields
    val isEvOrPhev: Boolean = false,
    val isPureEv: Boolean = false,
    val chargingRecords: List<ChargingRecord> = emptyList(),
    /** Charging efficiency segments, oldest first; drives the charging trend chart. */
    val chargingEntries: List<ChargingEfficiencyEntry> = emptyList(),
    val averageWhPerMi: Double? = null,
    val averageKwhPer100Km: Double? = null,
    val averageMpge: Double? = null,
    val totalChargingSpent: Double = 0.0,
    val batteryHealth: BatteryHealthSummary = BatteryHealthSummary(),
)

@HiltViewModel
class FuelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fuelRepository: FuelRepository,
    private val chargingRepository: ChargingRepository,
    vehicleRepository: VehicleRepository,
    preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<FuelUiState> = combine(
        fuelRepository.getRecordsForVehicle(vehicleId),
        chargingRepository.getRecordsForVehicle(vehicleId),
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
        preferencesRepository.unitSystem,
        preferencesRepository.appCurrency,
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val fuelRecords = array[0] as List<FuelRecord>
        @Suppress("UNCHECKED_CAST")
        val chargingRecords = array[1] as List<ChargingRecord>
        val vehicle = array[2] as Vehicle?
        val specs = array[3] as VehicleSpecs?
        val unitSystem = array[4] as UnitSystem
        val currency = array[5] as AppCurrency

        val isEvOrPhev = (specs?.isEvOrPhev() == true) || isVehicleEvByDetails(vehicle, specs)
        val isPureEv = vehicle?.isPureEv(specs) == true

        val fuelEntries = FuelEconomyCalculator.entriesFor(fuelRecords)
        val chargingEntries = ChargingCalculator.entriesFor(chargingRecords)
        val batteryHealth = ChargingCalculator.computeBatteryHealth(chargingRecords)

        FuelUiState(
            records = fuelRecords,
            mpgByRecordId = fuelEntries.associateBy({ it.record.id }) { it.mpg },
            fuelEntries = fuelEntries,
            averageMpg = FuelEconomyCalculator.averageMpg(fuelEntries),
            bestMpg = FuelEconomyCalculator.bestMpg(fuelEntries),
            worstMpg = FuelEconomyCalculator.worstMpg(fuelEntries),
            totalSpent = fuelRecords.sumOf { it.totalCost },
            unitSystem = unitSystem,
            currencySymbol = currency.symbol,
            isEvOrPhev = isEvOrPhev,
            isPureEv = isPureEv,
            chargingRecords = chargingRecords,
            chargingEntries = chargingEntries,
            averageWhPerMi = ChargingCalculator.averageWhPerMi(chargingEntries),
            averageKwhPer100Km = ChargingCalculator.averageKwhPer100Km(chargingEntries),
            averageMpge = ChargingCalculator.averageMpge(chargingEntries),
            totalChargingSpent = chargingRecords.sumOf { it.totalCost },
            batteryHealth = batteryHealth,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelUiState())

    fun saveRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelRepository.saveRecord(record.copy(vehicleId = vehicleId))
            WidgetRefresher.refresh(context)
            WorkScheduler.triggerImmediateReminderCheck(context)
            WorkScheduler.triggerImmediateLubeLoggerSync(context)
        }
    }

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelRepository.deleteRecord(record)
            WidgetRefresher.refresh(context)
            WorkScheduler.triggerImmediateReminderCheck(context)
            WorkScheduler.triggerImmediateLubeLoggerSync(context)
        }
    }

    fun saveChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            chargingRepository.saveRecord(record.copy(vehicleId = vehicleId))
            WidgetRefresher.refresh(context)
            WorkScheduler.triggerImmediateReminderCheck(context)
            WorkScheduler.triggerImmediateLubeLoggerSync(context)
        }
    }

    fun deleteChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            chargingRepository.deleteRecord(record)
            WidgetRefresher.refresh(context)
            WorkScheduler.triggerImmediateReminderCheck(context)
            WorkScheduler.triggerImmediateLubeLoggerSync(context)
        }
    }

    private companion object {
        fun isVehicleEvByDetails(vehicle: Vehicle?, specs: VehicleSpecs?): Boolean {
            if (specs?.isEvOrPhev() == true) return true
            val make = vehicle?.make?.lowercase().orEmpty()
            val model = vehicle?.model?.lowercase().orEmpty()
            val trim = vehicle?.trim?.lowercase().orEmpty()
            val text = "$make $model $trim"
            return text.contains("tesla") || text.contains("rivian") || text.contains("lucid") ||
                text.contains("polestar") || text.contains("mach-e") || text.contains("lightning") ||
                text.contains("ioniq") || text.contains("ev6") || text.contains("id.4") ||
                text.contains("leaf") || text.contains("bolt") || text.contains("phev") ||
                text.contains("plug-in") || text.contains("electric") || text.contains("ev")
        }
    }
}
