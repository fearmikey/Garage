package com.fearmikey.garage.ui.maintenance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.CustomMaintenanceRuleRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.MaintenanceSuggestion
import com.fearmikey.garage.data.schedule.MaintenanceTemplate
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import com.fearmikey.garage.notification.WorkScheduler
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MaintenanceSuggestionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val customMaintenanceRuleRepository: CustomMaintenanceRuleRepository,
    preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context? = null,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val unitSystem: StateFlow<UnitSystem> = preferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.IMPERIAL)

    val latestMileage: StateFlow<Int?> = maintenanceRepository.getLatestMileageForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customRules: StateFlow<List<CustomMaintenanceRule>> =
        customMaintenanceRuleRepository.getRulesForVehicle(vehicleId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestions: StateFlow<List<MaintenanceSuggestion>> = combine(
        vehicleRepository.getVehicleById(vehicleId),
        maintenanceRepository.getRecordsForVehicle(vehicleId),
        maintenanceRepository.getLatestMileageForVehicle(vehicleId),
        customMaintenanceRuleRepository.getRulesForVehicle(vehicleId),
        preferencesRepository.maintenanceMileageWindow,
    ) { flows: Array<Any?> ->
        val vehicle = flows[0] as? Vehicle
        val records = (flows[1] as? List<*>)?.filterIsInstance<MaintenanceRecord>() ?: emptyList()
        val latestMileage = flows[2] as? Int
        val customRules = (flows[3] as? List<*>)?.filterIsInstance<CustomMaintenanceRule>() ?: emptyList()
        val mileageWindow = flows[4] as? Int ?: 500

        if (vehicle == null) {
            emptyList()
        } else {
            MaintenanceScheduleEngine.suggestionsFor(
                vehicle = vehicle,
                latestMileage = latestMileage,
                records = records,
                customRules = customRules.map { it.toMaintenanceRule() },
                upcomingWindowMiles = mileageWindow,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun receiptFileFor(filename: String): File? = maintenanceRepository.imageFileFor(filename)

    /**
     * Saves a [MaintenanceRecord] logged directly from a suggestion (the "Log now" action).
     * The record already carries the suggestion's [MaintenanceRecord.category] and
     * [MaintenanceRecord.taskName] (see [AddEditMaintenanceRecordSheet]),
     * so it's tracked precisely rather than lumped in with other tasks in the same category.
     */
    fun logMaintenance(
        record: MaintenanceRecord,
        pickedReceiptUri: Uri? = null,
        deleteExistingReceipt: Boolean = false,
    ) {
        viewModelScope.launch {
            maintenanceRepository.saveRecord(
                record = record.copy(vehicleId = vehicleId),
                newPickedReceiptUri = pickedReceiptUri,
                deleteExistingReceipt = deleteExistingReceipt,
            )
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun saveCustomRule(rule: CustomMaintenanceRule) {
        viewModelScope.launch {
            customMaintenanceRuleRepository.saveRule(rule.copy(vehicleId = vehicleId))
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun applyTemplate(template: MaintenanceTemplate) {
        viewModelScope.launch {
            val existing = customMaintenanceRuleRepository.getRulesForVehicle(vehicleId).first()
            val existingTaskNames = existing.map { it.taskName.lowercase() }.toSet()

            for (rule in template.rules) {
                if (rule.taskName.lowercase() !in existingTaskNames) {
                    customMaintenanceRuleRepository.saveRule(
                        CustomMaintenanceRule(
                            vehicleId = vehicleId,
                            taskName = rule.taskName,
                            category = rule.category,
                            intervalMiles = rule.intervalMiles,
                            intervalMonths = rule.intervalMonths,
                            notes = rule.notes,
                        )
                    )
                }
            }
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }

    fun deleteCustomRule(rule: CustomMaintenanceRule) {
        viewModelScope.launch {
            customMaintenanceRuleRepository.deleteRule(rule)
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
        }
    }
}
