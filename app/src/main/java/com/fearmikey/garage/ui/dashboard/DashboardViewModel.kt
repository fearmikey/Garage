package com.fearmikey.garage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class VehicleListItem(
    val vehicle: Vehicle,
    val latestMileage: Int?,
    val imageFile: File?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    maintenanceRepository: MaintenanceRepository,
    imageStorageManager: ImageStorageManager,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val vehicles: StateFlow<List<VehicleListItem>> = vehicleRepository.getAllVehicles()
        .flatMapLatest { vehicles ->
            if (vehicles.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    vehicles.map { vehicle ->
                        maintenanceRepository.getLatestMileageForVehicle(vehicle.id).map { mileage ->
                            VehicleListItem(
                                vehicle = vehicle,
                                latestMileage = mileage,
                                imageFile = vehicle.imageUri?.let { imageStorageManager.imageFile(it) },
                            )
                        }
                    }
                ) { items -> items.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
