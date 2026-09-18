package com.fearmikey.garage.ui.vehicle

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
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
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationInsuranceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        override fun getAllVehicles(): Flow<List<Vehicle>> = MutableStateFlow(emptyList())
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = MutableStateFlow(null)
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = null
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

    private class FakeVehicleRegistrationDao : VehicleRegistrationDao {
        val flow = MutableStateFlow<VehicleRegistrationInsurance?>(null)

        override fun getByVehicleId(vehicleId: Long): Flow<VehicleRegistrationInsurance?> = flow

        override suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance) {
            flow.value = registrationInsurance
        }

        override suspend fun deleteByVehicleId(vehicleId: Long) {
            flow.value = null
        }
    }

    private class FakeVinDecoderApi : VinDecoderApi {
        override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse = VinDecodeResponse(emptyList())
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
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(2)

        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setTermsAccepted(accepted: Boolean) {}
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
    fun `initial state is empty when no record exists`() = runTest(testDispatcher) {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val regDao = FakeVehicleRegistrationDao()

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = regDao,
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = RegistrationInsuranceViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.record)
        assertFalse(viewModel.uiState.value.isSheetOpen)

        collectJob.cancel()
    }

    @Test
    fun `saving registration and insurance details updates flow and repository`() = runTest(testDispatcher) {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val regDao = FakeVehicleRegistrationDao()

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = regDao,
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = RegistrationInsuranceViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddOrEditClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSheetOpen)

        viewModel.onLicensePlateChanged("7ABC123")
        viewModel.onRegistrationStateChanged("CA")
        viewModel.onInsuranceProviderChanged("Geico")
        viewModel.onPolicyNumberChanged("POL-100200")

        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSheetOpen)
        assertNotNull(regDao.flow.value)
        assertEquals("7ABC123", regDao.flow.value?.licensePlate)
        assertEquals("CA", regDao.flow.value?.registrationState)
        assertEquals("Geico", regDao.flow.value?.insuranceProvider)
        assertEquals("POL-100200", regDao.flow.value?.policyNumber)

        collectJob.cancel()
    }

    @Test
    fun `deleting registration and insurance details clears record`() = runTest(testDispatcher) {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val regDao = FakeVehicleRegistrationDao()
        regDao.flow.value = VehicleRegistrationInsurance(
            vehicleId = vehicleId,
            licensePlate = "7ABC123",
            insuranceProvider = "State Farm",
        )

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = regDao,
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = RegistrationInsuranceViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("7ABC123", viewModel.uiState.value.record?.licensePlate)

        viewModel.onDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(regDao.flow.value)

        collectJob.cancel()
    }

    @Test
    fun `saving inspection details updates repository`() = runTest(testDispatcher) {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val regDao = FakeVehicleRegistrationDao()

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = regDao,
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = RegistrationInsuranceViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddOrEditClicked()
        viewModel.onInspectionResultChanged("Passed")
        viewModel.onInspectionNotesChanged("Sticker #12345")

        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Passed", regDao.flow.value?.inspectionResult)
        assertEquals("Sticker #12345", regDao.flow.value?.inspectionNotes)

        collectJob.cancel()
    }

    @Test
    fun `deleting individual sections clears only that category`() = runTest(testDispatcher) {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))
        val regDao = FakeVehicleRegistrationDao()
        regDao.flow.value = VehicleRegistrationInsurance(
            vehicleId = vehicleId,
            licensePlate = "7ABC123",
            inspectionResult = "Passed",
            insuranceProvider = "Geico",
        )

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = regDao,
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = RegistrationInsuranceViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddOrEditClicked()
        viewModel.onDeleteRegistrationSection()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(regDao.flow.value?.licensePlate)
        assertEquals("Passed", regDao.flow.value?.inspectionResult)
        assertEquals("Geico", regDao.flow.value?.insuranceProvider)

        viewModel.onDeleteInspectionSection()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(regDao.flow.value?.inspectionResult)
        assertEquals("Geico", regDao.flow.value?.insuranceProvider)

        viewModel.onDeleteInsuranceSection()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(regDao.flow.value)

        collectJob.cancel()
    }
}
