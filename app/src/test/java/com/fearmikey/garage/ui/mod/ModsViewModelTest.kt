package com.fearmikey.garage.ui.mod

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeModificationDao : ModificationDao {
        val modsFlow = MutableStateFlow<List<ModificationRecord>>(emptyList())

        override fun getModsForVehicle(vehicleId: Long): Flow<List<ModificationRecord>> =
            modsFlow.map { list -> list.filter { it.vehicleId == vehicleId } }

        override suspend fun getModById(id: Long): ModificationRecord? =
            modsFlow.value.find { it.id == id }

        override suspend fun upsert(mod: ModificationRecord): Long {
            val current = modsFlow.value.toMutableList()
            val newId = if (mod.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else mod.id
            val updated = mod.copy(id = newId)
            current.removeAll { it.id == newId }
            current.add(updated)
            modsFlow.value = current
            return newId
        }

        override suspend fun update(mod: ModificationRecord) {
            upsert(mod)
        }

        override suspend fun delete(mod: ModificationRecord) {
            val current = modsFlow.value.toMutableList()
            current.removeAll { it.id == mod.id }
            modsFlow.value = current
        }
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

    private lateinit var fakeDao: FakeModificationDao
    private lateinit var repository: ModificationRepository
    private lateinit var imageStorageManager: ImageStorageManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeModificationDao()
        imageStorageManager = ImageStorageManager(ContextWrapper(null))
        repository = ModificationRepository(fakeDao, imageStorageManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads mods and calculates total cost`() = runTest(testDispatcher) {
        val initialMods = listOf(
            ModificationRecord(
                id = 1L,
                vehicleId = 10L,
                title = "Cold Air Intake",
                category = ModificationCategory.PERFORMANCE,
                description = "Added K&N Intake",
                cost = 350.0,
            ),
            ModificationRecord(
                id = 2L,
                vehicleId = 10L,
                title = "Exhaust System",
                category = ModificationCategory.EXHAUST,
                description = "Cat-back exhaust",
                cost = 650.0,
            ),
        )
        fakeDao.modsFlow.value = initialMods

        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 10L))
        val viewModel = ModsViewModel(
            savedStateHandle = savedStateHandle,
            modificationRepository = repository,
            imageStorageManager = imageStorageManager,
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.mods.size)
        assertEquals(1000.0, state.totalCost, 0.01)
        assertFalse(state.isSheetOpen)

        collectJob.cancel()
    }

    @Test
    fun `add and save new modification`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 10L))
        val viewModel = ModsViewModel(
            savedStateHandle = savedStateHandle,
            modificationRepository = repository,
            imageStorageManager = imageStorageManager,
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddModClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSheetOpen)
        assertNull(viewModel.uiState.value.editingModId)

        viewModel.onTitleChanged("2-Inch Lift Kit")
        viewModel.onCategoryChanged(ModificationCategory.SUSPENSION)
        viewModel.onDescriptionChanged("Fox 2.0 Coilovers")
        viewModel.onCostChanged("1200.00")

        viewModel.onSaveMod()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSheetOpen)
        assertEquals(1, viewModel.uiState.value.mods.size)

        val savedMod = viewModel.uiState.value.mods.first()
        assertEquals("2-Inch Lift Kit", savedMod.title)
        assertEquals(ModificationCategory.SUSPENSION, savedMod.category)
        assertEquals("Fox 2.0 Coilovers", savedMod.description)
        assertEquals(1200.00, savedMod.cost, 0.01)

        collectJob.cancel()
    }

    @Test
    fun `edit and delete modification`() = runTest(testDispatcher) {
        val existing = ModificationRecord(
            id = 5L,
            vehicleId = 10L,
            title = "LED Headlights",
            category = ModificationCategory.LIGHTING,
            description = "Plug and play LED bulb upgrade",
            cost = 150.0,
        )
        fakeDao.modsFlow.value = listOf(existing)

        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 10L))
        val viewModel = ModsViewModel(
            savedStateHandle = savedStateHandle,
            modificationRepository = repository,
            imageStorageManager = imageStorageManager,
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditModClicked(existing)
        testDispatcher.scheduler.advanceUntilIdle()

        val editState = viewModel.uiState.value
        assertTrue(editState.isSheetOpen)
        assertEquals(5L, editState.editingModId)
        assertEquals("LED Headlights", editState.title)

        viewModel.onDeleteMod(existing)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSheetOpen)
        assertTrue(viewModel.uiState.value.mods.isEmpty())

        collectJob.cancel()
    }
}
