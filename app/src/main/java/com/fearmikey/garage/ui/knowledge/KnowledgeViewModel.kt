package com.fearmikey.garage.ui.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote
import com.fearmikey.garage.data.repository.KnowledgeRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: KnowledgeRepository
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val torqueSpecs: StateFlow<List<TorqueSpec>> = repository.getTorqueSpecs(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vehicleNotes: StateFlow<List<VehicleNote>> = repository.getVehicleNotes(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTorqueSpec(spec: TorqueSpec) {
        viewModelScope.launch {
            if (spec.id == 0L) {
                repository.insertTorqueSpec(spec.copy(vehicleId = vehicleId))
            } else {
                repository.updateTorqueSpec(spec.copy(vehicleId = vehicleId))
            }
        }
    }

    fun deleteTorqueSpec(spec: TorqueSpec) {
        viewModelScope.launch {
            repository.deleteTorqueSpec(spec)
        }
    }

    fun saveVehicleNote(note: VehicleNote) {
        viewModelScope.launch {
            val updatedNote = note.copy(
                vehicleId = vehicleId, 
                lastUpdated = System.currentTimeMillis()
            )
            if (updatedNote.id == 0L) {
                repository.insertVehicleNote(updatedNote)
            } else {
                repository.updateVehicleNote(updatedNote)
            }
        }
    }

    fun deleteVehicleNote(note: VehicleNote) {
        viewModelScope.launch {
            repository.deleteVehicleNote(note)
        }
    }
}
