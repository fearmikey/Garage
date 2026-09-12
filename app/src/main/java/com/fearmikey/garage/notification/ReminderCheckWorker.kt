package com.fearmikey.garage.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Runs periodically (see [WorkScheduler]) or immediately upon data changes
 * and re-evaluates every incomplete reminder and maintenance interval across all vehicles,
 * notifying for anything overdue or due soon.
 */
@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        notifier.ensureChannel()

        val incompleteReminders = reminderRepository.getIncompleteReminders()
        val processedTasksByVehicle = mutableMapOf<Long, MutableSet<String>>()

        for (reminder in incompleteReminders) {
            val vehicle = vehicleRepository.getVehicleByIdOnce(reminder.vehicleId) ?: continue
            val latestMileage = maintenanceRepository.getLatestMileageForVehicle(reminder.vehicleId).firstOrNull()
            val status = ReminderRepository.computeStatus(reminder, latestMileage)

            processedTasksByVehicle
                .getOrPut(reminder.vehicleId) { mutableSetOf() }
                .add(reminder.taskName.lowercase())

            if (status == ReminderStatus.OVERDUE || status == ReminderStatus.UPCOMING) {
                val vehicleLabel = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                    .joinToString(" ")
                    .ifBlank { vehicle.vin }
                notifier.notifyDue(reminder, vehicleLabel, status)
            } else {
                notifier.cancel(reminder.id)
            }
        }

        val allVehicles = vehicleRepository.getAllVehicles().firstOrNull() ?: emptyList()
        for (vehicle in allVehicles) {
            val latestMileage = maintenanceRepository.getLatestMileageForVehicle(vehicle.id).firstOrNull()
            val records = maintenanceRepository.getRecordsForVehicle(vehicle.id).firstOrNull() ?: emptyList()
            val customRules = customMaintenanceRuleRepository.getRulesForVehicle(vehicle.id).firstOrNull() ?: emptyList()

            val suggestions = MaintenanceScheduleEngine.suggestionsFor(
                vehicle = vehicle,
                latestMileage = latestMileage,
                records = records,
                customRules = customRules.map { it.toMaintenanceRule() },
            )

            val existingTasks = processedTasksByVehicle[vehicle.id] ?: emptySet()

            for (suggestion in suggestions) {
                val taskNameLower = suggestion.rule.taskName.lowercase()
                if (taskNameLower in existingTasks) continue

                val notificationId = taskNameLower.hashCode() xor vehicle.id.toInt()
                if (suggestion.status == ReminderStatus.OVERDUE || suggestion.status == ReminderStatus.UPCOMING) {
                    val vehicleLabel = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }
                    notifier.notifyDue(
                        notificationId = notificationId,
                        taskName = suggestion.rule.taskName,
                        vehicleLabel = vehicleLabel,
                        status = suggestion.status,
                    )
                } else {
                    notifier.cancel(notificationId.toLong())
                }
            }
        }

        WidgetRefresher.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reminder-check"
        const val IMMEDIATE_WORK_NAME = "reminder-check-immediate"
    }
}
