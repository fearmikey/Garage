package com.fearmikey.garage.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.MainActivity
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.DocumentExpirationEngine
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Runs periodically (see [WorkScheduler]) or immediately upon data changes
 * and re-evaluates every incomplete reminder and maintenance interval across all vehicles,
 * notifying for anything overdue or due soon.
 */
@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        notifier.ensureChannel()

        val upcomingWindowMiles = preferencesRepository.maintenanceMileageWindow.firstOrNull()
            ?: DEFAULT_UPCOMING_WINDOW_MILES
        val upcomingWindowDays = preferencesRepository.maintenanceDaysWindow.firstOrNull()
            ?: DEFAULT_UPCOMING_WINDOW_DAYS

        val docRemindersEnabled = preferencesRepository.documentExpirationRemindersEnabled.firstOrNull() ?: true
        val docDaysWindow = preferencesRepository.documentExpirationDaysWindow.firstOrNull() ?: 30

        val allVehicles = vehicleRepository.getAllVehicles().firstOrNull() ?: emptyList()
        for (vehicle in allVehicles) {
            val latestMileage = maintenanceRepository.getLatestMileageForVehicle(vehicle.id).firstOrNull()
            val records = maintenanceRepository.getRecordsForVehicle(vehicle.id).firstOrNull() ?: emptyList()
            val customRules = customMaintenanceRuleRepository.getRulesForVehicle(vehicle.id).firstOrNull() ?: emptyList()
            val ignoredRules = maintenanceRepository.getIgnoredRulesForVehicle(vehicle.id).firstOrNull() ?: emptyList()

            val vehicleLabel = listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                .joinToString(" ")
                .ifBlank { vehicle.vin }

            val suggestions = MaintenanceScheduleEngine.suggestionsFor(
                vehicle = vehicle,
                latestMileage = latestMileage,
                records = records,
                customRules = customRules.map { it.toMaintenanceRule() },
                ignoredTaskNames = ignoredRules.map { it.taskName }.toSet(),
                upcomingWindowMiles = upcomingWindowMiles,
                upcomingWindowDays = upcomingWindowDays,
            )

            for (suggestion in suggestions) {
                val taskNameLower = suggestion.rule.taskName.lowercase()

                val notificationId = taskNameLower.hashCode() xor vehicle.id.toInt()
                if ((suggestion.status == ReminderStatus.OVERDUE) || (suggestion.status == ReminderStatus.UPCOMING)) {
                    notifier.notifyDue(
                        notificationId = notificationId,
                        taskName = suggestion.rule.taskName,
                        vehicleLabel = vehicleLabel,
                        status = suggestion.status,
                        vehicleId = vehicle.id,
                    )
                } else {
                    notifier.cancel(notificationId.toLong())
                }
            }

            // Document & Renewal Expiry Reminders
            val regIns = vehicleRepository.getRegistrationInsurance(vehicle.id).firstOrNull()
            if ((regIns != null) && docRemindersEnabled) {
                val docReminders = DocumentExpirationEngine.checkExpirations(
                    record = regIns,
                    upcomingWindowDays = docDaysWindow,
                )
                for (docReminder in docReminders) {
                    val notificationId = (vehicle.id.toString() + "_" + docReminder.idKey).hashCode()
                    if ((docReminder.status == ReminderStatus.OVERDUE) || (docReminder.status == ReminderStatus.UPCOMING)) {
                        notifier.notifyDue(
                            notificationId = notificationId,
                            taskName = docReminder.title,
                            vehicleLabel = vehicleLabel,
                            status = docReminder.status,
                            vehicleId = vehicle.id,
                            action = MainActivity.ACTION_OPEN_DOCUMENTS,
                        )
                    } else {
                        notifier.cancel(notificationId.toLong())
                    }
                }
            }
        }

        // Personal Driver's License Expiry Reminder
        val dlExpiration = preferencesRepository.driversLicenseExpiration.firstOrNull()
        val dlNotificationId = "personal_drivers_license".hashCode()
        if (dlExpiration != null && docRemindersEnabled) {
            val windowMillis = TimeUnit.DAYS.toMillis(docDaysWindow.coerceAtLeast(1).toLong())
            val now = System.currentTimeMillis()
            val dlStatus = when {
                dlExpiration <= now -> ReminderStatus.OVERDUE
                dlExpiration <= (now + windowMillis) -> ReminderStatus.UPCOMING
                else -> ReminderStatus.OK
            }
            if ((dlStatus == ReminderStatus.OVERDUE) || (dlStatus == ReminderStatus.UPCOMING)) {
                notifier.notifyDue(
                    notificationId = dlNotificationId,
                    taskName = "Driver's License Renewal",
                    vehicleLabel = "Personal Driver Document",
                    status = dlStatus,
                    action = MainActivity.ACTION_OPEN_DRIVERS_LICENSE,
                )
            } else {
                notifier.cancel(dlNotificationId.toLong())
            }
        } else {
            notifier.cancel(dlNotificationId.toLong())
        }

        WidgetRefresher.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reminder-check"
        const val IMMEDIATE_WORK_NAME = "reminder-check-immediate"
        const val DEFAULT_UPCOMING_WINDOW_MILES = 500
        const val DEFAULT_UPCOMING_WINDOW_DAYS = 10
    }
}
