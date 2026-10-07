package com.fearmikey.garage.ui.settings

import android.content.ContextWrapper
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LubeLoggerDuplicatesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeImageStorageManager : ImageStorageManager(ContextWrapper(null)) {
        override fun imageFile(filename: String): File = File(filename)
    }

    private class FakeVehicleDao : VehicleDao {
        val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
        override fun getAllVehicles() = vehiclesFlow
        override fun getVehicleById(vehicleId: Long) = MutableStateFlow(null)
        override suspend fun getVehicleByIdOnce(vehicleId: Long) = null
        override suspend fun upsert(vehicle: Vehicle) = 1L
        override suspend fun update(vehicle: Vehicle) {}
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakeFuelDao : FuelDao {
        val recordsFlow = MutableStateFlow<List<FuelRecord>>(emptyList())
        override fun getRecordsForVehicle(vehicleId: Long) = recordsFlow
        override suspend fun upsert(record: FuelRecord) = 1L
        override suspend fun update(record: FuelRecord) {}
        override suspend fun delete(record: FuelRecord) {
            recordsFlow.value = recordsFlow.value.filter { it.id != record.id }
        }
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val recordsFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        override fun getRecordsForVehicleByDate(vehicleId: Long) = recordsFlow
        override fun getRecordsForVehicleByMileage(vehicleId: Long) = recordsFlow
        override fun getLatestMileageForVehicle(vehicleId: Long) = MutableStateFlow(null)
        override suspend fun upsert(record: MaintenanceRecord) = 1L
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {
            recordsFlow.value = recordsFlow.value.filter { it.id != record.id }
        }
    }

    private class FakeModificationDao : ModificationDao {
        val modsFlow = MutableStateFlow<List<ModificationRecord>>(emptyList())
        override fun getModsForVehicle(vehicleId: Long) = modsFlow
        override suspend fun getModById(id: Long) = null
        override suspend fun upsert(mod: ModificationRecord) = 1L
        override suspend fun update(mod: ModificationRecord) {}
        override suspend fun delete(mod: ModificationRecord) {
            modsFlow.value = modsFlow.value.filter { it.id != mod.id }
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
    fun `detects and groups duplicate fuel and maintenance records`() = runTest {
        val vehicleDao = FakeVehicleDao()
        val fuelDao = FakeFuelDao()
        val maintDao = FakeMaintenanceDao()
        val modDao = FakeModificationDao()

        val imageStorageManager = FakeImageStorageManager()

        val vehicleRepo = VehicleRepository(vehicleDao, mockDummy(), mockDummy(), mockDummy(), mockDummy())
        val fuelRepo = FuelRepository(fuelDao)
        val maintRepo = MaintenanceRepository(maintDao)
        val modRepo = ModificationRepository(modDao, imageStorageManager)

        vehicleDao.vehiclesFlow.value = listOf(Vehicle(id = 1L, year = 2023, make = "Toyota", model = "Tacoma"))
        
        // Two identical oil changes
        val date = 1_700_000_000_000L
        maintDao.recordsFlow.value = listOf(
            MaintenanceRecord(id = 101L, vehicleId = 1L, date = date, mileage = 80000, description = "Oil Change", cost = 65.0, category = MaintenanceCategory.FLUIDS, lubeLoggerId = 12),
            MaintenanceRecord(id = 102L, vehicleId = 1L, date = date, mileage = 80000, description = "Oil Change", cost = 65.0, category = MaintenanceCategory.FLUIDS, lubeLoggerId = 13),
        )

        val viewModel = LubeLoggerDuplicatesViewModel(vehicleRepo, fuelRepo, maintRepo, modRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.groups.size)

        val group = state.groups.single()
        assertEquals("2023 Toyota Tacoma", group.vehicleName)
        assertEquals(2, group.items.size)

        // First item (original) is NOT selected for deletion; second item (duplicate) IS selected by default.
        assertFalse(group.items[0].isSelectedForDeletion)
        assertTrue(group.items[1].isSelectedForDeletion)

        // Delete selected duplicates
        viewModel.deleteSelectedDuplicates()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Deleted 1 duplicate record(s).", viewModel.uiState.value.userMessage)
        // Only original remains
        assertEquals(1, maintDao.recordsFlow.value.size)
        assertEquals(101L, maintDao.recordsFlow.value.single().id)
    }

    private inline fun <reified T : Any> mockDummy(): T =
        java.lang.reflect.Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> null } as T
}
