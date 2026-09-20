package com.fearmikey.garage.ui.vehicle

import android.content.Context
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
import com.fearmikey.garage.notification.WorkScheduler
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VehiclePhotoItem(
    val filename: String? = null,
    val file: File? = null,
    val offsetY: Float = 0f,
)

data class AddEditVehicleUiState(
    val vin: String = "",
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val drivetrain: Drivetrain = Drivetrain.UNKNOWN,
    val photos: List<VehiclePhotoItem> = emptyList(),
    val isDecodingVin: Boolean = false,
    val vinDecodeError: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val saveComplete: Boolean = false,
    val deleteComplete: Boolean = false,
    /** Specs decoded alongside the last successful VIN decode; persisted once the vehicle is saved. */
    val pendingSpecs: VehicleSpecs? = null,
) {
    val imageFilename: String? get() = photos.firstOrNull()?.filename
    val imageFile: File? get() = photos.firstOrNull()?.file
    val imageOffsetY: Float get() = photos.firstOrNull()?.offsetY ?: 0f
}

@HiltViewModel
class AddEditVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val imageStorageManager: ImageStorageManager,
    @ApplicationContext private val context: Context? = null,
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(Destinations.VEHICLE_ID_ARG) ?: Destinations.NO_VEHICLE_ID
    private val isEditing = vehicleId != Destinations.NO_VEHICLE_ID

    private val _uiState = MutableStateFlow(AddEditVehicleUiState(isEditing = isEditing))
    val uiState: StateFlow<AddEditVehicleUiState> = _uiState.asStateFlow()

    // The filenames the vehicle had on disk when this editing session started.
    // Deleting old photos is deferred until saved via onSave().
    private var originalImageFilenames: List<String> = emptyList()

    init {
        if (isEditing) {
            viewModelScope.launch {
                vehicleRepository.getVehicleByIdOnce(vehicleId)?.let { vehicle ->
                    val loadedPhotos = vehicle.photos.map { photo ->
                        VehiclePhotoItem(
                            filename = photo.uri,
                            file = imageStorageManager.imageFile(photo.uri),
                            offsetY = photo.offsetY,
                        )
                    }
                    originalImageFilenames = vehicle.photos.map { it.uri }
                    _uiState.update {
                        it.copy(
                            vin = vehicle.vin,
                            year = vehicle.year?.toString().orEmpty(),
                            make = vehicle.make,
                            model = vehicle.model,
                            trim = vehicle.trim,
                            drivetrain = vehicle.drivetrain,
                            photos = loadedPhotos,
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

    fun onImagesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val currentPhotos = _uiState.value.photos.toMutableList()
            val availableSlots = (3 - currentPhotos.size).coerceAtLeast(0)
            for (uri in uris.take(availableSlots)) {
                val filename = imageStorageManager.copyPickedImageToInternalStorage(uri)
                val file = imageStorageManager.imageFile(filename)
                currentPhotos.add(VehiclePhotoItem(filename = filename, file = file, offsetY = 0f))
            }
            _uiState.update { it.copy(photos = currentPhotos) }
        }
    }

    fun onImagePicked(uri: Uri) = onImagesPicked(listOf(uri))

    fun onReplaceImagePicked(index: Int, uri: Uri) {
        viewModelScope.launch {
            val currentPhotos = _uiState.value.photos.toMutableList()
            if (index in currentPhotos.indices) {
                val oldItem = currentPhotos[index]
                val newFilename = imageStorageManager.copyPickedImageToInternalStorage(uri)
                val newFile = imageStorageManager.imageFile(newFilename)

                oldItem.filename?.let { oldName ->
                    if (oldName !in originalImageFilenames) {
                        imageStorageManager.deleteImage(oldName)
                    }
                }

                currentPhotos[index] = VehiclePhotoItem(filename = newFilename, file = newFile, offsetY = 0f)
                _uiState.update { it.copy(photos = currentPhotos) }
            }
        }
    }

    fun onRemovePhoto(index: Int) {
        val currentPhotos = _uiState.value.photos.toMutableList()
        if (index in currentPhotos.indices) {
            val removedItem = currentPhotos.removeAt(index)
            removedItem.filename?.let { name ->
                if (name !in originalImageFilenames) {
                    imageStorageManager.deleteImage(name)
                }
            }
            _uiState.update { it.copy(photos = currentPhotos) }
        }
    }

    fun onImageOffsetYChanged(index: Int, offsetY: Float) {
        val currentPhotos = _uiState.value.photos.toMutableList()
        if (index in currentPhotos.indices) {
            val clamped = offsetY.coerceIn(-1f, 1f)
            currentPhotos[index] = currentPhotos[index].copy(offsetY = clamped)
            _uiState.update { it.copy(photos = currentPhotos) }
        }
    }

    fun onImageOffsetYChanged(offsetY: Float) = onImageOffsetYChanged(0, offsetY)

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val photo1 = state.photos.getOrNull(0)
                val photo2 = state.photos.getOrNull(1)
                val photo3 = state.photos.getOrNull(2)

                val newOrUpdatedId = vehicleRepository.saveVehicle(
                    Vehicle(
                        id = if (isEditing) vehicleId else 0,
                        vin = state.vin,
                        year = state.year.toIntOrNull(),
                        make = state.make,
                        model = state.model,
                        trim = state.trim,
                        drivetrain = state.drivetrain,
                        imageUri = photo1?.filename,
                        imageOffsetY = photo1?.offsetY ?: 0f,
                        imageUri2 = photo2?.filename,
                        imageOffsetY2 = photo2?.offsetY ?: 0f,
                        imageUri3 = photo3?.filename,
                        imageOffsetY3 = photo3?.offsetY ?: 0f,
                    ),
                )
                val targetVehicleId = if (isEditing) vehicleId else newOrUpdatedId

                state.pendingSpecs?.let { specs ->
                    vehicleRepository.saveVehicleSpecs(targetVehicleId, specs)
                }

                val savedFilenames = state.photos.mapNotNull { it.filename }.toSet()
                originalImageFilenames.filter { it !in savedFilenames }.forEach { oldFilename ->
                    imageStorageManager.deleteImage(oldFilename)
                }

                context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, vinDecodeError = e.message ?: "An error occurred while saving the vehicle.")
                }
            }
        }
    }

    fun onDeleteVehicle() {
        if (!isEditing) return
        viewModelScope.launch {
            _uiState.value.photos.mapNotNull { it.filename }.forEach { filename ->
                imageStorageManager.deleteImage(filename)
            }
            vehicleRepository.getVehicleByIdOnce(vehicleId)?.let { vehicle ->
                vehicleRepository.deleteVehicle(vehicle)
            }
            context?.let { WorkScheduler.triggerImmediateReminderCheck(it) }
            _uiState.update { it.copy(deleteComplete = true) }
        }
    }
}
