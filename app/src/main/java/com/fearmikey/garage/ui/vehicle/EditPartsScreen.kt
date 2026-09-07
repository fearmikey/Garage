package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPartsUiState(
    val initialParts: VehiclePartsInfo? = null,
    val vehicle: Vehicle? = null,
    val specs: VehicleSpecs? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class EditPartsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<EditPartsUiState> = combine(
        vehicleRepository.getVehicleParts(vehicleId),
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
    ) { parts, vehicle, specs ->
        EditPartsUiState(
            initialParts = parts,
            vehicle = vehicle,
            specs = specs,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditPartsUiState())

    fun getEstimatedParts(): VehiclePartsInfo {
        val state = uiState.value
        return PartsEstimator.estimateParts(vehicleId, state.vehicle, state.specs)
    }

    fun saveParts(info: VehiclePartsInfo) {
        viewModelScope.launch {
            vehicleRepository.saveVehicleParts(info.copy(vehicleId = vehicleId))
        }
    }
}

@Composable
fun EditPartsScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditPartsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditPartsContent(
        uiState = uiState,
        vehicleId = viewModel.vehicleId,
        onBack = onBack,
        onGetEstimates = viewModel::getEstimatedParts,
        onSave = { info ->
            viewModel.saveParts(info)
            onDone()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPartsContent(
    uiState: EditPartsUiState,
    vehicleId: Long,
    onBack: () -> Unit,
    onGetEstimates: () -> VehiclePartsInfo,
    onSave: (VehiclePartsInfo) -> Unit,
) {
    if (uiState.isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Parts & Fluids") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val effectiveInitial = remember(uiState) {
        uiState.initialParts?.takeUnless { it.isEmpty() }
            ?: PartsEstimator.estimateParts(vehicleId, uiState.vehicle, uiState.specs)
    }

    var oilViscosity by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilViscosity.orEmpty()) }
    var oilCapacity by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilCapacity.orEmpty()) }
    var oilFilterPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilFilterPartNumber.orEmpty()) }
    var sparkPlugPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.sparkPlugPartNumber.orEmpty()) }
    var sparkPlugGap by remember(effectiveInitial) { mutableStateOf(effectiveInitial.sparkPlugGap.orEmpty()) }
    var tireSizeFront by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tireSizeFront.orEmpty()) }
    var tireSizeRear by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tireSizeRear.orEmpty()) }
    var tirePsiFront by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tirePsiFront.orEmpty()) }
    var tirePsiRear by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tirePsiRear.orEmpty()) }
    var wiperBladeSizeDriver by remember(effectiveInitial) { mutableStateOf(effectiveInitial.wiperBladeSizeDriver.orEmpty()) }
    var wiperBladeSizePassenger by remember(effectiveInitial) { mutableStateOf(effectiveInitial.wiperBladeSizePassenger.orEmpty()) }
    var wiperBladeSizeRear by remember(effectiveInitial) { mutableStateOf(effectiveInitial.wiperBladeSizeRear.orEmpty()) }

    var showAutoFillWarningDialog by remember { mutableStateOf(value = false) }

    fun applyEstimates(estimates: VehiclePartsInfo, overwriteAll: Boolean = false) {
        if (overwriteAll || oilViscosity.isBlank()) oilViscosity = estimates.oilViscosity.orEmpty()
        if (overwriteAll || oilCapacity.isBlank()) oilCapacity = estimates.oilCapacity.orEmpty()
        if (overwriteAll || sparkPlugGap.isBlank()) sparkPlugGap = estimates.sparkPlugGap.orEmpty()
        if (overwriteAll || tirePsiFront.isBlank()) tirePsiFront = estimates.tirePsiFront.orEmpty()
        if (overwriteAll || tirePsiRear.isBlank()) tirePsiRear = estimates.tirePsiRear.orEmpty()
        if (overwriteAll || wiperBladeSizeDriver.isBlank()) wiperBladeSizeDriver = estimates.wiperBladeSizeDriver.orEmpty()
        if (overwriteAll || wiperBladeSizePassenger.isBlank()) wiperBladeSizePassenger = estimates.wiperBladeSizePassenger.orEmpty()
        if (overwriteAll || wiperBladeSizeRear.isBlank()) wiperBladeSizeRear = estimates.wiperBladeSizeRear.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Parts & Fluids") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showAutoFillWarningDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Auto-fill estimated specs")
            }

            Text("Oil", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = oilViscosity,
                onValueChange = { oilViscosity = it },
                label = { Text("Oil viscosity") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = oilCapacity,
                onValueChange = { oilCapacity = it },
                label = { Text("Oil capacity") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = oilFilterPartNumber,
                onValueChange = { oilFilterPartNumber = it },
                label = { Text("Oil filter part #") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Ignition", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = sparkPlugPartNumber,
                onValueChange = { sparkPlugPartNumber = it },
                label = { Text("Spark plug part #") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sparkPlugGap,
                onValueChange = { sparkPlugGap = it },
                label = { Text("Spark plug gap") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Tires", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = tireSizeFront,
                onValueChange = { tireSizeFront = it },
                label = { Text("Front tire size") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tireSizeRear,
                onValueChange = { tireSizeRear = it },
                label = { Text("Rear tire size") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tirePsiFront,
                onValueChange = { tirePsiFront = it },
                label = { Text("Front tire PSI") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tirePsiRear,
                onValueChange = { tirePsiRear = it },
                label = { Text("Rear tire PSI") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Wipers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = wiperBladeSizeDriver,
                onValueChange = { wiperBladeSizeDriver = it },
                label = { Text("Driver wiper size") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = wiperBladeSizePassenger,
                onValueChange = { wiperBladeSizePassenger = it },
                label = { Text("Passenger wiper size") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = wiperBladeSizeRear,
                onValueChange = { wiperBladeSizeRear = it },
                label = { Text("Rear wiper size") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        VehiclePartsInfo(
                            vehicleId = vehicleId,
                            oilViscosity = oilViscosity.trim().ifBlank { null },
                            oilCapacity = oilCapacity.trim().ifBlank { null },
                            oilFilterPartNumber = oilFilterPartNumber.trim().ifBlank { null },
                            sparkPlugPartNumber = sparkPlugPartNumber.trim().ifBlank { null },
                            sparkPlugGap = sparkPlugGap.trim().ifBlank { null },
                            tireSizeFront = tireSizeFront.trim().ifBlank { null },
                            tireSizeRear = tireSizeRear.trim().ifBlank { null },
                            tirePsiFront = tirePsiFront.trim().ifBlank { null },
                            tirePsiRear = tirePsiRear.trim().ifBlank { null },
                            wiperBladeSizeDriver = wiperBladeSizeDriver.trim().ifBlank { null },
                            wiperBladeSizePassenger = wiperBladeSizePassenger.trim().ifBlank { null },
                            wiperBladeSizeRear = wiperBladeSizeRear.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }

    if (showAutoFillWarningDialog) {
        AlertDialog(
            onDismissRequest = { showAutoFillWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("Verify with Owner's Manual") },
            text = {
                Text(
                    "Auto-filled fluid capacities and part specs are estimated using decoded VIN and engine data.\n\n" +
                        "Please double-check all values with your official owner's manual to ensure accuracy before purchasing fluids or servicing your vehicle.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoFillWarningDialog = false
                        applyEstimates(onGetEstimates(), overwriteAll = true)
                    },
                ) {
                    Text("Apply & Understand")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAutoFillWarningDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
