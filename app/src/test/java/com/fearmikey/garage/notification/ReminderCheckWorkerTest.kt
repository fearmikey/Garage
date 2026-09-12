package com.fearmikey.garage.notification

import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCheckWorkerTest {

    private class FakeVehicleDao : VehicleDao {
        val vehiclesFlow = MutableStateFlow<List<Vehicle>>(
            listOf(Vehicle(id = 1, make = "Honda", model = "Civic", year = 2020))
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehiclesFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehiclesFlow.map { list -> list.find { it.id == vehicleId } }
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehiclesFlow.value.find { it.id == vehicleId }
        override suspend fun upsert(vehicle: Vehicle): Long = 1L
        override suspend fun update(vehicle: Vehicle) {}
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val recordsFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        val fuelRecordsFlow = MutableStateFlow<List<FuelRecord>>(emptyList())

        override fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>> = recordsFlow
        override fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> =
            combine(recordsFlow, fuelRecordsFlow) { maint, fuel ->
                val maxMaint = maint.maxOfOrNull { it.mileage }
                val maxFuel = fuel.maxOfOrNull { it.mileage }
                listOfNotNull(maxMaint, maxFuel).maxOrNull()
            }

        override suspend fun upsert(record: MaintenanceRecord): Long = 1L
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private class FakeReminderDao : ReminderDao {
        val remindersFlow = MutableStateFlow<List<Reminder>>(
            listOf(
                Reminder(
                    id = 10,
                    vehicleId = 1,
                    taskName = "Oil Change Reminder",
                    dueMileage = 5000,
                    isCompleted = false,
                )
            )
        )

        override fun getRemindersForVehicle(vehicleId: Long): Flow<List<Reminder>> = remindersFlow
        override suspend fun getIncompleteReminders(): List<Reminder> = remindersFlow.value.filter { !it.isCompleted }
        override suspend fun upsert(reminder: Reminder): Long = 1L
        override suspend fun update(reminder: Reminder) {}
        override suspend fun delete(reminder: Reminder) {}
    }

    private class FakeCustomRuleDao : CustomMaintenanceRuleDao {
        val rulesFlow = MutableStateFlow<List<CustomMaintenanceRule>>(emptyList())
        override fun getForVehicle(vehicleId: Long): Flow<List<CustomMaintenanceRule>> = rulesFlow
        override suspend fun upsert(rule: CustomMaintenanceRule): Long = 1L
        override suspend fun delete(rule: CustomMaintenanceRule) {}
    }

    @Test
    fun `when fuel record exceeds reminder due mileage status becomes overdue`() = runTest {
        val maintenanceDao = FakeMaintenanceDao()
        val reminderDao = FakeReminderDao()

        val reminder = reminderDao.getIncompleteReminders().first()
        assertEquals(5000, reminder.dueMileage)

        // Before fuel log: mileage is null
        val statusBefore = ReminderRepository.computeStatus(reminder, null)
        assertEquals(ReminderStatus.OK, statusBefore)

        // User logs fuel with mileage 5200 (going over the 5000 mileage interval)
        maintenanceDao.fuelRecordsFlow.value = listOf(
            FuelRecord(
                id = 1,
                vehicleId = 1,
                date = System.currentTimeMillis(),
                mileage = 5200,
                gallons = 10.0,
                totalCost = 30.0,
                pricePerGallon = 3.0,
            )
        )

        val currentMileage = maintenanceDao.getLatestMileageForVehicle(1).first()
        assertEquals(5200, currentMileage)

        val statusAfter = ReminderRepository.computeStatus(reminder, currentMileage)
        assertEquals(ReminderStatus.OVERDUE, statusAfter)
    }

    @Test
    fun `worker constants are defined for unique work names`() {
        assertEquals("reminder-check", ReminderCheckWorker.UNIQUE_WORK_NAME)
        assertEquals("reminder-check-immediate", ReminderCheckWorker.IMMEDIATE_WORK_NAME)
    }
}
