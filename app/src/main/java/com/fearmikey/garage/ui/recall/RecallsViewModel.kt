package com.fearmikey.garage.ui.recall

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.repository.RecallLookupResult
import com.fearmikey.garage.data.repository.RecallRepository
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecallsUiState(
    val isLoading: Boolean = false,
    val recalls: List<VehicleRecall> = emptyList(),
    val errorMessage: String? = null,
    /** True when the vehicle is missing a year, make, or model needed to look up recalls. */
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
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    private val _uiState = MutableStateFlow(RecallsUiState())
    val uiState: StateFlow<RecallsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val vehicle = vehicleRepository.getVehicleByIdOnce(vehicleId)
            val year = vehicle?.year
            val vin = vehicle?.vin.orEmpty()
            val make = vehicle?.make.orEmpty()
            val model = vehicle?.model.orEmpty()

            if ((vehicle == null) || (year == null) || make.isBlank() || model.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        missingVehicleInfo = true,
                        recalls = emptyList(),
                        vin = vin,
                        year = year,
                        make = make,
                        model = model,
                    )
                }
                return@launch
            }

            when (val result = recallRepository.getRecalls(year, make, model)) {
                is RecallLookupResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        recalls = result.recalls,
                        missingVehicleInfo = false,
                        vin = vin,
                        year = year,
                        make = make,
                        model = model,
                    )
                }
                is RecallLookupResult.Error -> _uiState.update {
                    it.copy(
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
}
