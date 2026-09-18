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

data class ModsUiState(
    val mods: List<ModificationRecord> = emptyList(),
    val totalCost: Double = 0.0,
    val isSheetOpen: Boolean = false,
    val editingModId: Long? = null,
    val title: String = "",
    val category: ModificationCategory = ModificationCategory.PERFORMANCE,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val cost: String = "",
    val pickedImageUri: Uri? = null,
    val currentImageFilename: String? = null,
    val imageFile: File? = null,
    val isImageRemoved: Boolean = false,
    val isSaving: Boolean = false,
    val currencySymbol: String = "$",
)

@HiltViewModel
class ModsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modificationRepository: ModificationRepository,
    private val imageStorageManager: ImageStorageManager,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _sheetState = MutableStateFlow(SheetState())

    val uiState: StateFlow<ModsUiState> = combine(
        modificationRepository.getModsForVehicle(vehicleId),
        preferencesRepository.appCurrency,
        _sheetState,
    ) { modsList, currency, sheet ->
        val total = modsList.sumOf { it.cost }
        val activeImageFile = when {
            sheet.isImageRemoved -> null
            sheet.pickedImageUri != null -> null // Picked URI handled separately in UI / AsyncImage
            sheet.currentImageFilename != null -> imageStorageManager.imageFile(sheet.currentImageFilename)
            else -> null
        }

        ModsUiState(
            mods = modsList,
            totalCost = total,
            isSheetOpen = sheet.isOpen,
            editingModId = sheet.editingModId,
            title = sheet.title,
            category = sheet.category,
            description = sheet.description,
            date = sheet.date,
            cost = sheet.cost,
            pickedImageUri = sheet.pickedImageUri,
            currentImageFilename = sheet.currentImageFilename,
            imageFile = activeImageFile,
            isImageRemoved = sheet.isImageRemoved,
            isSaving = sheet.isSaving,
            currencySymbol = currency.symbol,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModsUiState())

    fun imageFileFor(filename: String): File = imageStorageManager.imageFile(filename)

    fun onAddModClicked() {
        _sheetState.value = SheetState(isOpen = true)
    }

    fun onEditModClicked(mod: ModificationRecord) {
        _sheetState.value = SheetState(
            isOpen = true,
            editingModId = mod.id,
            title = mod.title,
            category = mod.category,
            description = mod.description,
            date = mod.date,
            cost = if (mod.cost > 0) "%.2f".format(mod.cost) else "",
            currentImageFilename = mod.imageUri,
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

    fun onImagePicked(uri: Uri) {
        _sheetState.update { it.copy(pickedImageUri = uri, isImageRemoved = false) }
    }

    fun onRemoveImage() {
        _sheetState.update { it.copy(pickedImageUri = null, isImageRemoved = true) }
    }

    fun onSaveMod() {
        val sheet = _sheetState.value
        if (sheet.title.isBlank()) return

        viewModelScope.launch {
            _sheetState.update { it.copy(isSaving = true) }
            val record = ModificationRecord(
                id = sheet.editingModId ?: 0,
                vehicleId = vehicleId,
                title = sheet.title.trim(),
                category = sheet.category,
                description = sheet.description.trim(),
                imageUri = sheet.currentImageFilename,
                date = sheet.date,
                cost = sheet.cost.toDoubleOrNull() ?: 0.0,
            )

            modificationRepository.saveMod(
                mod = record,
                newPickedUri = sheet.pickedImageUri,
                deleteExistingImage = sheet.isImageRemoved,
            )

            _sheetState.value = SheetState(isOpen = false)
        }
    }

    fun onDeleteMod(mod: ModificationRecord) {
        viewModelScope.launch {
            modificationRepository.deleteMod(mod)
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
        val pickedImageUri: Uri? = null,
        val currentImageFilename: String? = null,
        val isImageRemoved: Boolean = false,
        val isSaving: Boolean = false,
    )
}
