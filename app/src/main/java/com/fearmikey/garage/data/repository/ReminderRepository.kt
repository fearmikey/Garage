package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** How urgent a reminder currently is, relative to today's date/mileage. */
enum class ReminderStatus { OVERDUE, UPCOMING, OK, COMPLETED }

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
) {
    fun getRemindersForVehicle(vehicleId: Long): Flow<List<Reminder>> =
        reminderDao.getRemindersForVehicle(vehicleId)

    suspend fun getIncompleteReminders(): List<Reminder> = reminderDao.getIncompleteReminders()

    suspend fun saveReminder(reminder: Reminder): Long = reminderDao.upsert(reminder)

    suspend fun setCompleted(reminder: Reminder, isCompleted: Boolean) =
        reminderDao.update(reminder.copy(isCompleted = isCompleted))

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)

    companion object {
        const val UPCOMING_WINDOW_DAYS = 7L
        const val UPCOMING_WINDOW_MILES = 500

        /**
         * Shared status logic used by both the Reminders UI and
         * [com.fearmikey.garage.notification.ReminderCheckWorker] so the two never
         * disagree about what counts as "due soon".
         */
        fun computeStatus(
            reminder: Reminder,
            latestMileage: Int?,
            now: Long = System.currentTimeMillis(),
        ): ReminderStatus {
            if (reminder.isCompleted) return ReminderStatus.COMPLETED

            val upcomingWindowMillis = TimeUnit.DAYS.toMillis(UPCOMING_WINDOW_DAYS)
            val dueDate = reminder.dueDate
            val dueMileage = reminder.dueMileage

            val dateStatus = dueDate?.let {
                when {
                    now >= it -> ReminderStatus.OVERDUE
                    it - now <= upcomingWindowMillis -> ReminderStatus.UPCOMING
                    else -> ReminderStatus.OK
                }
            }
            val mileageStatus = if (dueMileage != null && latestMileage != null) {
                when {
                    latestMileage >= dueMileage -> ReminderStatus.OVERDUE
                    dueMileage - latestMileage <= UPCOMING_WINDOW_MILES -> ReminderStatus.UPCOMING
                    else -> ReminderStatus.OK
                }
            } else null

            // Whichever dimension (date or mileage) is more urgent wins.
            return listOfNotNull(dateStatus, mileageStatus).minByOrNull {
                when (it) {
                    ReminderStatus.OVERDUE -> 0
                    ReminderStatus.UPCOMING -> 1
                    ReminderStatus.OK -> 2
                    ReminderStatus.COMPLETED -> 3
                }
            } ?: ReminderStatus.OK
        }
    }
}
