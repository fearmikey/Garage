package com.fearmikey.garage.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Runs once a day (see [WorkScheduler]) and re-evaluates every incomplete
 * reminder across all vehicles, notifying for anything overdue or due soon.
 * Notifications are keyed per-reminder (see [ReminderNotifier]), so running
 * this daily simply refreshes existing notifications rather than spamming.
 */
@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        notifier.ensureChannel()

        val incompleteReminders = reminderRepository.getIncompleteReminders()
        for (reminder in incompleteReminders) {
            val vehicle = vehicleRepository.getVehicleByIdOnce(reminder.vehicleId) ?: continue
            val latestMileage = maintenanceRepository.getLatestMileageForVehicle(reminder.vehicleId).firstOrNull()
            val status = ReminderRepository.computeStatus(reminder, latestMileage)

            if (status == ReminderStatus.OVERDUE || status == ReminderStatus.UPCOMING) {
                val vehicleLabel = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                    .joinToString(" ")
                    .ifBlank { vehicle.vin }
                notifier.notifyDue(reminder, vehicleLabel, status)
            } else {
                notifier.cancel(reminder.id)
            }
        }
        WidgetRefresher.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reminder-check"
    }
}
