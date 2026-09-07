package com.fearmikey.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaintenanceSuggestionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val suggestions: StateFlow<List<MaintenanceSuggestion>> = combine(
        vehicleRepository.getVehicleById(vehicleId),
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        maintenanceRepository.getLatestMileageForVehicle(vehicleId),
    ) { vehicle, records, latestMileage ->
        if (vehicle == null) {
            emptyList()
        } else {
            MaintenanceScheduleEngine.suggestionsFor(vehicle, latestMileage, records)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Creates a [Reminder] pre-filled from a suggestion, so it shows up in the Reminders tab. */
    fun addAsReminder(suggestion: MaintenanceSuggestion) {
        viewModelScope.launch {
            reminderRepository.saveReminder(
                Reminder(
                    vehicleId = vehicleId,
                    taskName = suggestion.rule.taskName,
                    dueMileage = suggestion.nextDueMileage,
                )
            )
        }
    }

    /**
     * Saves a [MaintenanceRecord] logged directly from a suggestion (the "Log now" action).
     * The record already carries the suggestion's [MaintenanceRecord.category] and
     * [MaintenanceRecord.taskName] (see [AddEditMaintenanceRecordSheet]),
     * so it's tracked precisely rather than lumped in with other tasks in the same category.
     */
    fun logMaintenance(record: MaintenanceRecord) {
        viewModelScope.launch {
            maintenanceRepository.saveRecord(record.copy(vehicleId = vehicleId))
        }
    }
}
