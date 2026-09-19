package com.fearmikey.garage.ui.cost

import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CostOfOwnershipViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private class FakePreferencesRepository : PreferencesRepository {
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculates totals and category breakdowns correctly`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))

        val maintenanceDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()

        val maintenanceRepo = MaintenanceRepository(maintenanceDao)
        val fuelRepo = FuelRepository(fuelDao)
        val prefsRepo = FakePreferencesRepository()

        val now = System.currentTimeMillis()

        maintenanceDao.recordsFlow.value = listOf(
            MaintenanceRecord(
                id = 1,
                vehicleId = vehicleId,
                date = now,
                mileage = 15000,
                description = "Synthetic Oil Change",
                cost = 100.00,
                category = MaintenanceCategory.FLUIDS,
            ),
            MaintenanceRecord(
                id = 2,
                vehicleId = vehicleId,
                date = now - 86400000,
                mileage = 10000,
                description = "Front Brake Rotors",
                cost = 200.00,
                category = MaintenanceCategory.BRAKES,
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
            )
        )

        val viewModel = CostOfOwnershipViewModel(
            savedStateHandle = savedStateHandle,
            maintenanceRepository = maintenanceRepo,
            fuelRepository = fuelRepo,
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
        assertEquals(3, categories.size)

        val brakesCategory = categories.find { it.key == "BRAKES" }
        assertNotNull(brakesCategory)
        assertEquals(200.00, brakesCategory!!.totalCost, 0.001)

        val fuelCategory = categories.find { it.key == "FUEL" }
        assertNotNull(fuelCategory)
        assertEquals(50.00, fuelCategory!!.totalCost, 0.001)
    }

    @Test
    fun `time filters filter records correctly`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))

        val maintenanceDao = FakeMaintenanceDao()
        val fuelDao = FakeFuelDao()

        val maintenanceRepo = MaintenanceRepository(maintenanceDao)
        val fuelRepo = FuelRepository(fuelDao)
        val prefsRepo = FakePreferencesRepository()

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfThisYear = cal.timeInMillis

        cal.add(Calendar.YEAR, -1)
        val startOfLastYear = cal.timeInMillis

        val lastYearRecordDate = startOfLastYear + 86400000L
        val thisYearRecordDate = startOfThisYear + 86400000L

        maintenanceDao.recordsFlow.value = listOf(
            MaintenanceRecord(
                id = 1,
                vehicleId = vehicleId,
                date = thisYearRecordDate,
                mileage = 15000,
                description = "Oil Change",
                cost = 100.00,
                category = MaintenanceCategory.FLUIDS,
            ),
            MaintenanceRecord(
                id = 2,
                vehicleId = vehicleId,
                date = lastYearRecordDate,
                mileage = 10000,
                description = "Brakes",
                cost = 200.00,
                category = MaintenanceCategory.BRAKES,
            ),
        )

        val viewModel = CostOfOwnershipViewModel(
            savedStateHandle = savedStateHandle,
            maintenanceRepository = maintenanceRepo,
            fuelRepository = fuelRepo,
            preferencesRepository = prefsRepo,
        )

        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        // Default ALL_TIME includes both records
        assertEquals(300.00, viewModel.uiState.value.totalCost, 0.001)

        // THIS_YEAR includes only this year's record
        viewModel.setTimeFilter(TimeFilter.THIS_YEAR)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(100.00, viewModel.uiState.value.totalCost, 0.001)

        // LAST_YEAR includes only last year's record
        viewModel.setTimeFilter(TimeFilter.LAST_YEAR)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(200.00, viewModel.uiState.value.totalCost, 0.001)
    }
}
