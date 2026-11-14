package com.fearmikey.garage.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.repository.BackupResult
import com.fearmikey.garage.data.repository.WebDavBackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the daily WebDAV backup upload (see [CloudBackupScheduler]). Transient
 * failures (network/server issues) are retried; ones a retry can't fix (sync not
 * configured, credentials rejected) fail this run and wait for the next period,
 * instead of re-exporting the whole backup on every backoff.
 */
@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val webDavBackupRepository: WebDavBackupRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (val result = webDavBackupRepository.syncNow()) {
        BackupResult.Success -> Result.success()
        is BackupResult.Failure -> if (result.retryable) Result.retry() else Result.failure()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "cloud-backup-sync"
    }
}
