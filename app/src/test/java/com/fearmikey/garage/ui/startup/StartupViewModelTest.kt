package com.fearmikey.garage.ui.startup

import android.content.ContextWrapper
import com.fearmikey.garage.data.repository.PreferencesRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.fearmikey.garage.ui.util.AppCurrency

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestContext : ContextWrapper(null) {
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return 0
        }

        override fun checkSelfPermission(permission: String): Int {
            return 0
        }
    }

    private class FakePreferencesRepository : PreferencesRepository {
        val unitsTypeFlow = MutableStateFlow("metric")
        val currencyCodeFlow = MutableStateFlow("USD")
        val themeTypeFlow = MutableStateFlow("system")
        val amoledDarkFlow = MutableStateFlow(false)
        val dynamicColorFlow = MutableStateFlow(true)
        val appPaletteFlow = MutableStateFlow("default")
        val onboardingCompletedFlow = MutableStateFlow(false)
        val defaultVehicleIdFlow = MutableStateFlow<Long?>(null)

        var savedUnits: String? = null
        var savedCurrency: String? = null
        var savedTheme: String? = null
        var savedAmoledDark: Boolean? = null
        var savedDynamicColor: Boolean? = null
        var savedPalette: String? = null
        var isCompleted: Boolean = false

        override val unitsType: Flow<String> = unitsTypeFlow
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        override val currencyCode: Flow<String> = currencyCodeFlow
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = themeTypeFlow
        override val amoledDark: Flow<Boolean> = amoledDarkFlow
        override val dynamicColor: Flow<Boolean> = dynamicColorFlow
        override val appPalette: Flow<String> = appPaletteFlow
        override val onboardingCompleted: Flow<Boolean> = onboardingCompletedFlow
        override val defaultVehicleId: Flow<Long?> = defaultVehicleIdFlow
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(2)

        override suspend fun setUnitsType(units: String) {
            savedUnits = units
            unitsTypeFlow.value = units
        }

        override suspend fun setCurrencyCode(currencyCode: String) {
            savedCurrency = currencyCode
            currencyCodeFlow.value = currencyCode
        }

        override suspend fun setThemeType(theme: String) {
            savedTheme = theme
            themeTypeFlow.value = theme
        }

        override suspend fun setAmoledDark(enabled: Boolean) {
            savedAmoledDark = enabled
            amoledDarkFlow.value = enabled
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            savedDynamicColor = enabled
            dynamicColorFlow.value = enabled
        }

        override suspend fun setAppPalette(palette: String) {
            savedPalette = palette
            appPaletteFlow.value = palette
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            isCompleted = completed
            onboardingCompletedFlow.value = completed
        }

        override suspend fun setDefaultVehicleId(vehicleId: Long?) {
            defaultVehicleIdFlow.value = vehicleId
        }

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
    fun `selectUnits updates uiState`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectUnits("imperial")
        assertEquals("imperial", viewModel.uiState.value.selectedUnits)
    }

    @Test
    fun `selectCurrency updates uiState`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("USD", viewModel.uiState.value.selectedCurrency)
        viewModel.selectCurrency("EUR")
        assertEquals("EUR", viewModel.uiState.value.selectedCurrency)
    }

    @Test
    fun `theme and appearance selections update uiState and repository`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTheme("dark")
        viewModel.selectPalette("racing_red")
        viewModel.toggleDynamicColor(false)
        viewModel.toggleAmoledDark(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("dark", viewModel.uiState.value.selectedTheme)
        assertEquals("racing_red", viewModel.uiState.value.selectedPalette)
        assertEquals(false, viewModel.uiState.value.selectedDynamicColor)
        assertEquals(true, viewModel.uiState.value.selectedAmoledDark)

        assertEquals("dark", fakeRepo.savedTheme)
        assertEquals("racing_red", fakeRepo.savedPalette)
        assertEquals(false, fakeRepo.savedDynamicColor)
        assertEquals(true, fakeRepo.savedAmoledDark)
    }

    @Test
    fun `completeStartup saves units, currency, and marks onboarding as completed`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectUnits("imperial")
        viewModel.selectCurrency("GBP")
        viewModel.selectTheme("dark")
        viewModel.selectPalette("tuner_amber")

        var finishedCalled = false
        viewModel.completeStartup {
            finishedCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("imperial", fakeRepo.savedUnits)
        assertEquals("GBP", fakeRepo.savedCurrency)
        assertEquals("dark", fakeRepo.savedTheme)
        assertEquals("tuner_amber", fakeRepo.savedPalette)
        assertTrue(fakeRepo.isCompleted)
        assertTrue(finishedCalled)
    }
}