package com.fearmikey.garage.ui.recall

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.RecallState
import com.fearmikey.garage.data.repository.RecallLookupResult
import com.fearmikey.garage.data.repository.RecallRepository
import com.fearmikey.garage.data.repository.RecallStateRepository
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecallItemUiState(
    val recall: VehicleRecall,
    val state: RecallState,
)

data class RecallsUiState(
    val isLoading: Boolean = false,
    val rawRecalls: List<VehicleRecall> = emptyList(),
    val recalls: List<RecallItemUiState> = emptyList(),
    val errorMessage: String? = null,
    val missingVehicleInfo: Boolean = false,
    val vin: String = "",
    val year: Int? = null,
    val make: String = "",
    val model: String = "",
)

@HiltViewModel
class RecallsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val recallRepository: RecallRepository,
    private val recallStateRepository: RecallStateRepository,
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _internalUiState = MutableStateFlow(RecallsUiState())

    val uiState: StateFlow<RecallsUiState> = combine(
        _internalUiState,
        recallStateRepository.getStatesForVehicle(vehicleId),
    ) { state, savedStates ->
        val stateMap = savedStates.associateBy({ it.campaignNumber }, { it.state })
        val mappedRecalls = state.rawRecalls.map { recall ->
            RecallItemUiState(
                recall = recall,
                state = stateMap[recall.campaignNumber] ?: RecallState.OPEN,
            )
        }.sortedBy { it.state != RecallState.OPEN }
        state.copy(recalls = mappedRecalls)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecallsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _internalUiState.value = _internalUiState.value.copy(isLoading = true, errorMessage = null)

            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val year = vehicle?.year
            val vin = vehicle?.vin.orEmpty()
            val make = vehicle?.make.orEmpty()
            val model = vehicle?.model.orEmpty()

            if ((vehicle == null) || (year == null) || make.isBlank() || model.isBlank()) {
                _internalUiState.value = _internalUiState.value.copy(
                    isLoading = false,
                    missingVehicleInfo = true,
                    rawRecalls = emptyList(),
                    vin = vin,
                    year = year,
                    make = make,
                    model = model,
                )
                return@launch
            }

            when (val result = recallRepository.getRecalls(year, make, model)) {
                is RecallLookupResult.Success -> {
                    _internalUiState.value = _internalUiState.value.copy(
                        isLoading = false,
                        rawRecalls = result.recalls,
                        missingVehicleInfo = false,
                        vin = vin,
                        year = year,
                        make = make,
                        model = model,
                    )
                }
                is RecallLookupResult.Error -> {
                    _internalUiState.value = _internalUiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        missingVehicleInfo = false,
                        vin = vin,
                        year = year,
                        make = make,
                        model = model,
                    )
                }
            }
        }
    }

    fun updateRecallState(campaignNumber: String, state: RecallState) {
        viewModelScope.launch {
            recallStateRepository.saveState(vehicleId, campaignNumber, state)
        }
    }
}
