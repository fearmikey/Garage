package com.fearmikey.garage.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.CustomSpec
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleSpecsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val specs: StateFlow<VehicleSpecs?> = vehicleRepository.getVehicleSpecs(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Persists user-edited specs; blank fields are stored as null so they stay hidden. */
    fun saveSpecs(edited: VehicleSpecs) {
        viewModelScope.launch {
            vehicleRepository.saveVehicleSpecs(vehicleId, normalize(edited))
        }
    }

    companion object {
        fun normalize(specs: VehicleSpecs): VehicleSpecs {
            fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
            return specs.copy(
                engineCylinders = specs.engineCylinders.clean(),
                displacementL = specs.displacementL.clean(),
                engineHp = specs.engineHp.clean(),
                fuelType = specs.fuelType.clean(),
                fuelTypeSecondary = specs.fuelTypeSecondary.clean(),
                electrificationLevel = specs.electrificationLevel.clean(),
                transmissionStyle = specs.transmissionStyle.clean(),
                transmissionSpeeds = specs.transmissionSpeeds.clean(),
                bodyClass = specs.bodyClass.clean(),
                doors = specs.doors.clean(),
                gvwr = specs.gvwr.clean(),
                series = specs.series.clean(),
                vehicleType = specs.vehicleType.clean(),
                plantCity = specs.plantCity.clean(),
                plantState = specs.plantState.clean(),
                plantCountry = specs.plantCountry.clean(),
                manufacturer = specs.manufacturer.clean(),
                trailerBrakedCapacity = specs.trailerBrakedCapacity.clean(),
                trailerUnbrakedCapacity = specs.trailerUnbrakedCapacity.clean(),
                gcwr = specs.gcwr.clean(),
                curbWeight = specs.curbWeight.clean(),
                customSpecs = specs.customSpecs
                    .map { CustomSpec(label = it.label.trim(), value = it.value.trim()) }
                    .filter { it.label.isNotEmpty() || it.value.isNotEmpty() },
            )
        }
    }
}
