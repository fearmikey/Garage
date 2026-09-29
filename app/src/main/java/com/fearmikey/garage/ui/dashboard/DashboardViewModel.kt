package com.fearmikey.garage.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.GarageApplication
import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.fuel.FuelEconomyEntry
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.IgnoredMaintenanceRule
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.schedule.DocumentExpirationEngine
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.notification.WorkScheduler
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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VehicleListItem(
    val vehicle: Vehicle,
    val latestMileage: Int?,
    val imageFile: File? = null,
    val imageFiles: List<Pair<File, Float>> = emptyList(),
    val avgMpg: Double? = null,
    val overdueReminderCount: Int = 0,
    val upcomingReminderCount: Int = 0,
    val fuelEntries: List<FuelEconomyEntry> = emptyList(),
)

data class DriversLicenseState(
    val number: String = "",
    val state: String = "",
    val expiration: Long? = null,
    val notes: String = "",
    val imageFilenameFront: String? = null,
    val imageFilenameBack: String? = null,
) {
    val isEmpty: Boolean
        get() = (number.isBlank() && state.isBlank() && expiration == null && notes.isBlank() && imageFilenameFront == null && imageFilenameBack == null)
}

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
    customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    private val imageStorageManager: ImageStorageManager,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val affiliateLinksEnabled: StateFlow<Boolean> = preferencesRepository.affiliateLinksEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        
    val showFuelTrendGraph: StateFlow<Boolean> = preferencesRepository.showFuelTrendGraph
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val showFleetOverview: StateFlow<Boolean> = preferencesRepository.showFleetOverview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val vehicles: StateFlow<List<VehicleListItem>> = vehicleRepository.getAllVehicles()
        .flatMapLatest { vehicles ->
            if (vehicles.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    vehicles.map { vehicle ->
                        combine(
                            maintenanceRepository.getLatestMileageForVehicle(vehicle.id),
                            maintenanceRepository.getRecordsForVehicle(vehicle.id),
                            customMaintenanceRuleRepository.getRulesForVehicle(vehicle.id),
                            fuelRepository.getRecordsForVehicle(vehicle.id),
                            vehicleRepository.getRegistrationInsurance(vehicle.id),
                            maintenanceRepository.getIgnoredRulesForVehicle(vehicle.id),
                            preferencesRepository.maintenanceMileageWindow,
                            preferencesRepository.maintenanceDaysWindow,
                        ) { flows: Array<Any?> ->
                            val mileage = flows[0] as? Int
                            val maintenanceRecords = (flows[1] as? List<*>)?.filterIsInstance<MaintenanceRecord>() ?: emptyList()
                            val customRules = (flows[2] as? List<*>)?.filterIsInstance<CustomMaintenanceRule>() ?: emptyList()
                            val fuelRecords = (flows[3] as? List<*>)?.filterIsInstance<FuelRecord>() ?: emptyList()
                            val regIns = flows[4] as? VehicleRegistrationInsurance
                            val ignoredRules = (flows[5] as? List<*>)?.filterIsInstance<IgnoredMaintenanceRule>() ?: emptyList()
                            val upcomingWindowMiles = flows[6] as? Int ?: 500
                            val upcomingWindowDays = flows[7] as? Int ?: 10

                            val fuelEntries = FuelEconomyCalculator.entriesFor(fuelRecords)
                            val avgMpg = FuelEconomyCalculator.averageMpg(fuelEntries)

                            val suggestions = MaintenanceScheduleEngine.suggestionsFor(
                                vehicle = vehicle,
                                latestMileage = mileage,
                                records = maintenanceRecords,
                                customRules = customRules.map { it.toMaintenanceRule() },
                                ignoredTaskNames = ignoredRules.map { it.taskName }.toSet(),
                                upcomingWindowMiles = upcomingWindowMiles,
                                upcomingWindowDays = upcomingWindowDays,
                            )

                            val docReminders = regIns?.let {
                                DocumentExpirationEngine.checkExpirations(
                                    record = it,
                                    upcomingWindowDays = upcomingWindowDays,
                                )
                            } ?: emptyList()

                            val overdueCount = suggestions.count { it.status == ReminderStatus.OVERDUE } +
                                docReminders.count { it.status == ReminderStatus.OVERDUE }
                            val upcomingCount = suggestions.count { it.status == ReminderStatus.UPCOMING } +
                                docReminders.count { it.status == ReminderStatus.UPCOMING }

                            val imageFiles = vehicle.photos.mapNotNull { photo ->
                                val file = imageStorageManager.imageFile(photo.uri)
                                if (file.exists()) Pair(file, photo.offsetY) else null
                            }

                            VehicleListItem(
                                vehicle = vehicle,
                                latestMileage = mileage,
                                imageFile = imageFiles.firstOrNull()?.first,
                                imageFiles = imageFiles,
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

    val driversLicenseState: StateFlow<DriversLicenseState> = combine(
        preferencesRepository.driversLicenseNumber,
        preferencesRepository.driversLicenseState,
        preferencesRepository.driversLicenseExpiration,
        preferencesRepository.driversLicenseNotes,
        preferencesRepository.driversLicenseImageFront,
        preferencesRepository.driversLicenseImageBack,
    ) { flows: Array<Any?> ->
        val num = flows[0] as? String
        val state = flows[1] as? String
        val exp = flows[2] as? Long
        val notes = flows[3] as? String
        val front = flows[4] as? String
        val back = flows[5] as? String

        DriversLicenseState(
            number = num.orEmpty(),
            state = state.orEmpty(),
            expiration = exp,
            notes = notes.orEmpty(),
            imageFilenameFront = front,
            imageFilenameBack = back,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriversLicenseState())

    fun imageFileFor(filename: String): File = imageStorageManager.imageFile(filename)

    fun onSaveDriversLicense(
        number: String,
        state: String,
        expiration: Long?,
        notes: String,
        pickedFrontUri: Uri?,
        pickedBackUri: Uri?,
        existingFrontFilename: String?,
        existingBackFilename: String?,
    ) {
        viewModelScope.launch {
            val finalFrontFilename = when {
                pickedFrontUri != null -> {
                    try {
                        imageStorageManager.copyPickedImageToInternalStorage(pickedFrontUri)
                    } catch (_: Exception) {
                        existingFrontFilename
                    }
                }
                else -> existingFrontFilename
            }

            val finalBackFilename = when {
                pickedBackUri != null -> {
                    try {
                        imageStorageManager.copyPickedImageToInternalStorage(pickedBackUri)
                    } catch (_: Exception) {
                        existingBackFilename
                    }
                }
                else -> existingBackFilename
            }

            preferencesRepository.setDriversLicense(
                number = number.trim().takeIf { it.isNotBlank() },
                state = state.trim().takeIf { it.isNotBlank() },
                expiration = expiration,
                notes = notes.trim().takeIf { it.isNotBlank() },
                imageFront = finalFrontFilename,
                imageBack = finalBackFilename,
            )
            try {
                WorkScheduler.triggerImmediateReminderCheck(GarageApplication.instance)
            } catch (_: Exception) {}
        }
    }

    fun onDeleteDriversLicense() {
        viewModelScope.launch {
            preferencesRepository.setDriversLicense(null, null, null, null, null, null)
            try {
                WorkScheduler.triggerImmediateReminderCheck(GarageApplication.instance)
            } catch (_: Exception) {}
        }
    }
}

