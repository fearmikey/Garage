package com.fearmikey.garage.ui.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.AutoBackupManager
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.repository.WebDavBackupRepository
import com.fearmikey.garage.notification.ReminderNotifier
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun checkSelfPermission(permission: String): Int = 0
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val unitsType: Flow<String> = MutableStateFlow("metric")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)

        override suspend fun setUnitsType(units: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
    }

    private class FakeVehicleDao : VehicleDao {
        val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehiclesFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = MutableStateFlow(null)
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = null
        override suspend fun upsert(vehicle: Vehicle): Long = 1L
        override suspend fun update(vehicle: Vehicle) {}
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakeVehicleSpecsDao : VehicleSpecsDao {
        override fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?> = MutableStateFlow(null)
        override suspend fun upsert(specs: VehicleSpecs) {}
    }

    private class FakeVehiclePartsDao : VehiclePartsDao {
        override fun getByVehicleId(vehicleId: Long): Flow<VehiclePartsInfo?> = MutableStateFlow(null)
        override suspend fun upsert(info: VehiclePartsInfo) {}
    }

    private class FakeVinDecoderApi : VinDecoderApi {
        override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse = VinDecodeResponse(results = emptyList())
    }

    private class TestCloudBackupPreferencesManager(context: Context) : CloudBackupPreferencesManager(context) {
        val enabledFlow = MutableStateFlow(false)
        val folderUriFlow = MutableStateFlow("")
        val timestampFlow = MutableStateFlow<Long?>(null)
        val errorFlow = MutableStateFlow<String?>(null)

        override val localBackupEnabled: Flow<Boolean> get() = enabledFlow
        override val localBackupFolderUri: Flow<String> get() = folderUriFlow
        override val lastLocalBackupTimestamp: Flow<Long?> get() = timestampFlow
        override val lastLocalBackupError: Flow<String?> get() = errorFlow

        override val cloudSyncEnabled: Flow<Boolean> get() = MutableStateFlow(false)
        override val webdavUrl: Flow<String> get() = MutableStateFlow("")
        override val webdavUsername: Flow<String> get() = MutableStateFlow("")
        override val lastSyncTimestamp: Flow<Long?> get() = MutableStateFlow(null)
        override val lastSyncError: Flow<String?> get() = MutableStateFlow(null)

        override fun getWebdavPassword(): String? = null

        override suspend fun setLocalBackupEnabled(enabled: Boolean) {
            enabledFlow.value = enabled
        }

        override suspend fun setLocalBackupFolderUri(uri: String) {
            folderUriFlow.value = uri
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `local backup preferences flow updates uiState`() = runTest {
        val context = TestContext()
        val prefsRepo = FakePreferencesRepository()
        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val cloudPrefs = TestCloudBackupPreferencesManager(context)
        @Suppress("DEPRECATION")
        val dummyDb = object : GarageDatabase() {
            override fun vehicleDao(): VehicleDao = FakeVehicleDao()
            override fun maintenanceDao(): MaintenanceDao = object : MaintenanceDao {
                override fun getRecordsForVehicleByDate(vehicleId: Long) = MutableStateFlow(emptyList<MaintenanceRecord>())
                override fun getRecordsForVehicleByMileage(vehicleId: Long) = MutableStateFlow(emptyList<MaintenanceRecord>())
                override fun getLatestMileageForVehicle(vehicleId: Long) = MutableStateFlow(null)
                override suspend fun upsert(record: MaintenanceRecord) = 1L
                override suspend fun update(record: MaintenanceRecord) {}
                override suspend fun delete(record: MaintenanceRecord) {}
            }
            override fun reminderDao(): ReminderDao = object : ReminderDao {
                override fun getRemindersForVehicle(vehicleId: Long) = MutableStateFlow(emptyList<Reminder>())
                override suspend fun getIncompleteReminders() = emptyList<Reminder>()
                override suspend fun upsert(reminder: Reminder) = 1L
                override suspend fun update(reminder: Reminder) {}
                override suspend fun delete(reminder: Reminder) {}
            }
            override fun vehicleSpecsDao(): VehicleSpecsDao = FakeVehicleSpecsDao()
            override fun fuelDao(): FuelDao = object : FuelDao {
                override fun getRecordsForVehicle(vehicleId: Long) = MutableStateFlow(emptyList<FuelRecord>())
                override suspend fun upsert(record: FuelRecord) = 1L
                override suspend fun update(record: FuelRecord) {}
                override suspend fun delete(record: FuelRecord) {}
            }
            override fun vehiclePartsDao(): VehiclePartsDao = FakeVehiclePartsDao()
            override fun customMaintenanceRuleDao(): CustomMaintenanceRuleDao = object : CustomMaintenanceRuleDao {
                override fun getForVehicle(vehicleId: Long) = MutableStateFlow(emptyList<CustomMaintenanceRule>())
                override suspend fun upsert(rule: CustomMaintenanceRule) = 1L
                override suspend fun delete(rule: CustomMaintenanceRule) {}
            }
            override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
                throw UnsupportedOperationException()
            }
            override fun createInvalidationTracker(): InvalidationTracker {
                return InvalidationTracker(this, emptyMap(), emptyMap(), "vehicles")
            }
            override fun clearAllTables() {}
        }

        val backupRepo = BackupRepository(context, dummyDb, ImageStorageManager(context), cloudPrefs)
        val webDavRepo = WebDavBackupRepository(context, backupRepo, cloudPrefs)
        val autoBackupManager = AutoBackupManager(dummyDb, backupRepo, cloudPrefs)
        val notifier = ReminderNotifier(context)

        val viewModel = SettingsViewModel(
            context = context,
            backupRepository = backupRepo,
            preferencesRepository = prefsRepo,
            vehicleRepository = vehicleRepo,
            webDavBackupRepository = webDavRepo,
            cloudBackupPreferencesManager = cloudPrefs,
            autoBackupManager = autoBackupManager,
            reminderNotifier = notifier,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.localBackupEnabled)
        assertEquals("", viewModel.uiState.value.localBackupFolderUri)

        cloudPrefs.enabledFlow.value = true
        cloudPrefs.folderUriFlow.value = "content://com.android.externalstorage.documents/tree/primary%3ABackups"
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.localBackupEnabled)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3ABackups", viewModel.uiState.value.localBackupFolderUri)
    }
}
