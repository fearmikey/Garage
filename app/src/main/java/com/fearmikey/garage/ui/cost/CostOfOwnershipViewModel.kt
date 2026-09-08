package com.fearmikey.garage.ui.cost

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class TimeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    PAST_YEAR("Past Year"),
    PAST_6_MONTHS("6 Months"),
}

data class CostEntry(
    val id: Long,
    val date: Long,
    val title: String,
    val cost: Double,
    val mileage: Int,
    val detail: String? = null,
)

data class CategoryCostItem(
    val key: String,
    val title: String,
    val totalCost: Double,
    val percentage: Float, // 0..100
    val recordCount: Int,
    val entries: List<CostEntry>,
    val category: MaintenanceCategory? = null,
)

data class CostOfOwnershipUiState(
    val totalCost: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val fuelCost: Double = 0.0,
    val maintenanceRecordCount: Int = 0,
    val fuelRecordCount: Int = 0,
    val categories: List<CategoryCostItem> = emptyList(),
    val selectedTimeFilter: TimeFilter = TimeFilter.ALL_TIME,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
)

@HiltViewModel
class CostOfOwnershipViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    maintenanceRepository: MaintenanceRepository,
    fuelRepository: FuelRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _timeFilter = MutableStateFlow(TimeFilter.ALL_TIME)

    val uiState: StateFlow<CostOfOwnershipUiState> = combine(
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        fuelRepository.getRecordsForVehicle(vehicleId),
        preferencesRepository.unitSystem,
        _timeFilter,
    ) { maintenanceRecords, fuelRecords, unitSystem, filter ->
        val cutoff = computeCutoffTimestamp(filter)

        val filteredMaintenance = if (cutoff == null) {
            maintenanceRecords
        } else {
            maintenanceRecords.filter { it.date >= cutoff }
        }

        val filteredFuel = if (cutoff == null) {
            fuelRecords
        } else {
            fuelRecords.filter { it.date >= cutoff }
        }

        val maintTotal = filteredMaintenance.sumOf { it.cost }
        val fuelTotal = filteredFuel.sumOf { it.totalCost }
        val grandTotal = maintTotal + fuelTotal

        val maintenanceByCategory = filteredMaintenance.groupBy { it.category }

        val categoryItems = mutableListOf<CategoryCostItem>()

        MaintenanceCategory.entries.forEach { category ->
            val records = maintenanceByCategory[category].orEmpty()
            val categoryTotal = records.sumOf { it.cost }
            if (records.isNotEmpty() || (categoryTotal > 0.0)) {
                val pct = if (grandTotal > 0.0) ((categoryTotal / grandTotal) * 100).toFloat() else 0f
                val entries = records.sortedByDescending { it.date }.map { record ->
                    CostEntry(
                        id = record.id,
                        date = record.date,
                        title = record.taskName?.ifBlank { null } ?: record.description.ifBlank { category.displayName },
                        cost = record.cost,
                        mileage = record.mileage,
                        detail = record.description.takeIf { it.isNotBlank() && it != record.taskName },
                    )
                }
                categoryItems.add(
                    CategoryCostItem(
                        key = category.name,
                        title = category.displayName,
                        totalCost = categoryTotal,
                        percentage = pct,
                        recordCount = records.size,
                        entries = entries,
                        category = category,
                    ),
                )
            }
        }

        if (filteredFuel.isNotEmpty()) {
            val pct = if (grandTotal > 0.0) ((fuelTotal / grandTotal) * 100).toFloat() else 0f
            val entries = filteredFuel.sortedByDescending { it.date }.map { record ->
                CostEntry(
                    id = record.id,
                    date = record.date,
                    title = "Fuel Fill-Up",
                    cost = record.totalCost,
                    mileage = record.mileage,
                    detail = "%.1f gal @ $%.2f/gal".format(record.gallons, record.pricePerGallon),
                )
            }
            categoryItems.add(
                CategoryCostItem(
                    key = "FUEL",
                    title = "Fuel",
                    totalCost = fuelTotal,
                    percentage = pct,
                    recordCount = filteredFuel.size,
                    entries = entries,
                    category = null,
                )
            )
        }

        categoryItems.sortByDescending { it.totalCost }

        CostOfOwnershipUiState(
            totalCost = grandTotal,
            maintenanceCost = maintTotal,
            fuelCost = fuelTotal,
            maintenanceRecordCount = filteredMaintenance.size,
            fuelRecordCount = filteredFuel.size,
            categories = categoryItems,
            selectedTimeFilter = filter,
            unitSystem = unitSystem,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CostOfOwnershipUiState())

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    private fun computeCutoffTimestamp(filter: TimeFilter): Long? {
        val now = System.currentTimeMillis()
        return when (filter) {
            TimeFilter.ALL_TIME -> null
            TimeFilter.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            TimeFilter.PAST_YEAR -> now - TimeUnit.DAYS.toMillis(365)
            TimeFilter.PAST_6_MONTHS -> now - TimeUnit.DAYS.toMillis(180)
        }
    }
}
