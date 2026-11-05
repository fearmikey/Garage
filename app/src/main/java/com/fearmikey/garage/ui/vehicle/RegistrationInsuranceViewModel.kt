package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.CustomDocument
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.repository.CustomDocumentRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.obd.ObdScanSummary
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RegistrationInsuranceUiState(
    val record: VehicleRegistrationInsurance? = null,
    val currencySymbol: String = "$",
    val obdReadiness: ObdScanSummary.SavedReadiness? = null,
    val obdReadinessDate: Long? = null,
    /** User-named documents (warranty, roadside assistance, title...). */
    val customDocuments: List<CustomDocument> = emptyList(),
)

@HiltViewModel
class RegistrationInsuranceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    private val imageStorageManager: ImageStorageManager,
    preferencesRepository: PreferencesRepository,
    maintenanceRepository: MaintenanceRepository? = null,
    private val customDocumentRepository: CustomDocumentRepository? = null,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val latestObdScan: Flow<MaintenanceRecord?> =
        maintenanceRepository?.getRecordsForVehicle(vehicleId)
            ?.map { records ->
                records.filter { it.taskName == ObdScanSummary.SCAN_TASK_NAME }.maxByOrNull { it.date }
            }
            ?: flowOf(null)

    val uiState: StateFlow<RegistrationInsuranceUiState> = combine(
        vehicleRepository.getRegistrationInsurance(vehicleId),
        preferencesRepository.appCurrency,
        latestObdScan,
        customDocumentRepository?.getDocumentsForVehicle(vehicleId) ?: flowOf(emptyList()),
    ) { record, currency, obdScan, customDocuments ->
        val readiness = obdScan?.description?.let(ObdScanSummary::parseReadiness)
        RegistrationInsuranceUiState(
            record = record,
            currencySymbol = currency.symbol,
            obdReadiness = readiness,
            obdReadinessDate = obdScan?.date?.takeIf { readiness != null },
            customDocuments = customDocuments,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrationInsuranceUiState())

    fun imageFileFor(filename: String): File = imageStorageManager.imageFile(filename)

    fun saveCustomDocument(document: CustomDocument, pickedAttachment: Uri?, removeAttachment: Boolean) {
        val repository = customDocumentRepository ?: return
        viewModelScope.launch {
            repository.saveDocument(
                document = document.copy(vehicleId = vehicleId),
                pickedAttachment = pickedAttachment,
                removeAttachment = removeAttachment,
            )
        }
    }

    fun deleteCustomDocument(document: CustomDocument) {
        val repository = customDocumentRepository ?: return
        viewModelScope.launch { repository.deleteDocument(document) }
    }
}
