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
    val isDecodingVin: Boolean = false,
    val vinDecodeError: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val saveComplete: Boolean = false,
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

    init {
        if (isEditing) {
            viewModelScope.launch {
                vehicleRepository.getVehicleByIdOnce(vehicleId)?.let { vehicle ->
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

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val filename = imageStorageManager.copyPickedImageToInternalStorage(uri)
            // Best-effort cleanup of a previously picked (but since-replaced) image.
            _uiState.value.imageFilename?.let(imageStorageManager::deleteImage)
            _uiState.update {
                it.copy(imageFilename = filename, imageFile = imageStorageManager.imageFile(filename))
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
                ),
            )
            // Only persist specs when this session actually decoded a VIN; otherwise leave
            // whatever specs (if any) are already stored for this vehicle untouched.
            state.pendingSpecs?.let { specs ->
                vehicleRepository.saveVehicleSpecs(savedVehicleId, specs)
            }
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }
}
