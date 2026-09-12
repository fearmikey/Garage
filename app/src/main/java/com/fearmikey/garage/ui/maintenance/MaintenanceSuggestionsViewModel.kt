package com.fearmikey.garage.ui.maintenance

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.notification.WorkScheduler
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context? = null,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val latestMileage: StateFlow<Int?> = maintenanceRepository.getLatestMileageForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customRules: StateFlow<List<CustomMaintenanceRule>> =
        customMaintenanceRuleRepository.getRulesForVehicle(vehicleId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestions: StateFlow<List<MaintenanceSuggestion>> = combine(
        vehicleRepository.getVehicleById(vehicleId),
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        maintenanceRepository.getLatestMileageForVehicle(vehicleId),
        customMaintenanceRuleRepository.getRulesForVehicle(vehicleId),
        reminderRepository.getRemindersForVehicle(vehicleId),
    ) { vehicle, records, latestMileage, customRules, reminders ->
        if (vehicle == null) {
            emptyList()
        } else {
            val activeReminderTasks = reminders
                .asSequence()
                .filter { !it.isCompleted }
                .map { it.taskName.lowercase() }
                .toSet()

            MaintenanceScheduleEngine.suggestionsFor(
                vehicle,
                latestMileage,
                records,
                customRules.map { it.toMaintenanceRule() },
            ).filter { suggestion ->
                suggestion.rule.taskName.lowercase() !in activeReminderTasks
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Creates a [Reminder] pre-filled from a suggestion, so it shows up in the Reminders tab. */
    fun addAsReminder(suggestion: MaintenanceSuggestion) {
        viewModelScope.launch {
            reminderRepository.saveReminder(
                Reminder(
                    vehicleId = vehicleId,
                    taskName = suggestion.rule.taskName,
                    dueDate = suggestion.nextDueDate,
                    dueMileage = suggestion.nextDueMileage,
                )
            )
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
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
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun saveCustomRule(rule: CustomMaintenanceRule) {
        viewModelScope.launch {
            customMaintenanceRuleRepository.saveRule(rule.copy(vehicleId = vehicleId))
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun deleteCustomRule(rule: CustomMaintenanceRule) {
        viewModelScope.launch {
            customMaintenanceRuleRepository.deleteRule(rule)
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }
}
