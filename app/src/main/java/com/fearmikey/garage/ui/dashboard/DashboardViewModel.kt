package com.fearmikey.garage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class VehicleListItem(
    val vehicle: Vehicle,
    val latestMileage: Int?,
    val imageFile: File?,
    val avgMpg: Double? = null,
    val overdueReminderCount: Int = 0,
    val upcomingReminderCount: Int = 0,
    val fuelEntries: List<FuelEconomyEntry> = emptyList(),
)

data class FleetSummary(
    val totalVehicles: Int = 0,
    val fleetAvgMpg: Double? = null,
    val totalOverdueReminders: Int = 0,
    val totalUpcomingReminders: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    maintenanceRepository: MaintenanceRepository,
    fuelRepository: FuelRepository,
    reminderRepository: ReminderRepository,
    imageStorageManager: ImageStorageManager,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val vehicles: StateFlow<List<VehicleListItem>> = vehicleRepository.getAllVehicles()
        .flatMapLatest { vehicles ->
            if (vehicles.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    vehicles.map { vehicle ->
                        combine(
                            maintenanceRepository.getLatestMileageForVehicle(vehicle.id),
                            fuelRepository.getRecordsForVehicle(vehicle.id),
                            reminderRepository.getRemindersForVehicle(vehicle.id),
                        ) { mileage, fuelRecords, reminders ->
                            val fuelEntries = FuelEconomyCalculator.entriesFor(fuelRecords)
                            val avgMpg = FuelEconomyCalculator.averageMpg(fuelEntries)

                            val reminderStatuses = reminders.map { reminder ->
                                ReminderRepository.computeStatus(reminder, mileage)
                            }
                            val overdueCount = reminderStatuses.count { it == ReminderStatus.OVERDUE }
                            val upcomingCount = reminderStatuses.count { it == ReminderStatus.UPCOMING }

                            VehicleListItem(
                                vehicle = vehicle,
                                latestMileage = mileage,
                                imageFile = vehicle.imageUri?.let { imageStorageManager.imageFile(it) },
                                avgMpg = avgMpg,
                                overdueReminderCount = overdueCount,
                                upcomingReminderCount = upcomingCount,
                                fuelEntries = fuelEntries,
                            )
                        }
                    }
                ) { items -> items.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val fleetSummary: StateFlow<FleetSummary> = vehicles
        .map { items ->
            if (items.isEmpty()) {
                FleetSummary()
            } else {
                val allEntries = items.flatMap { it.fuelEntries }
                FleetSummary(
                    totalVehicles = items.size,
                    fleetAvgMpg = FuelEconomyCalculator.averageMpg(allEntries),
                    totalOverdueReminders = items.sumOf { it.overdueReminderCount },
                    totalUpcomingReminders = items.sumOf { it.upcomingReminderCount },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FleetSummary())
}

