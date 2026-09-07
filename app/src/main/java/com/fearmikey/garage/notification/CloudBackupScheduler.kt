package com.fearmikey.garage.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules (or cancels) the daily WebDAV cloud-backup upload job. */
object CloudBackupScheduler {
    fun scheduleOrCancel(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                CloudBackupWorker.UNIQUE_WORK_NAME,
                // UPDATE: settings (e.g. enabling sync) can change at any
                // time, so always apply the latest constraints/schedule
                // rather than keeping whatever was previously enqueued.
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } else {
            workManager.cancelUniqueWork(CloudBackupWorker.UNIQUE_WORK_NAME)
        }
    }
}
