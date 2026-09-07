package com.fearmikey.garage.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the daily reminder-check background job. */
object WorkScheduler {
    fun scheduleReminderChecks(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderCheckWorker.UNIQUE_WORK_NAME,
            // KEEP: if a check is already scheduled, leave it as-is rather
            // than resetting its schedule every time the app process starts.
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
