package com.fearmikey.garage.ui.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.PreferencesManager
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.TripRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TripRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val trips: StateFlow<List<TripRecord>> = repository.getTripsForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** User-configured default reimbursement rate, prefilled when logging a new trip. */
    val defaultReimbursementRate: StateFlow<Double> = preferencesRepository.mileageReimbursementRate
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PreferencesManager.DEFAULT_MILEAGE_REIMBURSEMENT_RATE,
        )

    val currency: StateFlow<AppCurrency> = preferencesRepository.appCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppCurrency.USD)

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)
        
    val tripState = com.fearmikey.garage.service.TripTrackingService.tripState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.fearmikey.garage.service.TripTrackingService.TripState.Stopped)

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
