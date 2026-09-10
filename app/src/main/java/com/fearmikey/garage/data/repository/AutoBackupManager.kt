package com.fearmikey.garage.data.repository

import android.content.Context
import androidx.room.InvalidationTracker
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.GarageDatabase
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
    private val database: GarageDatabase,
    private val backupRepository: BackupRepository,
    private val cloudBackupPreferencesManager: CloudBackupPreferencesManager,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null
    private val backupMutex = Mutex()

    init {
        setupRoomInvalidationObserver()
    }

    private fun setupRoomInvalidationObserver() {
        try {
            val tables = arrayOf(
                "vehicles",
                "maintenance_records",
                "reminders",
                "vehicle_specs",
                "fuel_records",
                "vehicle_parts_info",
                "custom_maintenance_rules",
            )
            val observer = object : InvalidationTracker.Observer(tables) {
                override fun onInvalidated(tables: Set<String>) {
                    triggerAutoBackup()
                }
            }
            database.invalidationTracker.addObserver(observer)
        } catch (_: Exception) {
            // Ignored if database is an uninitialized test double
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
            val failure = BackupResult.Failure("No local backup folder configured.")
            cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
            return failure
        }

        return backupRepository.exportBackupToLocalFolder(folderUri)
    }
}
