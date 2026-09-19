package com.fearmikey.garage

import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakePreferencesRepository : PreferencesRepository {
        val appOpenCountFlow = MutableStateFlow(0)
        val buyMeACoffeeDontAskAgainFlow = MutableStateFlow(false)
        val buyMeACoffeeNextPromptOpenCountFlow = MutableStateFlow(2)
        val onboardingCompletedFlow = MutableStateFlow(true)

        override val unitsType: Flow<String> = MutableStateFlow("metric")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = onboardingCompletedFlow
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = appOpenCountFlow
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = buyMeACoffeeDontAskAgainFlow
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = buyMeACoffeeNextPromptOpenCountFlow

        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
        override suspend fun setMaintenanceMileageWindow(miles: Int) {}
        override suspend fun incrementAppOpenCount(): Int {
            val newCount = appOpenCountFlow.value + 1
            appOpenCountFlow.value = newCount
            return newCount
        }
        override suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {
            buyMeACoffeeDontAskAgainFlow.value = dontAskAgain
        }
        override suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {
            buyMeACoffeeNextPromptOpenCountFlow.value = openCount
        }
    }

    private fun createFakeVehicleRepository(): VehicleRepository = VehicleRepository(
        vehicleDao = object : VehicleDao {
            override fun getAllVehicles(): Flow<List<Vehicle>> = MutableStateFlow(emptyList())
            override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = MutableStateFlow(null)
            override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = null
            override suspend fun upsert(vehicle: Vehicle): Long = 1L
            override suspend fun update(vehicle: Vehicle) {}
            override suspend fun delete(vehicle: Vehicle) {}
        },
        vehicleSpecsDao = object : VehicleSpecsDao {
            override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
            override suspend fun upsert(specs: VehicleSpecs) {}
        },
        vehiclePartsDao = object : VehiclePartsDao {
            override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
            override suspend fun upsert(info: VehiclePartsInfo) {}
        },
        vehicleRegistrationDao = object : VehicleRegistrationDao {
            override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
            override suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance) {}
            override suspend fun deleteByVehicleId(vehicleId: Long) {}
        },
        vinDecoderApi = object : VinDecoderApi {
            override suspend fun decodeVin(vin: String, format: String) =
                VinDecodeResponse(results = emptyList())
        }
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first open does not show buy me a coffee prompt`() = runTest {
        val prefsRepo = FakePreferencesRepository()
        val viewModel = MainViewModel(prefsRepo, createFakeVehicleRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showBuyMeACoffeePrompt.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, prefsRepo.appOpenCountFlow.value)
        assertFalse(viewModel.showBuyMeACoffeePrompt.value)
    }

    @Test
    fun `second open shows buy me a coffee prompt`() = runTest {
        val prefsRepo = FakePreferencesRepository().apply {
            appOpenCountFlow.value = 1 // next increment will make it 2
        }
        val viewModel = MainViewModel(prefsRepo, createFakeVehicleRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showBuyMeACoffeePrompt.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, prefsRepo.appOpenCountFlow.value)
        assertTrue(viewModel.showBuyMeACoffeePrompt.value)
    }

    @Test
    fun `dont ask again hides prompt permanently`() = runTest {
        val prefsRepo = FakePreferencesRepository().apply {
            appOpenCountFlow.value = 1
        }
        val viewModel = MainViewModel(prefsRepo, createFakeVehicleRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showBuyMeACoffeePrompt.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.showBuyMeACoffeePrompt.value)

        viewModel.onBuyMeACoffeeDontAskAgain()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(prefsRepo.buyMeACoffeeDontAskAgainFlow.value)
        assertFalse(viewModel.showBuyMeACoffeePrompt.value)
    }

    @Test
    fun `maybe later defers prompt until 4 opens later`() = runTest {
        val prefsRepo = FakePreferencesRepository().apply {
            appOpenCountFlow.value = 1 // becomes 2 on init
        }
        val viewModel = MainViewModel(prefsRepo, createFakeVehicleRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showBuyMeACoffeePrompt.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.showBuyMeACoffeePrompt.value)

        viewModel.onBuyMeACoffeeMaybeLater()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify next prompt open count is set to 2 + 4 = 6
        assertEquals(6, prefsRepo.buyMeACoffeeNextPromptOpenCountFlow.value)
        assertFalse(viewModel.showBuyMeACoffeePrompt.value)
    }
}
