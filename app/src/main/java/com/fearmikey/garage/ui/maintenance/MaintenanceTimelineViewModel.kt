package com.fearmikey.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.MaintenanceSortOrder
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaintenanceTimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _sortOrder = MutableStateFlow(MaintenanceSortOrder.DATE)
    val sortOrder: StateFlow<MaintenanceSortOrder> = _sortOrder.asStateFlow()

    val records: StateFlow<List<MaintenanceRecord>> = _sortOrder
        .flatMapLatest { order -> maintenanceRepository.getRecordsForVehicle(vehicleId, order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSortOrder(order: MaintenanceSortOrder) {
        _sortOrder.value = order
    }

    fun saveRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            maintenanceRepository.saveRecord(record.copy(vehicleId = vehicleId))
        }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { maintenanceRepository.deleteRecord(record) }
    }
}
