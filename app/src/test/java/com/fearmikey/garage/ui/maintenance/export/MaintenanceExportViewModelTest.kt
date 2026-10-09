package com.fearmikey.garage.ui.maintenance.export

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.ChargingDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.dao.RecallCampaignStateDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.entity.RecallCampaignState
import com.fearmikey.garage.data.repository.RecallStateRepository
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.RecallApi
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.RecallDto
import com.fearmikey.garage.data.remote.dto.RecallResponse
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.ChargingRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.RecallRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.notification.PdfExportNotifier
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
import kotlinx.coroutines.test.advanceUntilIdle
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
class MaintenanceExportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        val vehicleFlow = MutableStateFlow<List<Vehicle>>(
            listOf(
                Vehicle(
                    id = 1L,
                    make = "Toyota",
                    model = "Camry",
                    year = 2020,
                    vin = "4T1B11HK8LU123456",
                ),
            ),
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleFlow.map { list -> list.find { it.id == vehicleId } }
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleFlow.value.find { it.id == vehicleId }
        override suspend fun upsert(vehicle: Vehicle): Long = 1L
        override suspend fun update(vehicle: Vehicle) {}
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakeVehicleSpecsDao : VehicleSpecsDao {
        private val specs = VehicleSpecs(
            vehicleId = 1L,
            engineCylinders = "4",
            displacementL = "2.5",
            engineHp = "203",
            fuelType = "Gasoline",
        )
        override fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?> = MutableStateFlow(specs)
        override suspend fun getByVehicleIdOnce(vehicleId: Long): VehicleSpecs? = specs
        override suspend fun upsert(specs: VehicleSpecs) {}
    }

    private class FakeVehiclePartsDao : VehiclePartsDao {
        override fun getByVehicleId(vehicleId: Long): Flow<VehiclePartsInfo?> = MutableStateFlow(
            VehiclePartsInfo(
                vehicleId = 1L,
                oilViscosity = "0W-20",
                oilCapacity = "4.5 qts",
            ),
        )
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

    private class FakeMaintenanceDao : MaintenanceDao {
        val records = MutableStateFlow(
            listOf(
                MaintenanceRecord(
                    id = 10L,
                    vehicleId = 1L,
                    date = 1600000000000L,
                    mileage = 15000,
                    description = "Oil Change & Filter",
                    cost = 75.0,
                    category = MaintenanceCategory.FLUIDS,
                    taskName = "Oil change",
                ),
            ),
        )

        override fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>> = records
        override fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>> = records
        override fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> = MutableStateFlow(15000)
        override suspend fun upsert(record: MaintenanceRecord): Long = record.id
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private class FakeModificationDao : ModificationDao {
        val mods = MutableStateFlow(
            listOf(
                ModificationRecord(
                    id = 100L,
                    vehicleId = 1L,
                    title = "Cat-back Exhaust",
                    category = ModificationCategory.EXHAUST,
                    description = "Stainless steel exhaust system",
                    cost = 650.0,
                ),
            ),
        )
        override fun getModsForVehicle(vehicleId: Long): Flow<List<ModificationRecord>> = mods
        override suspend fun getModById(id: Long): ModificationRecord? = mods.value.find { it.id == id }
        override suspend fun upsert(mod: ModificationRecord): Long = mod.id
        override suspend fun update(mod: ModificationRecord) {}
        override suspend fun delete(mod: ModificationRecord) {}
    }

    private class FakeFuelDao : FuelDao {
        override fun getRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> = MutableStateFlow(emptyList())
        override suspend fun upsert(record: FuelRecord): Long = 1L
        override suspend fun update(record: FuelRecord) {}
        override suspend fun delete(record: FuelRecord) {}
    }

    private class FakeChargingDao : ChargingDao {
        override fun getRecordsForVehicle(vehicleId: Long): Flow<List<ChargingRecord>> = MutableStateFlow(emptyList())
        override suspend fun upsert(record: ChargingRecord): Long = 1L
        override suspend fun update(record: ChargingRecord) {}
        override suspend fun delete(record: ChargingRecord) {}
    }

    private class FakeRecallApi : RecallApi {
        override suspend fun getRecalls(make: String, model: String, modelYear: Int): RecallResponse = RecallResponse(
            count = 1,
            message = "Results returned",
            results = listOf(
                RecallDto(
                    campaignNumber = "20V123000",
                    component = "FUEL SYSTEM",
                    summary = "Fuel pump software defect",
                    consequence = "Engine stall",
                    remedy = "Update software at dealer",
                    reportReceivedDate = "2020-03-15",
                ),
            ),
        )
    }

    private class FakeRecallCampaignStateDao : RecallCampaignStateDao {
        private val states = MutableStateFlow<List<RecallCampaignState>>(emptyList())
        override fun getStatesForVehicle(vehicleId: Long) = states
        override suspend fun saveState(state: RecallCampaignState) {
            states.value = states.value.filterNot { it.campaignNumber == state.campaignNumber && it.vehicleId == state.vehicleId } + state
        }
    }

    private open class FakePreferencesRepository : PreferencesRepository {
        override val unitsType: Flow<String> = MutableStateFlow("imperial")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.IMPERIAL)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(1L)
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(5)

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
    fun `uiState emits complete vehicle data and total care investment calculation`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L))
        val vehicleRepository = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(FakeMaintenanceDao())
        val modificationRepository = ModificationRepository(
            modificationDao = FakeModificationDao(),
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
        )
        val fuelRepository = FuelRepository(FakeFuelDao())
        val chargingRepository = ChargingRepository(FakeChargingDao())
        val recallRepository = RecallRepository(FakeRecallApi())
        val recallStateRepository = RecallStateRepository(FakeRecallCampaignStateDao())
        val preferencesRepository = FakePreferencesRepository()
        val pdfExportNotifier = PdfExportNotifier(ContextWrapper(null))

        val viewModel = MaintenanceExportViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            modificationRepository = modificationRepository,
            fuelRepository = fuelRepository,
            chargingRepository = chargingRepository,
            recallRepository = recallRepository,
            recallStateRepository = recallStateRepository,
            preferencesRepository = preferencesRepository,
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
            pdfExportNotifier = pdfExportNotifier,
        )

        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Toyota", state.vehicle?.make)
        assertEquals("Camry", state.vehicle?.model)
        assertEquals(1, state.maintenanceRecords.size)
        assertEquals(75.0, state.totalMaintenanceCost, 0.01)
        assertEquals(650.0, state.totalModificationCost, 0.01)
        assertEquals(725.0, state.totalCareInvestment, 0.01)
        assertEquals("0W-20", state.parts?.oilViscosity)
        assertEquals("2.5", state.specs?.displacementL)
        assertEquals(1, state.recalls.size)
        assertEquals("20V123000", state.recalls.first().campaignNumber)

        job.cancel()
    }

    @Test
    fun `toggling inclusions correctly updates ExportUiState`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L))
        val vehicleRepository = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(FakeMaintenanceDao())
        val modificationRepository = ModificationRepository(
            modificationDao = FakeModificationDao(),
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
        )
        val fuelRepository = FuelRepository(FakeFuelDao())
        val chargingRepository = ChargingRepository(FakeChargingDao())
        val recallRepository = RecallRepository(FakeRecallApi())
        val recallStateRepository = RecallStateRepository(FakeRecallCampaignStateDao())
        val preferencesRepository = FakePreferencesRepository()
        val pdfExportNotifier = PdfExportNotifier(ContextWrapper(null))

        val viewModel = MaintenanceExportViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            modificationRepository = modificationRepository,
            fuelRepository = fuelRepository,
            chargingRepository = chargingRepository,
            recallRepository = recallRepository,
            recallStateRepository = recallStateRepository,
            preferencesRepository = preferencesRepository,
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
            pdfExportNotifier = pdfExportNotifier,
        )

        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.includeMods)
        assertEquals(725.0, viewModel.uiState.value.totalCareInvestment, 0.01)

        viewModel.toggleIncludeMods(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.includeMods)
        assertEquals(0.0, viewModel.uiState.value.totalModificationCost, 0.01)
        assertEquals(75.0, viewModel.uiState.value.totalCareInvestment, 0.01)

        job.cancel()
    }

    @Test
    fun `toggling and resolving recall campaign updates openRecalls list`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L))
        val vehicleRepository = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(FakeMaintenanceDao())
        val modificationRepository = ModificationRepository(
            modificationDao = FakeModificationDao(),
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
        )
        val fuelRepository = FuelRepository(FakeFuelDao())
        val chargingRepository = ChargingRepository(FakeChargingDao())
        val recallRepository = RecallRepository(FakeRecallApi())
        val recallStateDao = FakeRecallCampaignStateDao()
        recallStateDao.saveState(RecallCampaignState(1L, "20V123000", com.fearmikey.garage.data.local.entity.RecallState.SERVICED))
        val recallStateRepository = RecallStateRepository(recallStateDao)
        val preferencesRepository = FakePreferencesRepository()
        val pdfExportNotifier = PdfExportNotifier(ContextWrapper(null))

        val viewModel = MaintenanceExportViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            modificationRepository = modificationRepository,
            fuelRepository = fuelRepository,
            chargingRepository = chargingRepository,
            recallRepository = recallRepository,
            recallStateRepository = recallStateRepository,
            preferencesRepository = preferencesRepository,
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
            pdfExportNotifier = pdfExportNotifier,
        )

        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recalls.size)
        // By default, recalls are unchecked (0 open / serviced)
        assertEquals(0, viewModel.uiState.value.openRecalls.size)

        // Mark recall campaign as open / unaddressed
        viewModel.toggleRecallCampaignOpen("20V123000", true)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recalls.size)
        assertEquals(1, viewModel.uiState.value.openRecalls.size)

        // Mark all resolved
        viewModel.markAllRecallsResolved()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.openRecalls.size)

        job.cancel()
    }

    @Test
    fun `export uiState inherits metric unit system from preferences`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L))
        val vehicleRepository = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceRepository = MaintenanceRepository(FakeMaintenanceDao())
        val modificationRepository = ModificationRepository(
            modificationDao = FakeModificationDao(),
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
        )
        val fuelRepository = FuelRepository(FakeFuelDao())
        val chargingRepository = ChargingRepository(FakeChargingDao())
        val recallRepository = RecallRepository(FakeRecallApi())
        val recallStateRepository = RecallStateRepository(FakeRecallCampaignStateDao())
        val preferencesRepository = object : FakePreferencesRepository() {
            override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        }
        val pdfExportNotifier = PdfExportNotifier(ContextWrapper(null))

        val viewModel = MaintenanceExportViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            modificationRepository = modificationRepository,
            fuelRepository = fuelRepository,
            chargingRepository = chargingRepository,
            recallRepository = recallRepository,
            recallStateRepository = recallStateRepository,
            preferencesRepository = preferencesRepository,
            imageStorageManager = ImageStorageManager(ContextWrapper(null)),
            pdfExportNotifier = pdfExportNotifier,
        )

        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(UnitSystem.METRIC, viewModel.uiState.value.unitSystem)

        job.cancel()
    }
}
