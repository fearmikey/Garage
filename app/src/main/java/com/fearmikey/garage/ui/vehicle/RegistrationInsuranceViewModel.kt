package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.obd.ObdScanSummary
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.parseToDoubleOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RegistrationInsuranceUiState(
    val record: VehicleRegistrationInsurance? = null,
    val currencySymbol: String = "$",
    val isSheetOpen: Boolean = false,
    val isSaving: Boolean = false,

    // Form field states
    val licensePlate: String = "",
    val registrationState: String = "",
    val registrationExpiration: Long? = null,
    val registrationFee: String = "",
    val registrationNotes: String = "",
    val registrationImageFilename: String? = null,
    val pickedRegistrationImageUri: Uri? = null,

    val inspectionExpiration: Long? = null,
    val inspectionDate: Long? = null,
    val inspectionResult: String = "",
    val inspectionNotes: String = "",

    val emissionsExpiration: Long? = null,
    val emissionsDate: Long? = null,
    val emissionsResult: String = "",
    val emissionsNotes: String = "",

    val inspectionStickerExpiration: Long? = null,
    val inspectionStickerNumber: String = "",
    val inspectionStickerNotes: String = "",

    val insuranceProvider: String = "",
    val policyNumber: String = "",
    val insuranceExpiration: Long? = null,
    val insurancePremium: String = "",
    val insuranceAgentContact: String = "",
    val insuranceNotes: String = "",
    val insuranceImageFilename: String? = null,
    val pickedInsuranceImageUri: Uri? = null,

    val tollPassParkingExpiration: Long? = null,
    val tollPassParkingAccount: String = "",
    val tollPassParkingNotes: String = "",

    /** Emissions readiness from the most recent OBD2 scan saved to the timeline, if any. */
    val obdReadiness: ObdScanSummary.SavedReadiness? = null,
    val obdReadinessDate: Long? = null,
) {
    val hasRegistrationData: Boolean
        get() = licensePlate.isNotBlank() || registrationState.isNotBlank() || (registrationExpiration != null) ||
            registrationFee.isNotBlank() || registrationNotes.isNotBlank() || (registrationImageFilename != null) ||
            (pickedRegistrationImageUri != null)

    val hasInspectionData: Boolean
        get() = (inspectionExpiration != null) || (inspectionDate != null) || inspectionResult.isNotBlank() ||
            inspectionNotes.isNotBlank()

    val hasEmissionsData: Boolean
        get() = (emissionsExpiration != null) || (emissionsDate != null) || emissionsResult.isNotBlank() ||
            emissionsNotes.isNotBlank()

    val hasInspectionStickerData: Boolean
        get() = (inspectionStickerExpiration != null) || inspectionStickerNumber.isNotBlank() ||
            inspectionStickerNotes.isNotBlank()

    val hasInsuranceData: Boolean
        get() = insuranceProvider.isNotBlank() || policyNumber.isNotBlank() || (insuranceExpiration != null) ||
            insurancePremium.isNotBlank() || insuranceAgentContact.isNotBlank() || insuranceNotes.isNotBlank() ||
            (insuranceImageFilename != null) || (pickedInsuranceImageUri != null)

    val hasTollPassParkingData: Boolean
        get() = (tollPassParkingExpiration != null) || tollPassParkingAccount.isNotBlank() ||
            tollPassParkingNotes.isNotBlank()
}

