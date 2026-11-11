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
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.components.verticalScrollbar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import com.fearmikey.garage.ui.util.UnitSystem
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
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val isLoading: Boolean = true,
)

@HiltViewModel
class EditPartsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val vehicleId: Long = checkNotNull(savedStateHandle[Destinations.VEHICLE_ID_ARG])

    val uiState: StateFlow<EditPartsUiState> = combine(
        vehicleRepository.getVehicleParts(vehicleId),
        vehicleRepository.getVehicleById(vehicleId),
        vehicleRepository.getVehicleSpecs(vehicleId),
        preferencesRepository.unitSystem,
    ) { parts, vehicle, specs, unitSystem ->
        EditPartsUiState(
            initialParts = parts,
            vehicle = vehicle,
            specs = specs,
            unitSystem = unitSystem,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditPartsUiState())

    fun getEstimatedParts(): VehiclePartsInfo {
        val state = uiState.value
        return PartsEstimator.estimateParts(vehicleId, state.vehicle, state.specs, state.unitSystem)
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
                    title = { Text(stringResource(R.string.parts_edit_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            ?: PartsEstimator.estimateParts(vehicleId, uiState.vehicle, uiState.specs, uiState.unitSystem)
    }

    var oilViscosity by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilViscosity.orEmpty()) }
    var oilCapacity by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilCapacity.orEmpty()) }
    var oilFilterPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.oilFilterPartNumber.orEmpty()) }
    var sparkPlugPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.sparkPlugPartNumber.orEmpty()) }
    var sparkPlugGap by remember(effectiveInitial) { mutableStateOf(effectiveInitial.sparkPlugGap.orEmpty()) }
    var engineAirFilterPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.engineAirFilterPartNumber.orEmpty()) }
    var cabinAirFilterPartNumber by remember(effectiveInitial) { mutableStateOf(effectiveInitial.cabinAirFilterPartNumber.orEmpty()) }
    var tireSizeFront by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tireSizeFront.orEmpty()) }
    var tireSizeRear by remember(effectiveInitial) { mutableStateOf(effectiveInitial.tireSizeRear.orEmpty()) }
    // Tire pressures are edited as numbers in the user's unit (bar or PSI), matching the Parts list.
    val unitSystem = uiState.unitSystem
    val pressureUnit = UnitConverter.tirePressureUnit(unitSystem)
    val wiperUnit = UnitConverter.wiperSizeUnit(unitSystem)
    var tirePsiFront by remember(effectiveInitial, unitSystem) { mutableStateOf(UnitConverter.tirePressureForInput(effectiveInitial.tirePsiFront, unitSystem)) }
    var tirePsiRear by remember(effectiveInitial, unitSystem) { mutableStateOf(UnitConverter.tirePressureForInput(effectiveInitial.tirePsiRear, unitSystem)) }
    var wiperBladeSizeDriver by remember(effectiveInitial, unitSystem) { mutableStateOf(UnitConverter.wiperSizeForInput(effectiveInitial.wiperBladeSizeDriver, unitSystem)) }
    var wiperBladeSizePassenger by remember(effectiveInitial, unitSystem) { mutableStateOf(UnitConverter.wiperSizeForInput(effectiveInitial.wiperBladeSizePassenger, unitSystem)) }
    var wiperBladeSizeRear by remember(effectiveInitial, unitSystem) { mutableStateOf(UnitConverter.wiperSizeForInput(effectiveInitial.wiperBladeSizeRear, unitSystem)) }

    var showAutoFillWarningDialog by remember { mutableStateOf(value = false) }
    // Cabin air filters and wipers don't exist on motorcycles.
    val isMotorcycle = uiState.vehicle?.isMotorcycle == true

    fun applyEstimates(estimates: VehiclePartsInfo, overwriteAll: Boolean = false) {
        if (overwriteAll || oilViscosity.isBlank()) oilViscosity = estimates.oilViscosity.orEmpty()
        if (overwriteAll || oilCapacity.isBlank()) oilCapacity = estimates.oilCapacity.orEmpty()
        if (overwriteAll || sparkPlugGap.isBlank()) sparkPlugGap = estimates.sparkPlugGap.orEmpty()
        if (overwriteAll || engineAirFilterPartNumber.isBlank()) engineAirFilterPartNumber = estimates.engineAirFilterPartNumber.orEmpty()
        if (overwriteAll || cabinAirFilterPartNumber.isBlank()) cabinAirFilterPartNumber = estimates.cabinAirFilterPartNumber.orEmpty()
        if (overwriteAll || tirePsiFront.isBlank()) tirePsiFront = UnitConverter.tirePressureForInput(estimates.tirePsiFront, unitSystem)
        if (overwriteAll || tirePsiRear.isBlank()) tirePsiRear = UnitConverter.tirePressureForInput(estimates.tirePsiRear, unitSystem)
        if (overwriteAll || wiperBladeSizeDriver.isBlank()) wiperBladeSizeDriver = UnitConverter.wiperSizeForInput(estimates.wiperBladeSizeDriver, unitSystem)
        if (overwriteAll || wiperBladeSizePassenger.isBlank()) wiperBladeSizePassenger = UnitConverter.wiperSizeForInput(estimates.wiperBladeSizePassenger, unitSystem)
        if (overwriteAll || wiperBladeSizeRear.isBlank()) wiperBladeSizeRear = UnitConverter.wiperSizeForInput(estimates.wiperBladeSizeRear, unitSystem)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parts_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showAutoFillWarningDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.parts_autofill))
            }

            Text(stringResource(R.string.parts_group_oil), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = oilViscosity,
                onValueChange = { oilViscosity = it },
                label = { Text(stringResource(R.string.parts_oil_viscosity)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = oilCapacity,
                onValueChange = { oilCapacity = it },
                label = { Text(stringResource(R.string.parts_oil_capacity)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = oilFilterPartNumber,
                onValueChange = { oilFilterPartNumber = it },
                label = { Text(stringResource(R.string.parts_oil_filter_num)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.parts_group_ignition), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = sparkPlugPartNumber,
                onValueChange = { sparkPlugPartNumber = it },
                label = { Text(stringResource(R.string.parts_spark_plug_num)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sparkPlugGap,
                onValueChange = { sparkPlugGap = it },
                label = { Text(stringResource(R.string.parts_spark_plug_gap)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.parts_group_filters), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = engineAirFilterPartNumber,
                onValueChange = { engineAirFilterPartNumber = it },
                label = { Text(stringResource(R.string.parts_engine_air_filter_num)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!isMotorcycle) {
                OutlinedTextField(
                    value = cabinAirFilterPartNumber,
                    onValueChange = { cabinAirFilterPartNumber = it },
                    label = { Text(stringResource(R.string.parts_cabin_air_filter_num)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.parts_group_tires), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = tireSizeFront,
                onValueChange = { tireSizeFront = it },
                label = { Text(stringResource(R.string.parts_front_tire_size)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tireSizeRear,
                onValueChange = { tireSizeRear = it },
                label = { Text(stringResource(R.string.parts_rear_tire_size)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tirePsiFront,
                onValueChange = { tirePsiFront = it },
                label = { Text(stringResource(R.string.parts_front_tire_psi)) },
                suffix = { Text(pressureUnit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tirePsiRear,
                onValueChange = { tirePsiRear = it },
                label = { Text(stringResource(R.string.parts_rear_tire_psi)) },
                suffix = { Text(pressureUnit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!isMotorcycle) {
                Text(stringResource(R.string.parts_group_wipers), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = wiperBladeSizeDriver,
                    onValueChange = { wiperBladeSizeDriver = it },
                    label = { Text(stringResource(R.string.parts_driver_wiper)) },
                    suffix = { Text(wiperUnit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wiperBladeSizePassenger,
                    onValueChange = { wiperBladeSizePassenger = it },
                    label = { Text(stringResource(R.string.parts_passenger_wiper)) },
                    suffix = { Text(wiperUnit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wiperBladeSizeRear,
                    onValueChange = { wiperBladeSizeRear = it },
                    label = { Text(stringResource(R.string.parts_rear_wiper)) },
                    suffix = { Text(wiperUnit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                            engineAirFilterPartNumber = engineAirFilterPartNumber.trim().ifBlank { null },
                            cabinAirFilterPartNumber = cabinAirFilterPartNumber.trim().ifBlank { null }?.takeUnless { isMotorcycle },
                            tireSizeFront = tireSizeFront.trim().ifBlank { null },
                            tireSizeRear = tireSizeRear.trim().ifBlank { null },
                            tirePsiFront = UnitConverter.tirePressureFromInput(tirePsiFront, unitSystem),
                            tirePsiRear = UnitConverter.tirePressureFromInput(tirePsiRear, unitSystem),
                            wiperBladeSizeDriver = UnitConverter.wiperSizeFromInput(wiperBladeSizeDriver, unitSystem)?.takeUnless { isMotorcycle },
                            wiperBladeSizePassenger = UnitConverter.wiperSizeFromInput(wiperBladeSizePassenger, unitSystem)?.takeUnless { isMotorcycle },
                            wiperBladeSizeRear = UnitConverter.wiperSizeFromInput(wiperBladeSizeRear, unitSystem)?.takeUnless { isMotorcycle },
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_save))
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
            title = { Text(stringResource(R.string.parts_verify_title)) },
            text = {
                Text(
                    stringResource(R.string.parts_autofill_msg),
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
                    Text(stringResource(R.string.parts_apply_understand))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAutoFillWarningDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
