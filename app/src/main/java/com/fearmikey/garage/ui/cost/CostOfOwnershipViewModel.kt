package com.fearmikey.garage.ui.cost

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class TimeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    LAST_YEAR("Last Year"),
    PAST_12_MONTHS("12 Months"),
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
    val modificationCategory: ModificationCategory? = null,
)

data class CostOfOwnershipUiState(
    val totalCost: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val fuelCost: Double = 0.0,
    val modCost: Double = 0.0,
    val maintenanceRecordCount: Int = 0,
    val fuelRecordCount: Int = 0,
    val modRecordCount: Int = 0,
    val includeModsInCost: Boolean = false,
    val categories: List<CategoryCostItem> = emptyList(),
    val selectedTimeFilter: TimeFilter = TimeFilter.ALL_TIME,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val currencySymbol: String = "$",
)

@HiltViewModel
class CostOfOwnershipViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    maintenanceRepository: MaintenanceRepository,
    fuelRepository: FuelRepository,
    modificationRepository: ModificationRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _timeFilter = MutableStateFlow(TimeFilter.ALL_TIME)

    val uiState: StateFlow<CostOfOwnershipUiState> = combine(
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        fuelRepository.getRecordsForVehicle(vehicleId),
        modificationRepository.getModsForVehicle(vehicleId),
        preferencesRepository.unitSystem,
        preferencesRepository.appCurrency,
        preferencesRepository.includeModsInCost,
        _timeFilter,
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val maintenanceRecords = array[0] as List<MaintenanceRecord>
        @Suppress("UNCHECKED_CAST")
        val fuelRecords = array[1] as List<FuelRecord>
        @Suppress("UNCHECKED_CAST")
        val modRecords = array[2] as List<ModificationRecord>
        val unitSystem = array[3] as UnitSystem
        val currency = array[4] as AppCurrency
        val includeMods = array[5] as Boolean
        val filter = array[6] as TimeFilter

        val timeRange = computeTimeRange(filter)

        val filteredMaintenance = maintenanceRecords.filter { record ->
            ((timeRange.start == null) || (record.date >= timeRange.start)) &&
            ((timeRange.end == null) || (record.date <= timeRange.end))
        }

        val filteredFuel = fuelRecords.filter { record ->
            ((timeRange.start == null) || (record.date >= timeRange.start)) &&
            ((timeRange.end == null) || (record.date <= timeRange.end))
        }

        val filteredMods = modRecords.filter { record ->
            ((timeRange.start == null) || (record.date >= timeRange.start)) &&
            ((timeRange.end == null) || (record.date <= timeRange.end))
        }

        val maintTotal = filteredMaintenance.sumOf { it.cost }
        val fuelTotal = filteredFuel.sumOf { it.totalCost }
        val modTotal = filteredMods.sumOf { it.cost }

        val grandTotal = maintTotal + fuelTotal + if (includeMods) modTotal else 0.0

        val categoryItems = mutableListOf<CategoryCostItem>()

        val maintenanceByCategory = filteredMaintenance.groupBy { it.category }

        MaintenanceCategory.entries.forEach { category ->
            val records = maintenanceByCategory[category].orEmpty()
            val categoryTotal = records.sumOf { it.cost }
            if (records.isNotEmpty() || (categoryTotal > 0.0)) {
                val pct = if (grandTotal > 0.0) ((categoryTotal / grandTotal) * 100).toFloat() else 0f
                val entries = records.asSequence().sortedByDescending { it.date }.map { record ->
                    CostEntry(
                        id = record.id,
                        date = record.date,
                        title = record.taskName?.ifBlank { null } ?: record.description.ifBlank { category.displayName },
                        cost = record.cost,
                        mileage = record.mileage,
                        detail = record.description.takeIf { (it.isNotBlank()) && (it != record.taskName) },
                    )
                }
                categoryItems.add(
                    CategoryCostItem(
                        key = category.name,
                        title = category.displayName,
                        totalCost = categoryTotal,
                        percentage = pct,
                        recordCount = records.size,
                        entries = entries.toList(),
                        category = category,
                    ),
                )
            }
        }

        if (filteredFuel.isNotEmpty()) {
            val pct = if (grandTotal > 0.0) ((fuelTotal / grandTotal) * 100).toFloat() else 0f
            val entries = filteredFuel.asSequence().sortedByDescending { it.date }.map { record ->
                val displayVolume = UnitConverter.displayVolumeValue(record.gallons, unitSystem)
                val volumeUnit = if (unitSystem == UnitSystem.METRIC) "L" else "gal"
                val displayPrice = if (unitSystem == UnitSystem.METRIC) record.pricePerGallon / UnitConverter.LITERS_PER_GALLON else record.pricePerGallon
                CostEntry(
                    id = record.id,
                    date = record.date,
                    title = "Fuel Fill-Up",
                    cost = record.totalCost,
                    mileage = record.mileage,
                    detail = "%.1f %s @ %s%.2f/%s".format(displayVolume, volumeUnit, currency.symbol, displayPrice, volumeUnit),
                )
            }.toList()
            categoryItems.add(
                CategoryCostItem(
                    key = "FUEL",
                    title = "Fuel",
                    totalCost = fuelTotal,
                    percentage = pct,
                    recordCount = filteredFuel.size,
                    entries = entries,
                    category = null,
                ),
            )
        }

        if (includeMods && filteredMods.isNotEmpty()) {
            val modsByCategory = filteredMods.groupBy { it.category }
            ModificationCategory.entries.forEach { modCategory ->
                val records = modsByCategory[modCategory].orEmpty()
                val categoryTotal = records.sumOf { it.cost }
                if (records.isNotEmpty() || (categoryTotal > 0.0)) {
                    val pct = if (grandTotal > 0.0) ((categoryTotal / grandTotal) * 100).toFloat() else 0f
                    val entries = records.asSequence().sortedByDescending { it.date }.map { record ->
                        CostEntry(
                            id = record.id,
                            date = record.date,
                            title = record.title.ifBlank { modCategory.displayName },
                            cost = record.cost,
                            mileage = 0,
                            detail = record.description.takeIf { it.isNotBlank() },
                        )
                    }.toList()
                    categoryItems.add(
                        CategoryCostItem(
                            key = "MOD_${modCategory.name}",
                            title = "Mod: ${modCategory.displayName}",
                            totalCost = categoryTotal,
                            percentage = pct,
                            recordCount = records.size,
                            entries = entries,
                            category = null,
                            modificationCategory = modCategory,
                        ),
                    )
                }
            }
        }

        categoryItems.sortByDescending { it.totalCost }

        CostOfOwnershipUiState(
            totalCost = grandTotal,
            maintenanceCost = maintTotal,
            fuelCost = fuelTotal,
            modCost = modTotal,
            maintenanceRecordCount = filteredMaintenance.size,
            fuelRecordCount = filteredFuel.size,
            modRecordCount = filteredMods.size,
            includeModsInCost = includeMods,
            categories = categoryItems,
            selectedTimeFilter = filter,
            unitSystem = unitSystem,
            currencySymbol = currency.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CostOfOwnershipUiState())

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun toggleIncludeMods(include: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeModsInCost(include)
        }
    }

    private data class TimeRange(val start: Long? = null, val end: Long? = null)

    private fun computeTimeRange(filter: TimeFilter): TimeRange {
        val now = System.currentTimeMillis()
        return when (filter) {
            TimeFilter.ALL_TIME -> TimeRange()
            TimeFilter.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal[Calendar.DAY_OF_YEAR] = 1
                cal[Calendar.HOUR_OF_DAY] = 0
                cal[Calendar.MINUTE] = 0
                cal[Calendar.SECOND] = 0
                cal[Calendar.MILLISECOND] = 0
                TimeRange(start = cal.timeInMillis)
            }
            TimeFilter.LAST_YEAR -> {
                val cal = Calendar.getInstance()
                cal[Calendar.DAY_OF_YEAR] = 1
                cal[Calendar.HOUR_OF_DAY] = 0
                cal[Calendar.MINUTE] = 0
                cal[Calendar.SECOND] = 0
                cal[Calendar.MILLISECOND] = 0
                val startOfThisYear = cal.timeInMillis

                cal.add(Calendar.YEAR, -1)
                val startOfLastYear = cal.timeInMillis

                TimeRange(start = startOfLastYear, end = startOfThisYear - 1)
            }
            TimeFilter.PAST_12_MONTHS -> TimeRange(start = now - TimeUnit.DAYS.toMillis(365))
            TimeFilter.PAST_6_MONTHS -> TimeRange(start = now - TimeUnit.DAYS.toMillis(180))
        }
    }
}