@HiltViewModel
class RegistrationInsuranceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val imageStorageManager: ImageStorageManager,
    preferencesRepository: PreferencesRepository,
    maintenanceRepository: MaintenanceRepository? = null,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _sheetState = MutableStateFlow(RegistrationInsuranceUiState())

    private val latestObdScan: Flow<MaintenanceRecord?> =
        maintenanceRepository?.getRecordsForVehicle(vehicleId)
            ?.map { records ->
                records.filter { it.taskName == ObdScanSummary.SCAN_TASK_NAME }.maxByOrNull { it.date }
            }
            ?: flowOf(null)

    val uiState: StateFlow<RegistrationInsuranceUiState> = combine(
        vehicleRepository.getRegistrationInsurance(vehicleId),
        preferencesRepository.appCurrency,
        _sheetState,
        latestObdScan,
    ) { record, currency, formState, obdScan ->
        val readiness = obdScan?.description?.let(ObdScanSummary::parseReadiness)
        formState.copy(
            record = record,
            currencySymbol = currency.symbol,
            obdReadiness = readiness,
            obdReadinessDate = obdScan?.date?.takeIf { readiness != null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrationInsuranceUiState())

    fun imageFileFor(filename: String): File = imageStorageManager.imageFile(filename)

    fun onAddOrEditClicked() {
        val currentRecord = uiState.value.record
        _sheetState.update { state ->
            state.copy(
                isSheetOpen = true,
                licensePlate = currentRecord?.licensePlate.orEmpty(),
                registrationState = currentRecord?.registrationState.orEmpty(),
                registrationExpiration = currentRecord?.registrationExpiration,
                registrationFee = currentRecord?.registrationFee?.let { "%.2f".format(it) }.orEmpty(),
                registrationNotes = currentRecord?.registrationNotes.orEmpty(),
                registrationImageFilename = currentRecord?.registrationImageUri,
                pickedRegistrationImageUri = null,

                inspectionExpiration = currentRecord?.inspectionExpiration,
                inspectionDate = currentRecord?.inspectionDate,
                inspectionResult = currentRecord?.inspectionResult.orEmpty(),
                inspectionNotes = currentRecord?.inspectionNotes.orEmpty(),

                emissionsExpiration = currentRecord?.emissionsExpiration,
                emissionsDate = currentRecord?.emissionsDate,
                emissionsResult = currentRecord?.emissionsResult.orEmpty(),
                emissionsNotes = currentRecord?.emissionsNotes.orEmpty(),

                inspectionStickerExpiration = currentRecord?.inspectionStickerExpiration,
                inspectionStickerNumber = currentRecord?.inspectionStickerNumber.orEmpty(),
                inspectionStickerNotes = currentRecord?.inspectionStickerNotes.orEmpty(),

                insuranceProvider = currentRecord?.insuranceProvider.orEmpty(),
                policyNumber = currentRecord?.policyNumber.orEmpty(),
                insuranceExpiration = currentRecord?.insuranceExpiration,
                insurancePremium = currentRecord?.insurancePremium?.let { "%.2f".format(it) }.orEmpty(),
                insuranceAgentContact = currentRecord?.insuranceAgentContact.orEmpty(),
                insuranceNotes = currentRecord?.insuranceNotes.orEmpty(),
                insuranceImageFilename = currentRecord?.insuranceImageUri,
                pickedInsuranceImageUri = null,

                tollPassParkingExpiration = currentRecord?.tollPassParkingExpiration,
                tollPassParkingAccount = currentRecord?.tollPassParkingAccount.orEmpty(),
                tollPassParkingNotes = currentRecord?.tollPassParkingNotes.orEmpty(),
            )
        }
    }

    fun onDismissSheet() {
        _sheetState.update { it.copy(isSheetOpen = false, isSaving = false) }
    }

    fun onLicensePlateChanged(value: String) {
        _sheetState.update { it.copy(licensePlate = value) }
    }

    fun onRegistrationStateChanged(value: String) {
        _sheetState.update { it.copy(registrationState = value) }
    }

    fun onRegistrationExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(registrationExpiration = value) }
    }

    fun onRegistrationFeeChanged(value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' || c == ',' }
        _sheetState.update { it.copy(registrationFee = filtered) }
    }

    fun onRegistrationNotesChanged(value: String) {
        _sheetState.update { it.copy(registrationNotes = value) }
    }

    fun onRegistrationImagePicked(uri: Uri) {
        _sheetState.update { it.copy(pickedRegistrationImageUri = uri) }
    }

    fun onRemoveRegistrationImage() {
        _sheetState.update { it.copy(pickedRegistrationImageUri = null, registrationImageFilename = null) }
    }

    fun onInspectionExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(inspectionExpiration = value) }
    }

    fun onInspectionDateChanged(value: Long?) {
        _sheetState.update { it.copy(inspectionDate = value) }
    }

    fun onInspectionResultChanged(value: String) {
        _sheetState.update { it.copy(inspectionResult = value) }
    }

    fun onInspectionNotesChanged(value: String) {
        _sheetState.update { it.copy(inspectionNotes = value) }
    }

    fun onEmissionsExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(emissionsExpiration = value) }
    }

    fun onEmissionsDateChanged(value: Long?) {
        _sheetState.update { it.copy(emissionsDate = value) }
    }

    fun onEmissionsResultChanged(value: String) {
        _sheetState.update { it.copy(emissionsResult = value) }
    }

    fun onEmissionsNotesChanged(value: String) {
        _sheetState.update { it.copy(emissionsNotes = value) }
    }

    fun onInspectionStickerExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(inspectionStickerExpiration = value) }
    }

    fun onInspectionStickerNumberChanged(value: String) {
        _sheetState.update { it.copy(inspectionStickerNumber = value) }
    }

    fun onInspectionStickerNotesChanged(value: String) {
        _sheetState.update { it.copy(inspectionStickerNotes = value) }
    }

    fun onInsuranceProviderChanged(value: String) {
        _sheetState.update { it.copy(insuranceProvider = value) }
    }

    fun onPolicyNumberChanged(value: String) {
        _sheetState.update { it.copy(policyNumber = value) }
    }

    fun onInsuranceExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(insuranceExpiration = value) }
    }

    fun onInsurancePremiumChanged(value: String) {
        val filtered = value.filter { c -> c.isDigit() || c == '.' || c == ',' }
        _sheetState.update { it.copy(insurancePremium = filtered) }
    }

    fun onInsuranceAgentContactChanged(value: String) {
        _sheetState.update { it.copy(insuranceAgentContact = value) }
    }

    fun onInsuranceNotesChanged(value: String) {
        _sheetState.update { it.copy(insuranceNotes = value) }
    }

    fun onInsuranceImagePicked(uri: Uri) {
        _sheetState.update { it.copy(pickedInsuranceImageUri = uri) }
    }

    fun onRemoveInsuranceImage() {
        _sheetState.update { it.copy(pickedInsuranceImageUri = null, insuranceImageFilename = null) }
    }

    fun onTollPassParkingExpirationChanged(value: Long?) {
        _sheetState.update { it.copy(tollPassParkingExpiration = value) }
    }

    fun onTollPassParkingAccountChanged(value: String) {
        _sheetState.update { it.copy(tollPassParkingAccount = value) }
    }

    fun onTollPassParkingNotesChanged(value: String) {
        _sheetState.update { it.copy(tollPassParkingNotes = value) }
    }

    fun onDeleteRegistrationSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    licensePlate = "",
                    registrationState = "",
                    registrationExpiration = null,
                    registrationFee = "",
                    registrationNotes = "",
                    registrationImageFilename = null,
                    pickedRegistrationImageUri = null,
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onDeleteInspectionSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    inspectionExpiration = null,
                    inspectionDate = null,
                    inspectionResult = "",
                    inspectionNotes = "",
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onDeleteEmissionsSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    emissionsExpiration = null,
                    emissionsDate = null,
                    emissionsResult = "",
                    emissionsNotes = "",
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onDeleteInspectionStickerSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    inspectionStickerExpiration = null,
                    inspectionStickerNumber = "",
                    inspectionStickerNotes = "",
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onDeleteInsuranceSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    insuranceProvider = "",
                    policyNumber = "",
                    insuranceExpiration = null,
                    insurancePremium = "",
                    insuranceAgentContact = "",
                    insuranceNotes = "",
                    insuranceImageFilename = null,
                    pickedInsuranceImageUri = null,
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onDeleteTollPassParkingSection() {
        viewModelScope.launch {
            _sheetState.update { state ->
                state.copy(
                    tollPassParkingExpiration = null,
                    tollPassParkingAccount = "",
                    tollPassParkingNotes = "",
                )
            }
            saveCurrentStateInternal()
        }
    }

    fun onSave() {
        viewModelScope.launch {
            _sheetState.update { it.copy(isSaving = true) }
            saveCurrentStateInternal()
            _sheetState.update { it.copy(isSheetOpen = false, isSaving = false) }
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            vehicleRepository.deleteRegistrationInsurance(vehicleId)
            _sheetState.update { it.copy(isSheetOpen = false) }
        }
    }

    private suspend fun saveCurrentStateInternal() {
        val currentState = _sheetState.value

        val finalRegFilename = when {
            currentState.pickedRegistrationImageUri != null -> {
                try {
                    imageStorageManager.copyPickedImageToInternalStorage(currentState.pickedRegistrationImageUri)
                } catch (_: Exception) {
                    currentState.registrationImageFilename
                }
            }
            else -> currentState.registrationImageFilename
        }

        val finalInsFilename = when {
            currentState.pickedInsuranceImageUri != null -> {
                try {
                    imageStorageManager.copyPickedImageToInternalStorage(currentState.pickedInsuranceImageUri)
                } catch (_: Exception) {
                    currentState.insuranceImageFilename
                }
            }
            else -> currentState.insuranceImageFilename
        }

        val newRecord = VehicleRegistrationInsurance(
            vehicleId = vehicleId,
            licensePlate = currentState.licensePlate.trim().takeIf { it.isNotBlank() },
            registrationState = currentState.registrationState.trim().takeIf { it.isNotBlank() },
            registrationExpiration = currentState.registrationExpiration,
            registrationFee = currentState.registrationFee.parseToDoubleOrNull(),
            registrationNotes = currentState.registrationNotes.trim().takeIf { it.isNotBlank() },
            registrationImageUri = finalRegFilename,

            inspectionExpiration = currentState.inspectionExpiration,
            inspectionDate = currentState.inspectionDate,
            inspectionResult = currentState.inspectionResult.trim().takeIf { it.isNotBlank() },
            inspectionNotes = currentState.inspectionNotes.trim().takeIf { it.isNotBlank() },

            emissionsExpiration = currentState.emissionsExpiration,
            emissionsDate = currentState.emissionsDate,
            emissionsResult = currentState.emissionsResult.trim().takeIf { it.isNotBlank() },
            emissionsNotes = currentState.emissionsNotes.trim().takeIf { it.isNotBlank() },

            inspectionStickerExpiration = currentState.inspectionStickerExpiration,
            inspectionStickerNumber = currentState.inspectionStickerNumber.trim().takeIf { it.isNotBlank() },
            inspectionStickerNotes = currentState.inspectionStickerNotes.trim().takeIf { it.isNotBlank() },

            insuranceProvider = currentState.insuranceProvider.trim().takeIf { it.isNotBlank() },
            policyNumber = currentState.policyNumber.trim().takeIf { it.isNotBlank() },
            insuranceExpiration = currentState.insuranceExpiration,
            insurancePremium = currentState.insurancePremium.parseToDoubleOrNull(),
            insuranceAgentContact = currentState.insuranceAgentContact.trim().takeIf { it.isNotBlank() },
            insuranceNotes = currentState.insuranceNotes.trim().takeIf { it.isNotBlank() },
            insuranceImageUri = finalInsFilename,

            tollPassParkingExpiration = currentState.tollPassParkingExpiration,
            tollPassParkingAccount = currentState.tollPassParkingAccount.trim().takeIf { it.isNotBlank() },
            tollPassParkingNotes = currentState.tollPassParkingNotes.trim().takeIf { it.isNotBlank() },
        )

        if (newRecord.isEmpty()) {
            vehicleRepository.deleteRegistrationInsurance(vehicleId)
        } else {
            vehicleRepository.saveRegistrationInsurance(newRecord)
        }
    }
}
