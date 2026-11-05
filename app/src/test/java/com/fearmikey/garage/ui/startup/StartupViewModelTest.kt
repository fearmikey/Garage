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
import com.fearmikey.garage.ui.vehicle.VehicleTab

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
        var savedShowFuelTrendGraph: Boolean? = null
        var savedShowFleetOverview: Boolean? = null
        var savedDefaultHiddenTabs: String? = null
        val defaultHiddenTabsFlow = MutableStateFlow("")

        override val defaultHiddenTabs: Flow<String> = defaultHiddenTabsFlow

        override suspend fun setShowFuelTrendGraph(enabled: Boolean) {
            savedShowFuelTrendGraph = enabled
        }

        override suspend fun setShowFleetOverview(enabled: Boolean) {
            savedShowFleetOverview = enabled
        }

        override suspend fun setDefaultHiddenTabs(raw: String) {
            savedDefaultHiddenTabs = raw
            defaultHiddenTabsFlow.value = raw
        }

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

    @Test
    fun `step navigation moves forward, backward and clamps at the ends`() = runTest {
        val viewModel = StartupViewModel(TestContext(), FakePreferencesRepository())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)
        viewModel.previousStep()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(OnboardingStep.APPEARANCE, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.REGION, viewModel.uiState.value.currentStep)

        viewModel.skipToFinish()
        assertEquals(OnboardingStep.FINISH, viewModel.uiState.value.currentStep)
        viewModel.nextStep()
        assertEquals(OnboardingStep.FINISH, viewModel.uiState.value.currentStep)

        viewModel.goToStep(OnboardingStep.SCREENS)
        assertEquals(OnboardingStep.SCREENS, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `default hidden tabs load from preferences and ignore non-hideable tabs`() = runTest {
        val fakeRepo = FakePreferencesRepository().apply { defaultHiddenTabsFlow.value = "MODS,TRIPS" }
        val viewModel = StartupViewModel(TestContext(), fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf(VehicleTab.MODS, VehicleTab.TRIPS), viewModel.uiState.value.defaultHiddenTabs)

        viewModel.setTabVisible(VehicleTab.MODS, visible = true)
        viewModel.setTabVisible(VehicleTab.RECALLS, visible = false)
        viewModel.setTabVisible(VehicleTab.TIMELINE, visible = false)
        assertEquals(setOf(VehicleTab.TRIPS, VehicleTab.RECALLS), viewModel.uiState.value.defaultHiddenTabs)

        viewModel.setDefaultHiddenTabs(setOf(VehicleTab.TIMELINE, VehicleTab.SPECS))
        assertEquals(setOf(VehicleTab.SPECS), viewModel.uiState.value.defaultHiddenTabs)
    }

    @Test
    fun `completeStartup persists dashboard and default tab choices`() = runTest {
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(TestContext(), fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFuelTrendGraph(false)
        viewModel.toggleFleetOverview(true)
        val allHideable = VehicleTab.entries.filter { it.canHide }.toSet()
        viewModel.setDefaultHiddenTabs(allHideable - ESSENTIAL_VEHICLE_TABS)

        viewModel.completeStartup {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, fakeRepo.savedShowFuelTrendGraph)
        assertEquals(true, fakeRepo.savedShowFleetOverview)
        assertEquals(
            setOf(VehicleTab.SPECS, VehicleTab.PARTS, VehicleTab.MODS, VehicleTab.RECALLS, VehicleTab.KNOWLEDGE, VehicleTab.TRIPS),
            VehicleTab.parseHidden(fakeRepo.savedDefaultHiddenTabs),
        )
        assertTrue(fakeRepo.isCompleted)
    }
}