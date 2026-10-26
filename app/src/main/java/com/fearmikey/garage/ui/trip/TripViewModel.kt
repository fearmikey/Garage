package com.fearmikey.garage.ui.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.data.repository.TripRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TripRepository
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val trips: StateFlow<List<TripRecord>> = repository.getTripsForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTrip(trip: TripRecord) {
        viewModelScope.launch {
            if (trip.id == 0L) {
                repository.insertTrip(trip.copy(vehicleId = vehicleId))
            } else {
                repository.updateTrip(trip.copy(vehicleId = vehicleId))
            }
        }
    }

    fun deleteTrip(trip: TripRecord) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }
}
