package com.fearmikey.garage.ui.mod

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModPhotoItem(
    val filename: String? = null,
    val file: File? = null,
)

data class ModsUiState(
    val mods: List<ModificationRecord> = emptyList(),
    val totalCost: Double = 0.0,
    val isGridView: Boolean = true,
    val viewingMod: ModificationRecord? = null,
    val isSheetOpen: Boolean = false,
    val editingModId: Long? = null,
    val title: String = "",
    val category: ModificationCategory = ModificationCategory.PERFORMANCE,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val cost: String = "",
    val productUrl: String = "",
    val photos: List<ModPhotoItem> = emptyList(),
    val isSaving: Boolean = false,
    val currencySymbol: String = "$",
) {
    val pickedImageUri: Uri? get() = null
    val currentImageFilename: String? get() = photos.firstOrNull()?.filename
    val imageFile: File? get() = photos.firstOrNull()?.file
    val isImageRemoved: Boolean get() = false
}

@HiltViewModel
class ModsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modificationRepository: ModificationRepository,
    private val imageStorageManager: ImageStorageManager,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _sheetState = MutableStateFlow(SheetState())
    private val _viewingMod = MutableStateFlow<ModificationRecord?>(null)

    val uiState: StateFlow<ModsUiState> = combine(
        modificationRepository.getModsForVehicle(vehicleId),
        preferencesRepository.appCurrency,
        preferencesRepository.isModsGridView,
        _sheetState,
        _viewingMod,
    ) { modsList, currency, isGrid, sheet, viewing ->
        val total = modsList.sumOf { it.cost }

        ModsUiState(
            mods = modsList,
            totalCost = total,
            isGridView = isGrid,
            viewingMod = viewing,
            isSheetOpen = sheet.isOpen,
            editingModId = sheet.editingModId,
            title = sheet.title,
            category = sheet.category,
            description = sheet.description,
            date = sheet.date,
            cost = sheet.cost,
            productUrl = sheet.productUrl,
            photos = sheet.photos,
            isSaving = sheet.isSaving,
            currencySymbol = currency.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModsUiState())

    fun imageFileFor(filename: String): File = imageStorageManager.imageFile(filename)

    fun onToggleViewMode(isGrid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setModsGridView(isGrid)
        }
    }

    fun onModClicked(mod: ModificationRecord) {
        _viewingMod.value = mod
    }

    fun onDismissViewSheet() {
        _viewingMod.value = null
    }

    fun onAddModClicked() {
        _viewingMod.value = null
        _sheetState.value = SheetState(isOpen = true)
    }

    fun onEditModClicked(mod: ModificationRecord) {
        _viewingMod.value = null
        val loadedPhotos = mod.imageUris.map { filename ->
            ModPhotoItem(filename = filename, file = imageStorageManager.imageFile(filename))
        }
        _sheetState.value = SheetState(
            isOpen = true,
            editingModId = mod.id,
            title = mod.title,
            category = mod.category,
            description = mod.description,
            date = mod.date,
            cost = if (mod.cost > 0) "%.2f".format(mod.cost) else "",
            productUrl = mod.productUrl,
            photos = loadedPhotos,
            originalImageFilenames = mod.imageUris,
        )
    }

    fun onDismissSheet() {
        _sheetState.update { it.copy(isOpen = false) }
    }

    fun onTitleChanged(title: String) {
        _sheetState.update { it.copy(title = title) }
    }

    fun onCategoryChanged(category: ModificationCategory) {
        _sheetState.update { it.copy(category = category) }
    }

    fun onDescriptionChanged(description: String) {
        _sheetState.update { it.copy(description = description) }
    }

    fun onDateChanged(date: Long) {
        _sheetState.update { it.copy(date = date) }
    }

    fun onCostChanged(cost: String) {
        _sheetState.update { it.copy(cost = cost) }
    }

    fun onProductUrlChanged(productUrl: String) {
        _sheetState.update { it.copy(productUrl = productUrl) }
    }

    fun onImagesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val currentPhotos = _sheetState.value.photos.toMutableList()
            val availableSlots = (6 - currentPhotos.size).coerceAtLeast(0)
            for (uri in uris.take(availableSlots)) {
                val filename = imageStorageManager.copyPickedImageToInternalStorage(uri)
                val file = imageStorageManager.imageFile(filename)
                currentPhotos.add(ModPhotoItem(filename = filename, file = file))
            }
            _sheetState.update { it.copy(photos = currentPhotos) }
        }
    }

    fun onImagePicked(uri: Uri) = onImagesPicked(listOf(uri))

    fun onReplaceImagePicked(index: Int, uri: Uri) {
        viewModelScope.launch {
            val currentPhotos = _sheetState.value.photos.toMutableList()
            if (index in currentPhotos.indices) {
                val oldItem = currentPhotos[index]
                val newFilename = imageStorageManager.copyPickedImageToInternalStorage(uri)
                val newFile = imageStorageManager.imageFile(newFilename)

                oldItem.filename?.let { name ->
                    if (name !in _sheetState.value.originalImageFilenames) {
                        imageStorageManager.deleteImage(name)
                    }
                }

                currentPhotos[index] = ModPhotoItem(filename = newFilename, file = newFile)
                _sheetState.update { it.copy(photos = currentPhotos) }
            }
        }
    }

    fun onRemovePhoto(index: Int) {
        val currentPhotos = _sheetState.value.photos.toMutableList()
        if (index in currentPhotos.indices) {
            val removedItem = currentPhotos.removeAt(index)
            removedItem.filename?.let { name ->
                if (name !in _sheetState.value.originalImageFilenames) {
                    imageStorageManager.deleteImage(name)
                }
            }
            _sheetState.update { it.copy(photos = currentPhotos) }
        }
    }

    fun onRemoveImage() = onRemovePhoto(0)

    fun onSaveMod() {
        val sheet = _sheetState.value
        if (sheet.title.isBlank()) return

        viewModelScope.launch {
            _sheetState.update { it.copy(isSaving = true) }
            val p1 = sheet.photos.getOrNull(0)?.filename
            val p2 = sheet.photos.getOrNull(1)?.filename
            val p3 = sheet.photos.getOrNull(2)?.filename
            val p4 = sheet.photos.getOrNull(3)?.filename
            val p5 = sheet.photos.getOrNull(4)?.filename
            val p6 = sheet.photos.getOrNull(5)?.filename

            val record = ModificationRecord(
                id = sheet.editingModId ?: 0,
                vehicleId = vehicleId,
                title = sheet.title.trim(),
                category = sheet.category,
                description = sheet.description.trim(),
                imageUri = p1,
                imageUri2 = p2,
                imageUri3 = p3,
                imageUri4 = p4,
                imageUri5 = p5,
                imageUri6 = p6,
                date = sheet.date,
                cost = sheet.cost.toDoubleOrNull() ?: 0.0,
                productUrl = sheet.productUrl.trim(),
            )

            modificationRepository.saveMod(record)

            val savedFilenames = sheet.photos.mapNotNull { it.filename }.toSet()
            sheet.originalImageFilenames.filter { it !in savedFilenames }.forEach { oldFilename ->
                imageStorageManager.deleteImage(oldFilename)
            }

            _sheetState.value = SheetState(isOpen = false)
        }
    }

    fun onDeleteMod(mod: ModificationRecord) {
        viewModelScope.launch {
            modificationRepository.deleteMod(mod)
            if (_viewingMod.value?.id == mod.id) {
                _viewingMod.value = null
            }
            if (_sheetState.value.editingModId == mod.id) {
                _sheetState.value = SheetState(isOpen = false)
            }
        }
    }

    private data class SheetState(
        val isOpen: Boolean = false,
        val editingModId: Long? = null,
        val title: String = "",
        val category: ModificationCategory = ModificationCategory.PERFORMANCE,
        val description: String = "",
        val date: Long = System.currentTimeMillis(),
        val cost: String = "",
        val productUrl: String = "",
        val photos: List<ModPhotoItem> = emptyList(),
        val originalImageFilenames: List<String> = emptyList(),
        val isSaving: Boolean = false,
    )
}
