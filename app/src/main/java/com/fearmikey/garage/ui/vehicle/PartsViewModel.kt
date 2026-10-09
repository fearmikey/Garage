package com.fearmikey.garage.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
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
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val parts: StateFlow<VehiclePartsInfo?> = combine(
        vehicleRepository.getVehicleParts(vehicleId),
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
        preferencesRepository.unitSystem,
    ) { storedParts, vehicle, specs, unitSystem ->
        storedParts?.takeUnless { it.isEmpty() }
            ?: PartsEstimator.estimateParts(vehicleId, vehicle, specs, unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveParts(info: VehiclePartsInfo) {
        viewModelScope.launch {
            vehicleRepository.saveVehicleParts(info.copy(vehicleId = vehicleId))
        }
    }
}
