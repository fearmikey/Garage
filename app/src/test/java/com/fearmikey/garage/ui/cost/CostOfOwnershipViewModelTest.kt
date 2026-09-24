package com.fearmikey.garage.ui.cost

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.ChargingDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.repository.ChargingRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CostOfOwnershipViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeImageStorageManager : ImageStorageManager(ContextWrapper(null)) {
        override fun imageFile(filename: String): File = File(filename)
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val recordsFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        override fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> = MutableStateFlow(null)
        override suspend fun upsert(record: MaintenanceRecord): Long = 1L
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private class FakeFuelDao : FuelDao {
        val recordsFlow = MutableStateFlow<List<FuelRecord>>(emptyList())
        override fun getRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> = recordsFlow
        override suspend fun upsert(record: FuelRecord): Long = 1L
        override suspend fun update(record: FuelRecord) {}
        override suspend fun delete(record: FuelRecord) {}
    }

    private class FakeChargingDao : ChargingDao {
        val recordsFlow = MutableStateFlow<List<ChargingRecord>>(emptyList())
        override fun getRecordsForVehicle(vehicleId: Long): Flow<List<ChargingRecord>> = recordsFlow
        override suspend fun upsert(record: ChargingRecord): Long = 1L
        override suspend fun update(record: ChargingRecord) {}
        override suspend fun delete(record: ChargingRecord) {}
    }

    private class FakeModificationDao : ModificationDao {
        val recordsFlow = MutableStateFlow<List<ModificationRecord>>(emptyList())
        override fun getModsForVehicle(vehicleId: Long): Flow<List<ModificationRecord>> = recordsFlow
        override suspend fun getModById(id: Long): ModificationRecord? = recordsFlow.value.find { it.id == id }
        override suspend fun upsert(mod: ModificationRecord): Long = 1L
        override suspend fun update(mod: ModificationRecord) {}
        override suspend fun delete(mod: ModificationRecord) {}
    }

    private class FakePreferencesRepository : PreferencesRepository {
        val includeModsFlow = MutableStateFlow(false)
        override val unitsType: Flow<String> = MutableStateFlow("imperial")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.IMPERIAL)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val maintenanceDaysWindow: Flow<Int> = MutableStateFlow(10)
        override val includeModsInCost: Flow<Boolean> = includeModsFlow
        override val affiliateLinksEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(10)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)

        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
        override suspend fun setMaintenanceMileageWindow(miles: Int) {}
        override suspend fun setMaintenanceDaysWindow(days: Int) {}
        override suspend fun setIncludeModsInCost(includeMods: Boolean) {
            includeModsFlow.value = includeMods
        }
        override suspend fun setAffiliateLinksEnabled(enabled: Boolean) {}
        override suspend fun incrementAppOpenCount(): Int = 1
        override suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {}
        override suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {}
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
    fun `uiState calculates totals correctly for ALL_TIME without mods`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf("vehicleId" to vehicleId))

        val maintDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val chargingDao = FakeChargingDao()
        val modDao = FakeModificationDao()
        val imageStorageManager = FakeImageStorageManager()

        val maintenanceRepo = MaintenanceRepository(maintDao)
        val fuelRepo = FuelRepository(fuelDao)
        val chargingRepo = ChargingRepository(chargingDao)
        val modRepo = ModificationRepository(modDao, imageStorageManager)
        val prefsRepo = FakePreferencesRepository()

        val now = System.currentTimeMillis()

        maintDao.recordsFlow.value = listOf(
            MaintenanceRecord(
                id = 1,
                vehicleId = vehicleId,
                date = now,
                mileage = 10000,
                description = "Oil Change",
                cost = 100.00,
                category = MaintenanceCategory.FLUIDS,
            ),
            MaintenanceRecord(
                id = 2,
                vehicleId = vehicleId,
                date = now,
                mileage = 12000,
                description = "New Tires",
                cost = 200.00,
                category = MaintenanceCategory.TIRES,
            ),
        )

        fuelDao.recordsFlow.value = listOf(
            FuelRecord(
                id = 1,
                vehicleId = vehicleId,
                date = now,
                mileage = 15000,
                gallons = 15.0,
                totalCost = 50.00,
                pricePerGallon = 3.333,
            ),
        )

        val viewModel = CostOfOwnershipViewModel(
            savedStateHandle = savedStateHandle,
            maintenanceRepository = maintenanceRepo,
            fuelRepository = fuelRepo,
            chargingRepository = chargingRepo,
            modificationRepository = modRepo,
            preferencesRepository = prefsRepo,
        )

        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(350.00, state.totalCost, 0.001)
        assertEquals(300.00, state.maintenanceCost, 0.001)
        assertEquals(50.00, state.fuelCost, 0.001)
        assertEquals(2, state.maintenanceRecordCount)
        assertEquals(1, state.fuelRecordCount)

        val categories = state.categories
        assertNotNull(categories)

        collectJob.cancel()
    }

    @Test
    fun `toggleIncludeMods includes modifications in total cost`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf("vehicleId" to vehicleId))

        val maintDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val chargingDao = FakeChargingDao()
        val modDao = FakeModificationDao()
        val imageStorageManager = FakeImageStorageManager()

        val maintenanceRepo = MaintenanceRepository(maintDao)
        val fuelRepo = FuelRepository(fuelDao)
        val chargingRepo = ChargingRepository(chargingDao)
        val modRepo = ModificationRepository(modDao, imageStorageManager)
        val prefsRepo = FakePreferencesRepository()

        val now = System.currentTimeMillis()

        maintDao.recordsFlow.value = listOf(
            MaintenanceRecord(
                id = 1,
                vehicleId = vehicleId,
                date = now,
                mileage = 10000,
                description = "Oil Change",
                cost = 100.00,
                category = MaintenanceCategory.FLUIDS,
            ),
        )

        modDao.recordsFlow.value = listOf(
            ModificationRecord(
                id = 1,
                vehicleId = vehicleId,
                title = "Exhaust System",
                description = "Cat-back exhaust",
                cost = 500.00,
                category = ModificationCategory.PERFORMANCE,
                date = now,
            ),
        )

        val viewModel = CostOfOwnershipViewModel(
            savedStateHandle = savedStateHandle,
            maintenanceRepository = maintenanceRepo,
            fuelRepository = fuelRepo,
            chargingRepository = chargingRepo,
            modificationRepository = modRepo,
            preferencesRepository = prefsRepo,
        )

        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100.00, viewModel.uiState.value.totalCost, 0.001)

        viewModel.toggleIncludeMods(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(600.00, viewModel.uiState.value.totalCost, 0.001)

        collectJob.cancel()
    }

    @Test
    fun `setTimeFilter filters records by date`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf("vehicleId" to vehicleId))

        val maintDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()
        val chargingDao = FakeChargingDao()
        val modDao = FakeModificationDao()
        val imageStorageManager = FakeImageStorageManager()

        val maintenanceRepo = MaintenanceRepository(maintDao)
        val fuelRepo = FuelRepository(fuelDao)
        val chargingRepo = ChargingRepository(chargingDao)
        val modRepo = ModificationRepository(modDao, imageStorageManager)
        val prefsRepo = FakePreferencesRepository()

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -2)
        val oldDate = cal.timeInMillis

        maintDao.recordsFlow.value = listOf(
            MaintenanceRecord(
                id = 1,
                vehicleId = vehicleId,
                date = now,
                mileage = 10000,
                description = "Recent Oil Change",
                cost = 100.00,
                category = MaintenanceCategory.FLUIDS,
            ),
            MaintenanceRecord(
                id = 2,
                vehicleId = vehicleId,
                date = oldDate,
                mileage = 5000,
                description = "Old Oil Change",
                cost = 80.00,
                category = MaintenanceCategory.FLUIDS,
            ),
        )

        val viewModel = CostOfOwnershipViewModel(
            savedStateHandle = savedStateHandle,
            maintenanceRepository = maintenanceRepo,
            fuelRepository = fuelRepo,
            chargingRepository = chargingRepo,
            modificationRepository = modRepo,
            preferencesRepository = prefsRepo,
        )

        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(180.00, viewModel.uiState.value.totalCost, 0.001)

        viewModel.setTimeFilter(TimeFilter.THIS_YEAR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100.00, viewModel.uiState.value.totalCost, 0.001)

        collectJob.cancel()
    }
}
