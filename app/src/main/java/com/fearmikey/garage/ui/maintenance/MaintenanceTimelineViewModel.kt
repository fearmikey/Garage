package com.fearmikey.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaintenanceTimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val maintenanceRepository: MaintenanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val records: StateFlow<List<MaintenanceRecord>> = maintenanceRepository.getRecordsForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestMileage: StateFlow<Int?> = maintenanceRepository.getLatestMileageForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            maintenanceRepository.saveRecord(record.copy(vehicleId = vehicleId))
        }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { maintenanceRepository.deleteRecord(record) }
    }
}
