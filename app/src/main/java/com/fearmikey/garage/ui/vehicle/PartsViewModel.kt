package com.fearmikey.garage.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val parts: StateFlow<VehiclePartsInfo?> = combine(
        vehicleRepository.getVehicleParts(vehicleId),
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
    ) { storedParts, vehicle, specs ->
        storedParts?.takeUnless { it.isEmpty() }
            ?: PartsEstimator.estimateParts(vehicleId, vehicle, specs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveParts(info: VehiclePartsInfo) {
        viewModelScope.launch {
            vehicleRepository.saveVehicleParts(info.copy(vehicleId = vehicleId))
        }
    }
}
