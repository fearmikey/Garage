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

    fun scheduleLubeLoggerSync(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "LubeLoggerPeriodicSync",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } catch (_: Exception) {
            // WorkManager not initialized (e.g. in unit tests)
        }
    }

    fun triggerImmediateLubeLoggerSync(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "LubeLoggerImmediateSync",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        } catch (_: Exception) {
            // WorkManager not initialized (e.g. in unit tests)
        }
    }

    fun triggerManualLubeLoggerSync(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker>()
                .addTag(com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker.TAG_MANUAL)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker.MANUAL_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        } catch (_: Exception) {
            // WorkManager not initialized (e.g. in unit tests)
        }
    }
}
