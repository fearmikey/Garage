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
 * Runs the daily WebDAV backup upload (see [CloudBackupScheduler]). Failures
 * are retried rather than treated as permanent, since they're almost always
 * transient network/server issues rather than something a retry can't fix.
 */
@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val webDavBackupRepository: WebDavBackupRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (webDavBackupRepository.syncNow()) {
        BackupResult.Success -> Result.success()
        is BackupResult.Failure -> Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "cloud-backup-sync"
    }
}
