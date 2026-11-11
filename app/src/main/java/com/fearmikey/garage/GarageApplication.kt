package com.fearmikey.garage

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.repository.AutoBackupManager
import com.fearmikey.garage.di.ApplicationScope
import com.fearmikey.garage.notification.CloudBackupScheduler
import com.fearmikey.garage.notification.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main [Application] class for Garage.
 *
 * NOTE: Garage is strictly a 100% Free and Open Source (FOSS) application built for
 * F-Droid and open-source distribution. It contains zero Google Play Services, zero ads,
 * zero analytics, and zero proprietary binary dependencies.
 */
@HiltAndroidApp
class GarageApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var cloudBackupPreferencesManager: CloudBackupPreferencesManager

    @Inject
    lateinit var autoBackupManager: AutoBackupManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.scheduleReminderChecks(this)
        WorkScheduler.scheduleLubeLoggerSync(this)
        WorkScheduler.triggerImmediateLubeLoggerSync(this)
        // Reading DataStore does disk I/O; don't block cold start on it.
        applicationScope.launch {
            val cloudSyncEnabled = cloudBackupPreferencesManager.cloudSyncEnabled.first()
            CloudBackupScheduler.scheduleOrCancel(this@GarageApplication, enabled = cloudSyncEnabled)
        }
    }
}
