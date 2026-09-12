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
        val onboardingCompletedFlow = MutableStateFlow(false)
        val termsAcceptedFlow = MutableStateFlow(false)
        val defaultVehicleIdFlow = MutableStateFlow<Long?>(null)

        var savedUnits: String? = null
        var savedCurrency: String? = null
        var isCompleted: Boolean = false
        var isTermsAccepted: Boolean = false

        override val unitsType: Flow<String> = unitsTypeFlow
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        override val currencyCode: Flow<String> = currencyCodeFlow
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = themeTypeFlow
        override val onboardingCompleted: Flow<Boolean> = onboardingCompletedFlow
        override val termsAccepted: Flow<Boolean> = termsAcceptedFlow
        override val defaultVehicleId: Flow<Long?> = defaultVehicleIdFlow

        override suspend fun setUnitsType(units: String) {
            savedUnits = units
            unitsTypeFlow.value = units
        }

        override suspend fun setCurrencyCode(currencyCode: String) {
            savedCurrency = currencyCode
            currencyCodeFlow.value = currencyCode
        }

        override suspend fun setThemeType(theme: String) {
            themeTypeFlow.value = theme
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            isCompleted = completed
            onboardingCompletedFlow.value = completed
        }

        override suspend fun setTermsAccepted(accepted: Boolean) {
            isTermsAccepted = accepted
            termsAcceptedFlow.value = accepted
        }

        override suspend fun setDefaultVehicleId(vehicleId: Long?) {
            defaultVehicleIdFlow.value = vehicleId
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
    fun `setTermsAccepted updates uiState`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.termsAccepted)
        viewModel.setTermsAccepted(true)
        assertEquals(true, viewModel.uiState.value.termsAccepted)
    }

    @Test
    fun `completeStartup saves units, currency, terms accepted, and marks onboarding as completed`() = runTest {
        val context = TestContext()
        val fakeRepo = FakePreferencesRepository()
        val viewModel = StartupViewModel(context, fakeRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectUnits("imperial")
        viewModel.selectCurrency("GBP")
        viewModel.setTermsAccepted(true)

        var finishedCalled = false
        viewModel.completeStartup {
            finishedCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("imperial", fakeRepo.savedUnits)
        assertEquals("GBP", fakeRepo.savedCurrency)
        assertTrue(fakeRepo.isTermsAccepted)
        assertTrue(fakeRepo.isCompleted)
        assertTrue(finishedCalled)
    }
}
