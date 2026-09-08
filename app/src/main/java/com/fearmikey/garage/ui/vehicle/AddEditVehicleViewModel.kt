package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.data.repository.VinLookupResult
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AddEditVehicleUiState(
    val vin: String = "",
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val drivetrain: Drivetrain = Drivetrain.UNKNOWN,
    val imageFilename: String? = null,
    val imageFile: File? = null,
    val imageOffsetY: Float = 0f,
    val isDecodingVin: Boolean = false,
    val vinDecodeError: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val saveComplete: Boolean = false,
    val deleteComplete: Boolean = false,
    /** Specs decoded alongside the last successful VIN decode; persisted once the vehicle is saved. */
    val pendingSpecs: VehicleSpecs? = null,
)

@HiltViewModel
class AddEditVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val imageStorageManager: ImageStorageManager,
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(Destinations.VEHICLE_ID_ARG) ?: Destinations.NO_VEHICLE_ID
    private val isEditing = vehicleId != Destinations.NO_VEHICLE_ID

    private val _uiState = MutableStateFlow(AddEditVehicleUiState(isEditing = isEditing))
    val uiState: StateFlow<AddEditVehicleUiState> = _uiState.asStateFlow()

    // The filename the vehicle already had on disk when this editing session started (if
    // any). Deleting the *old* photo is deferred until a new one is actually persisted via
    // onSave() -- if we deleted it as soon as the user picked a replacement, backing out
    // without saving would silently and permanently destroy their original photo.
    private var originalImageFilename: String? = null

    init {
        if (isEditing) {
            viewModelScope.launch {
                vehicleRepository.getVehicleByIdOnce(vehicleId)?.let { vehicle ->
                    originalImageFilename = vehicle.imageUri
                    _uiState.update {
                        it.copy(
                            vin = vehicle.vin,
                            year = vehicle.year?.toString().orEmpty(),
                            make = vehicle.make,
                            model = vehicle.model,
                            trim = vehicle.trim,
                            drivetrain = vehicle.drivetrain,
                            imageFilename = vehicle.imageUri,
                            imageFile = vehicle.imageUri?.let(imageStorageManager::imageFile),
                            imageOffsetY = vehicle.imageOffsetY,
                        )
                    }
                }
            }
        }
    }

    fun onVinChanged(vin: String) {
        val normalized = vin.uppercase().take(17)
        _uiState.update { it.copy(vin = normalized, vinDecodeError = null) }
        if (normalized.length == 17) {
            decodeVin(normalized)
        }
    }

    fun onDecodeVinClicked() {
        val vin = _uiState.value.vin
        if (vin.isNotBlank()) decodeVin(vin)
    }

    private fun decodeVin(vin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDecodingVin = true, vinDecodeError = null) }
            when (val result = vehicleRepository.decodeVin(vin)) {
                is VinLookupResult.Success -> _uiState.update {
                    it.copy(
                        isDecodingVin = false,
                        year = result.info.year?.toString() ?: it.year,
                        make = result.info.make.ifBlank { it.make },
                        model = result.info.model.ifBlank { it.model },
                        trim = result.info.trim.ifBlank { it.trim },
                        drivetrain = result.info.drivetrain ?: it.drivetrain,
                        pendingSpecs = result.info.specs,
                    )
                }
                is VinLookupResult.Error -> _uiState.update {
                    it.copy(isDecodingVin = false, vinDecodeError = result.message)
                }
            }
        }
    }

    fun onYearChanged(year: String) = _uiState.update { it.copy(year = year.filter(Char::isDigit).take(4)) }
    fun onMakeChanged(make: String) = _uiState.update { it.copy(make = make) }
    fun onModelChanged(model: String) = _uiState.update { it.copy(model = model) }
    fun onTrimChanged(trim: String) = _uiState.update { it.copy(trim = trim) }
    fun onDrivetrainChanged(drivetrain: Drivetrain) = _uiState.update { it.copy(drivetrain = drivetrain) }
    fun onImageOffsetYChanged(offsetY: Float) = _uiState.update { it.copy(imageOffsetY = offsetY.coerceIn(-1f, 1f)) }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val filename = imageStorageManager.copyPickedImageToInternalStorage(uri)
            // Clean up a previously *picked-but-not-yet-saved* replacement from earlier in
            // this same session (if the user changed their mind and picked again) -- but
            // never touch originalImageFilename here; that's only deleted once onSave()
            // has actually persisted its replacement.
            _uiState.value.imageFilename
                ?.takeIf { it != originalImageFilename }
                ?.let(imageStorageManager::deleteImage)
            _uiState.update {
                it.copy(imageFilename = filename, imageFile = imageStorageManager.imageFile(filename), imageOffsetY = 0f)
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val savedVehicleId = vehicleRepository.saveVehicle(
                Vehicle(
                    id = if (isEditing) vehicleId else 0,
                    vin = state.vin,
                    year = state.year.toIntOrNull(),
                    make = state.make,
                    model = state.model,
                    trim = state.trim,
                    drivetrain = state.drivetrain,
                    imageUri = state.imageFilename,
                    imageOffsetY = state.imageOffsetY,
                ),
            )
            // Only persist specs when this session actually decoded a VIN; otherwise leave
            // whatever specs (if any) are already stored for this vehicle untouched.
            state.pendingSpecs?.let { specs ->
                vehicleRepository.saveVehicleSpecs(savedVehicleId, specs)
            }
            // Now that the new photo (if any) is safely referenced by the saved vehicle,
            // it's safe to clean up the old one it replaced.
            originalImageFilename
                ?.takeIf { it != state.imageFilename }
                ?.let(imageStorageManager::deleteImage)
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }

    fun onDeleteVehicle() {
        if (!isEditing) return
        viewModelScope.launch {
            _uiState.value.imageFilename?.let(imageStorageManager::deleteImage)
            vehicleRepository.getVehicleByIdOnce(vehicleId)?.let { vehicle ->
                vehicleRepository.deleteVehicle(vehicle)
            }
            _uiState.update { it.copy(deleteComplete = true) }
        }
    }
}
