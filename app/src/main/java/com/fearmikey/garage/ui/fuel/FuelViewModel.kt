package com.fearmikey.garage.ui.fuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelUiState(
    val records: List<FuelRecord> = emptyList(),
    /** Segment MPG for the fill-up that *completed* it, keyed by [FuelRecord.id]. */
    val mpgByRecordId: Map<Long, Double> = emptyMap(),
    val averageMpg: Double? = null,
    val totalSpent: Double = 0.0,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
)

@HiltViewModel
class FuelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fuelRepository: FuelRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<FuelUiState> = combine(
        fuelRepository.getRecordsForVehicle(vehicleId),
        preferencesRepository.unitSystem,
    ) { records, unitSystem ->
        val entries = FuelEconomyCalculator.entriesFor(records)
        FuelUiState(
            records = records,
            mpgByRecordId = entries.associate { it.record.id to it.mpg },
            averageMpg = FuelEconomyCalculator.averageMpg(entries),
            totalSpent = records.sumOf { it.totalCost },
            unitSystem = unitSystem,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelUiState())

    fun saveRecord(record: FuelRecord) {
        viewModelScope.launch {
            fuelRepository.saveRecord(record.copy(vehicleId = vehicleId))
        }
    }

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch { fuelRepository.deleteRecord(record) }
    }
}
