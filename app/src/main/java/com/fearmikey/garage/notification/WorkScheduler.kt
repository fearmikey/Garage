package com.fearmikey.garage.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules background jobs for reminder and maintenance checks. */
object WorkScheduler {
    fun scheduleReminderChecks(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ReminderCheckWorker.UNIQUE_WORK_NAME,
                // KEEP: if a check is already scheduled, leave it as-is rather
                // than resetting its schedule every time the app process starts.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } catch (_: Exception) {
            // WorkManager not initialized (e.g. in unit tests)
        }
    }

    fun triggerImmediateReminderCheck(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<ReminderCheckWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ReminderCheckWorker.IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        } catch (_: Exception) {
            // WorkManager not initialized (e.g. in unit tests)
        }
    }
}
