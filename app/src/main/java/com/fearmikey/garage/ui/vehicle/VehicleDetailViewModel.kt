package com.fearmikey.garage.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    imageStorageManager: ImageStorageManager,
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

    /** Backs the shared-element vehicle photo header in [VehicleDetailScreen]. */
    val imageFile: StateFlow<File?> = vehicle
        .map { it?.imageUri?.let { uri -> imageStorageManager.imageFile(uri) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
