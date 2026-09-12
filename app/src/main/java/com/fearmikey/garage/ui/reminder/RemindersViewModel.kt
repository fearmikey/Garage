package com.fearmikey.garage.ui.reminder

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
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

data class ReminderListItem(
    val reminder: Reminder,
    val status: ReminderStatus,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository,
    maintenanceRepository: MaintenanceRepository,
    preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context? = null,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val reminders: StateFlow<List<ReminderListItem>> = combine(
        reminderRepository.getRemindersForVehicle(vehicleId),
        maintenanceRepository.getLatestMileageForVehicle(vehicleId),
    ) { reminders, latestMileage ->
        reminders.map { reminder ->
            ReminderListItem(reminder, ReminderRepository.computeStatus(reminder, latestMileage))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.saveReminder(reminder.copy(vehicleId = vehicleId))
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun setCompleted(reminder: Reminder, isCompleted: Boolean) {
        viewModelScope.launch {
            reminderRepository.setCompleted(reminder, isCompleted)
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminder)
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }
}
