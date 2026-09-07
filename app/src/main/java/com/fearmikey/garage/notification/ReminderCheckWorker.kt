package com.fearmikey.garage.notification

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.widget.GarageWidget
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
        refreshWidgets()
        return Result.success()
    }

    private suspend fun refreshWidgets() {
        // Best-effort: a widget update failure (e.g. no widgets currently placed) should
        // never fail this otherwise-successful reminder check.
        try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val widget = GarageWidget()
            manager.getGlanceIds(GarageWidget::class.java).forEach { id -> widget.update(applicationContext, id) }
        } catch (e: Exception) {
            Log.w("ReminderCheckWorker", "Failed to refresh Garage widget", e)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reminder-check"
    }
}
