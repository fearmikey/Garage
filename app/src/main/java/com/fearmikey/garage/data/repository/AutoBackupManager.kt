package com.fearmikey.garage.data.repository

import android.content.Context
import androidx.room.InvalidationTracker
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors database table changes and triggers an automatic backup write
 * to a user-selected local folder whenever data is modified in the app.
 */
@Singleton
class AutoBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GarageDatabase,
    private val backupRepository: BackupRepository,
    private val cloudBackupPreferencesManager: CloudBackupPreferencesManager,
    private val lubeLoggerCredentialsManager: LubeLoggerCredentialsManager,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null
    private var syncDebounceJob: Job? = null
    private val backupMutex = Mutex()

    init {
        setupRoomInvalidationObserver()
    }

    private fun setupRoomInvalidationObserver() {
        try {
            val observer = object : InvalidationTracker.Observer(BACKUP_TABLES + SYNCED_TABLES) {
                override fun onInvalidated(tables: Set<String>) {
                    triggerAutoBackup()
                    if (tables.any { it in SYNCED_TABLES }) triggerLubeLoggerSync()
                }
            }
            database.invalidationTracker.addObserver(observer)
        } catch (_: Exception) {
            // Ignored if database is an uninitialized test double
        }
    }

    /**
     * Debounced, and never cancels a sync in progress: the sync's own writes invalidate these
     * tables too, and the old REPLACE policy restarted the sync halfway through every time it
     * saved a record (a cancelled push could also leave a server record Garage never linked).
     * While a sync runs, at most one follow-up sync is queued behind it so edits made meanwhile
     * still go out.
     */
    private fun triggerLubeLoggerSync() {
        if (!lubeLoggerCredentialsManager.isConfigured() || !lubeLoggerCredentialsManager.isSyncEnabled()) return
        syncDebounceJob?.cancel()
        syncDebounceJob = scope.launch {
            delay(2000)
            enqueueLubeLoggerSync()
        }
    }

    private fun enqueueLubeLoggerSync() {
        val worker = com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker
        val policy = if (worker.isRunning) {
            if (!worker.followUpQueued.compareAndSet(false, true)) return
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE
        } else {
            androidx.work.ExistingWorkPolicy.REPLACE
        }
        try {
            val request = androidx.work.OneTimeWorkRequestBuilder<com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("LubeLoggerSync", policy, request)
        } catch (_: Exception) {
            // Ignored if WorkManager is not initialized (e.g. unit tests)
        }
    }

    /**
     * Called when local app data or images are updated.
     * Debounces rapid consecutive modifications before triggering an export.
     */
    fun triggerAutoBackup() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500)
            performAutoBackup()
        }
    }

    suspend fun performAutoBackup(): BackupResult = backupMutex.withLock {
        val enabled = cloudBackupPreferencesManager.localBackupEnabled.first()
        if (!enabled) return BackupResult.Success

        val folderUri = cloudBackupPreferencesManager.localBackupFolderUri.first()
        if (folderUri.isBlank()) {
            val failure = BackupResult.Failure(context.getString(R.string.backup_error_no_folder))
            cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
            return failure
        }

        return backupRepository.exportBackupToLocalFolder(folderUri)
    }

    private companion object {
        /** User data that isn't synced with LubeLogger but still belongs in the backup. */
        val BACKUP_TABLES = arrayOf(
            "vehicle_specs",
            "vehicle_parts_info",
            "custom_maintenance_rules",
            "ignored_maintenance_rules",
            "recall_campaign_states",
            "torque_specs",
            "trip_records",
            "custom_documents",
        )

        /** Tables whose changes LubeLoggerSyncWorker pushes (also backed up). */
        val SYNCED_TABLES = arrayOf(
            "vehicles",
            "vehicle_registration_insurance",
            "maintenance_records",
            "fuel_records",
            "charging_records",
            "modification_records",
            "supply_records",
            "vehicle_notes",
        )
    }
}
