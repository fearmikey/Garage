package com.fearmikey.garage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class DuplicateRecordType { FUEL, MAINTENANCE, MODIFICATION }

data class DuplicateItem(
    val type: DuplicateRecordType,
    val localId: Long,
    val dateMillis: Long,
    val mileage: Int,
    val titleOrDescription: String,
    val cost: Double,
    val lubeLoggerId: Int?,
    val isSelectedForDeletion: Boolean = false,
)

data class DuplicateGroup(
    val groupKey: String,
    val vehicleName: String,
    val displayDate: String,
    val items: List<DuplicateItem>,
)

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val groups: List<DuplicateGroup> = emptyList(),
    val isDeleting: Boolean = false,
    val userMessage: String? = null,
)

@HiltViewModel
class LubeLoggerDuplicatesViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val modificationRepository: ModificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    private var rawFuelRecords = mapOf<Long, List<FuelRecord>>()
    private var rawMaintenanceRecords = mapOf<Long, List<MaintenanceRecord>>()
    private var rawModRecords = mapOf<Long, List<ModificationRecord>>()

    init {
        loadDuplicates()
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val vehicles = vehicleRepository.getAllVehicles().first()
            val vehicleMap = vehicles.associateBy { it.id }

            val duplicateGroups = mutableListOf<DuplicateGroup>()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            for (vehicle in vehicles) {
                val vehicleName = "${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model}".trim()

                // Fuel
                val fuels = fuelRepository.getRecordsForVehicle(vehicle.id).first()
                rawFuelRecords = rawFuelRecords + (vehicle.id to fuels)
                val fuelGroups = fuels.groupBy { "${it.date / 86400000L}|${it.mileage}|${"%.2f".format(Locale.US, it.totalCost)}" }
                for ((key, group) in fuelGroups) {
                    if (group.size > 1) {
                        duplicateGroups.add(
                            DuplicateGroup(
                                groupKey = "fuel_${vehicle.id}_$key",
                                vehicleName = vehicleName,
                                displayDate = dateFormat.format(Date(group.first().date)),
                                items = group.mapIndexed { index, record ->
                                    DuplicateItem(
                                        type = DuplicateRecordType.FUEL,
                                        localId = record.id,
                                        dateMillis = record.date,
                                        mileage = record.mileage,
                                        titleOrDescription = "Fuel (${"%.2f".format(Locale.US, record.gallons)} gal)",
                                        cost = record.totalCost,
                                        lubeLoggerId = record.lubeLoggerId,
                                        isSelectedForDeletion = index > 0, // auto-select duplicates beyond the first
                                    )
                                },
                            ),
                        )
                    }
                }

                // Maintenance
                val maints = maintenanceRepository.getRecordsForVehicle(vehicle.id).first()
                rawMaintenanceRecords = rawMaintenanceRecords + (vehicle.id to maints)
                val maintGroups = maints.groupBy { "${it.date / 86400000L}|${it.mileage}|${it.description.trim().lowercase()}|${"%.2f".format(Locale.US, it.cost)}" }
                for ((key, group) in maintGroups) {
                    if (group.size > 1) {
                        duplicateGroups.add(
                            DuplicateGroup(
                                groupKey = "maint_${vehicle.id}_$key",
                                vehicleName = vehicleName,
                                displayDate = dateFormat.format(Date(group.first().date)),
                                items = group.mapIndexed { index, record ->
                                    DuplicateItem(
                                        type = DuplicateRecordType.MAINTENANCE,
                                        localId = record.id,
                                        dateMillis = record.date,
                                        mileage = record.mileage,
                                        titleOrDescription = record.description,
                                        cost = record.cost,
                                        lubeLoggerId = record.lubeLoggerId,
                                        isSelectedForDeletion = index > 0,
                                    )
                                },
                            ),
                        )
                    }
                }

                // Modifications
                val mods = modificationRepository.getModsForVehicle(vehicle.id).first()
                rawModRecords = rawModRecords + (vehicle.id to mods)
                val modGroups = mods.groupBy { "${it.date / 86400000L}|${it.title.trim().lowercase()}|${"%.2f".format(Locale.US, it.cost)}" }
                for ((key, group) in modGroups) {
                    if (group.size > 1) {
                        duplicateGroups.add(
                            DuplicateGroup(
                                groupKey = "mod_${vehicle.id}_$key",
                                vehicleName = vehicleName,
                                displayDate = dateFormat.format(Date(group.first().date)),
                                items = group.mapIndexed { index, record ->
                                    DuplicateItem(
                                        type = DuplicateRecordType.MODIFICATION,
                                        localId = record.id,
                                        dateMillis = record.date,
                                        mileage = 0,
                                        titleOrDescription = record.title,
                                        cost = record.cost,
                                        lubeLoggerId = record.lubeLoggerId,
                                        isSelectedForDeletion = index > 0,
                                    )
                                },
                            ),
                        )
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false, groups = duplicateGroups) }
        }
    }

    fun toggleItemSelection(groupKey: String, localId: Long) {
        _uiState.update { state ->
            val updatedGroups = state.groups.map { group ->
                if (group.groupKey == groupKey) {
                    group.copy(
                        items = group.items.map { item ->
                            if (item.localId == localId) {
                                item.copy(isSelectedForDeletion = !item.isSelectedForDeletion)
                            } else {
                                item
                            }
                        },
                    )
                } else {
                    group
                }
            }
            state.copy(groups = updatedGroups)
        }
    }

    fun deleteSelectedDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            var deletedCount = 0

            val currentGroups = _uiState.value.groups
            for (group in currentGroups) {
                for (item in group.items) {
                    if (item.isSelectedForDeletion) {
                        when (item.type) {
                            DuplicateRecordType.FUEL -> {
                                val record = rawFuelRecords.values.flatten().find { it.id == item.localId }
                                if (record != null) {
                                    fuelRepository.deleteRecord(record)
                                    deletedCount++
                                }
                            }
                            DuplicateRecordType.MAINTENANCE -> {
                                val record = rawMaintenanceRecords.values.flatten().find { it.id == item.localId }
                                if (record != null) {
                                    maintenanceRepository.deleteRecord(record)
                                    deletedCount++
                                }
                            }
                            DuplicateRecordType.MODIFICATION -> {
                                val record = rawModRecords.values.flatten().find { it.id == item.localId }
                                if (record != null) {
                                    modificationRepository.deleteMod(record)
                                    deletedCount++
                                }
                            }
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isDeleting = false,
                    userMessage = "Deleted $deletedCount duplicate record(s).",
                )
            }
            loadDuplicates()
        }
    }

    fun consumeUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
