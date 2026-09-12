package com.fearmikey.garage.ui.dashboard

import android.content.ContextWrapper
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
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

    private class FakeVinDecoderApi : VinDecoderApi {
        override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse = VinDecodeResponse(emptyList())
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

    private class FakePreferencesRepository : PreferencesRepository {
        override val unitsType: Flow<String> = MutableStateFlow("imperial")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.IMPERIAL)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val termsAccepted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)
        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setTermsAccepted(accepted: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
    }

    private class FakeImageStorageManager : ImageStorageManager(
        context = FakeContext()
    ) {
        override fun imageFile(filename: String): File = File(filename)
    }

    private class FakeContext : ContextWrapper(null)

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
        val vehicleDao = FakeVehicleDao()
        val maintenanceDao = FakeMaintenanceDao()

        val vehicleRepository = VehicleRepository(
            vehicleDao = vehicleDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(maintenanceDao)
        val preferencesRepository = FakePreferencesRepository()

        val viewModel = DashboardViewModel(
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            imageStorageManager = FakeImageStorageManager(),
            preferencesRepository = preferencesRepository,
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
}
