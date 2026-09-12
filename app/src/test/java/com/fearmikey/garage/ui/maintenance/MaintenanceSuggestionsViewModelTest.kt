package com.fearmikey.garage.ui.maintenance

import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.VehicleRepository
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
class MaintenanceSuggestionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        val vehicleFlow = MutableStateFlow<Vehicle?>(
            Vehicle(
                id = 1L,
                make = "Honda",
                model = "Civic",
                year = 2021,
                drivetrain = Drivetrain.FWD,
            )
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow.map { listOfNotNull(it) }
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleFlow
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleFlow.value
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
        val recordsFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        override fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> =
            recordsFlow.map { list -> list.maxOfOrNull { it.mileage } }
        override suspend fun upsert(record: MaintenanceRecord): Long {
            val current = recordsFlow.value.toMutableList()
            current.add(record)
            recordsFlow.value = current
            return 1L
        }
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private class FakeReminderDao : ReminderDao {
        val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())
        override fun getRemindersForVehicle(vehicleId: Long): Flow<List<Reminder>> = remindersFlow
        override suspend fun getIncompleteReminders(): List<Reminder> = remindersFlow.value.filter { !it.isCompleted }
        override suspend fun upsert(reminder: Reminder): Long {
            val current = remindersFlow.value.toMutableList()
            current.add(reminder)
            remindersFlow.value = current
            return 1L
        }
        override suspend fun update(reminder: Reminder) {}
        override suspend fun delete(reminder: Reminder) {}
    }

    private class FakeCustomRuleDao : CustomMaintenanceRuleDao {
        val rulesFlow = MutableStateFlow<List<CustomMaintenanceRule>>(emptyList())
        override fun getForVehicle(vehicleId: Long): Flow<List<CustomMaintenanceRule>> = rulesFlow
        override suspend fun upsert(rule: CustomMaintenanceRule): Long = 1L
        override suspend fun delete(rule: CustomMaintenanceRule) {}
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `adding reminder removes suggestion from suggestions list`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceDao = FakeMaintenanceDao()
        val reminderDao = FakeReminderDao()
        val customRuleDao = FakeCustomRuleDao()

        val viewModel = MaintenanceSuggestionsViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            maintenanceRepository = MaintenanceRepository(maintenanceDao),
            reminderRepository = ReminderRepository(reminderDao),
            customMaintenanceRuleRepository = CustomMaintenanceRuleRepository(customRuleDao),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob = backgroundScope.launch { viewModel.suggestions.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val initialSuggestions = viewModel.suggestions.value
        val oilChangeSuggestion = initialSuggestions.find { it.rule.taskName == "Engine oil change" }
        assertTrue(oilChangeSuggestion != null)

        viewModel.addAsReminder(oilChangeSuggestion!!)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSuggestions = viewModel.suggestions.value
        assertFalse(updatedSuggestions.any { it.rule.taskName == "Engine oil change" })

        collectJob.cancel()
    }

    @Test
    fun `latestMileage is exposed and reflects max record mileage`() = runTest {
        val vehicleId = 1L
        val savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to vehicleId))

        val vehicleRepo = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )
        val maintenanceDao = FakeMaintenanceDao()
        val reminderDao = FakeReminderDao()
        val customRuleDao = FakeCustomRuleDao()

        val viewModel = MaintenanceSuggestionsViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepo,
            maintenanceRepository = MaintenanceRepository(maintenanceDao),
            reminderRepository = ReminderRepository(reminderDao),
            customMaintenanceRuleRepository = CustomMaintenanceRuleRepository(customRuleDao),
            preferencesRepository = FakePreferencesRepository(),
        )

        val collectJob1 = backgroundScope.launch { viewModel.suggestions.collect {} }
        val collectJob2 = backgroundScope.launch { viewModel.latestMileage.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.latestMileage.value)

        // Log maintenance at 12,000 miles
        val record = MaintenanceRecord(
            id = 1,
            vehicleId = vehicleId,
            date = System.currentTimeMillis(),
            mileage = 12000,
            description = "Engine oil change",
            cost = 50.0,
            category = MaintenanceCategory.FLUIDS,
            taskName = "Engine oil change",
        )
        viewModel.logMaintenance(record)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(12000, viewModel.latestMileage.value)

        val oilChangeSuggestion = viewModel.suggestions.value.find { it.rule.taskName == "Engine oil change" }
        assertEquals(12000, oilChangeSuggestion?.lastServiceMileage)
        assertEquals(17000, oilChangeSuggestion?.nextDueMileage)

        collectJob1.cancel()
        collectJob2.cancel()
    }
}
