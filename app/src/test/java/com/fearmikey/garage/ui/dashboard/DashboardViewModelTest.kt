package com.fearmikey.garage.ui.dashboard

import android.content.Context
import android.content.ContextWrapper
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.ChargingDao
import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.IgnoredMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.dao.RecallCampaignStateDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager
import com.fearmikey.garage.data.repository.BackupRepository
import com.fearmikey.garage.data.repository.BackupResult
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.repository.WebDavBackupRepository
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        val vehicleFlow = MutableStateFlow<List<Vehicle>>(
            listOf(
                Vehicle(
                    id = 1L,
                    make = "Toyota",
                    model = "Tacoma",
                    year = 2020,
                )
            )
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleFlow.map { list -> list.find { it.id == vehicleId } }
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleFlow.value.find { it.id == vehicleId }
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

    private class FakeVehicleRegistrationDao : VehicleRegistrationDao {
        override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
        override suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance) {}
        override suspend fun deleteByVehicleId(vehicleId: Long) {}
    }

    private class FakeVinDecoderApi : VinDecoderApi {
        override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse = VinDecodeResponse(emptyList())
    }

    private class FakeFuelDao : FuelDao {
        override fun getRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> = MutableStateFlow(emptyList())
        override suspend fun upsert(record: FuelRecord): Long = 1L
        override suspend fun update(record: FuelRecord) {}
        override suspend fun delete(record: FuelRecord) {}
    }

    private class FakeCustomRuleDao : CustomMaintenanceRuleDao {
        override fun getForVehicle(vehicleId: Long): Flow<List<CustomMaintenanceRule>> = MutableStateFlow(emptyList())
        override suspend fun upsert(rule: CustomMaintenanceRule): Long = 1L
        override suspend fun delete(rule: CustomMaintenanceRule) {}
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val latestMileageFlow = MutableStateFlow<Int?>(null)
        override fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>> = MutableStateFlow(emptyList())
        override fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>> = MutableStateFlow(emptyList())
        override fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> = latestMileageFlow
        override suspend fun upsert(record: MaintenanceRecord): Long = 1L
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private open class FakePreferencesRepository : PreferencesRepository {
        override val unitsType: Flow<String> = MutableStateFlow("imperial")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.IMPERIAL)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(2)
        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
        override suspend fun setMaintenanceMileageWindow(miles: Int) {}
        override suspend fun incrementAppOpenCount(): Int = 1
        override suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {}
        override suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {}
    }

    private class FakeImageStorageManager : ImageStorageManager(
        context = FakeContext()
    ) {
        override fun imageFile(filename: String): File = File(filename)
    }

    private class FakeContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private class FakeLubeLoggerCredentialsManager(context: Context) :
        LubeLoggerCredentialsManager(context) {
        var isConfiguredValue = false
        override fun isConfigured(): Boolean = isConfiguredValue
    }


    private class FakeCloudBackupPreferencesManager(context: Context) :
        CloudBackupPreferencesManager(context) {
        val cloudSyncEnabledFlow = MutableStateFlow(false)
        override val cloudSyncEnabled: Flow<Boolean> get() = cloudSyncEnabledFlow
    }

    private class FakeWebDavBackupRepository(
        context: Context,
        cloudBackupPreferencesManager: CloudBackupPreferencesManager,
    ) : WebDavBackupRepository(
        context = context,
        backupRepository = BackupRepository(
            context = context,
            database = object : GarageDatabase() {
                override fun vehicleDao() = FakeVehicleDao()
                override fun vehicleSpecsDao() = FakeVehicleSpecsDao()
                override fun vehiclePartsDao() = FakeVehiclePartsDao()
                override fun vehicleRegistrationDao() = FakeVehicleRegistrationDao()
                override fun fuelDao() = FakeFuelDao()
                override fun customMaintenanceRuleDao() = FakeCustomRuleDao()
                override fun maintenanceDao() = FakeMaintenanceDao()
                override fun chargingDao(): ChargingDao = throw NotImplementedError()
                override fun ignoredMaintenanceRuleDao(): IgnoredMaintenanceRuleDao = throw NotImplementedError()
                override fun recallCampaignStateDao(): RecallCampaignStateDao = throw NotImplementedError()
                override fun modificationDao(): ModificationDao = throw NotImplementedError()
                override fun lubeLoggerPendingDeleteDao(): LubeLoggerPendingDeleteDao = throw NotImplementedError()
                override fun clearAllTables() {}
                override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
                    androidx.room.InvalidationTracker(this, emptyMap(), emptyMap(), "vehicles")
                @Suppress("DEPRECATION")
                override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper =
                    throw NotImplementedError()
            },
            imageStorageManager = FakeImageStorageManager(),
            cloudBackupPreferencesManager = cloudBackupPreferencesManager,
        ),
        cloudBackupPreferencesManager = cloudBackupPreferencesManager
    ) {
        var syncNowCalled = false
        override suspend fun syncNow(): BackupResult {
            syncNowCalled = true
            return BackupResult.Success
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
    fun `dashboard vehicles exposes latest mileage from maintenance repository`() = runTest {
        val context = FakeContext()
        val vehicleDao = FakeVehicleDao()
        val maintenanceDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val customRuleDao = FakeCustomRuleDao()

        val vehicleRepository = VehicleRepository(
            vehicleDao = vehicleDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(maintenanceDao)
        val fuelRepository = FuelRepository(fuelDao)
        val customMaintenanceRuleRepository = CustomMaintenanceRuleRepository(customRuleDao)
        val preferencesRepository = FakePreferencesRepository()
        val lubeLoggerCreds = FakeLubeLoggerCredentialsManager(context)
        val cloudPrefs = FakeCloudBackupPreferencesManager(context)
        val webDavRepo = FakeWebDavBackupRepository(context, cloudPrefs)

        val viewModel = DashboardViewModel(
            context = context,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            fuelRepository = fuelRepository,
            customMaintenanceRuleRepository = customMaintenanceRuleRepository,
            imageStorageManager = FakeImageStorageManager(),
            preferencesRepository = preferencesRepository,
            lubeLoggerCredentialsManager = lubeLoggerCreds,
            webDavBackupRepository = webDavRepo,
            cloudBackupPreferencesManager = cloudPrefs,
        )

        val collectJob = backgroundScope.launch { viewModel.vehicles.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.vehicles.value.size)
        assertEquals(null, viewModel.vehicles.value[0].latestMileage)

        // Simulate latest mileage updated from maintenance or fuel record to 45,000 miles
        maintenanceDao.latestMileageFlow.value = 45000
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(45000, viewModel.vehicles.value[0].latestMileage)

        // Simulate fuel update pushing latest mileage to 48,000 miles
        maintenanceDao.latestMileageFlow.value = 48000
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(48000, viewModel.vehicles.value[0].latestMileage)

        collectJob.cancel()
    }

    @Test
    fun `drivers license state reflects repository values and supports updates`() = runTest {
        val context = FakeContext()
        val vehicleDao = FakeVehicleDao()
        val maintenanceDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val customRuleDao = FakeCustomRuleDao()

        val vehicleRepository = VehicleRepository(
            vehicleDao = vehicleDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(maintenanceDao)
        val fuelRepository = FuelRepository(fuelDao)
        val customMaintenanceRuleRepository = CustomMaintenanceRuleRepository(customRuleDao)

        class TestPreferencesRepository : FakePreferencesRepository() {
            val numFlow = MutableStateFlow<String?>(null)
            val stateFlow = MutableStateFlow<String?>(null)
            val expFlow = MutableStateFlow<Long?>(null)
            val notesFlow = MutableStateFlow<String?>(null)

            override val driversLicenseNumber = numFlow
            override val driversLicenseState = stateFlow
            override val driversLicenseExpiration = expFlow
            override val driversLicenseNotes = notesFlow

            override suspend fun setDriversLicense(
                number: String?,
                state: String?,
                expiration: Long?,
                notes: String?,
                imageFront: String?,
                imageBack: String?,
            ) {
                numFlow.value = number
                stateFlow.value = state
                expFlow.value = expiration
                notesFlow.value = notes
            }
        }

        val testPrefsRepo = TestPreferencesRepository()
        val lubeLoggerCreds = FakeLubeLoggerCredentialsManager(context)
        val cloudPrefs = FakeCloudBackupPreferencesManager(context)
        val webDavRepo = FakeWebDavBackupRepository(context, cloudPrefs)

        val viewModel = DashboardViewModel(
            context = context,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            fuelRepository = fuelRepository,
            customMaintenanceRuleRepository = customMaintenanceRuleRepository,
            imageStorageManager = FakeImageStorageManager(),
            preferencesRepository = testPrefsRepo,
            lubeLoggerCredentialsManager = lubeLoggerCreds,
            webDavBackupRepository = webDavRepo,
            cloudBackupPreferencesManager = cloudPrefs,
        )

        val collectJob = backgroundScope.launch { viewModel.driversLicenseState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertTrue(viewModel.driversLicenseState.value.isEmpty)

        viewModel.onSaveDriversLicense("DL987654", "CA", 1_700_000_000_000L, "Class C", null, null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertFalse(viewModel.driversLicenseState.value.isEmpty)
        assertEquals("DL987654", viewModel.driversLicenseState.value.number)
        assertEquals("CA", viewModel.driversLicenseState.value.state)
        assertEquals(1_700_000_000_000L, viewModel.driversLicenseState.value.expiration)
        assertEquals("Class C", viewModel.driversLicenseState.value.notes)

        viewModel.onDeleteDriversLicense()
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertTrue(viewModel.driversLicenseState.value.isEmpty)

        collectJob.cancel()
    }

    @Test
    fun `refreshSync triggers webdav sync when cloud sync is enabled`() = runTest {
        val context = FakeContext()
        val vehicleDao = FakeVehicleDao()
        val maintenanceDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val customRuleDao = FakeCustomRuleDao()

        val vehicleRepository = VehicleRepository(
            vehicleDao = vehicleDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(maintenanceDao)
        val fuelRepository = FuelRepository(fuelDao)
        val customMaintenanceRuleRepository = CustomMaintenanceRuleRepository(customRuleDao)
        val preferencesRepository = FakePreferencesRepository()
        val lubeLoggerCreds = FakeLubeLoggerCredentialsManager(context)
        val cloudPrefs = FakeCloudBackupPreferencesManager(context)
        val webDavRepo = FakeWebDavBackupRepository(context, cloudPrefs)

        cloudPrefs.cloudSyncEnabledFlow.value = true

        val viewModel = DashboardViewModel(
            context = context,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            fuelRepository = fuelRepository,
            customMaintenanceRuleRepository = customMaintenanceRuleRepository,
            imageStorageManager = FakeImageStorageManager(),
            preferencesRepository = preferencesRepository,
            lubeLoggerCredentialsManager = lubeLoggerCreds,
            webDavBackupRepository = webDavRepo,
            cloudBackupPreferencesManager = cloudPrefs,
        )

        Assert.assertFalse(webDavRepo.syncNowCalled)
        Assert.assertFalse(viewModel.isRefreshing.value)

        viewModel.refreshSync()
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertTrue(webDavRepo.syncNowCalled)
        Assert.assertFalse(viewModel.isRefreshing.value)
    }
}
