package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.CurrencySelectionDialog
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.obd.ObdDevicePickerDialog
import com.fearmikey.garage.ui.obd.hasObdBluetoothPermissions
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPreferencesSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDefaultVehicleDialog by remember { mutableStateOf(false) }
    var showObdAdapterDialog by remember { mutableStateOf(false) }
    var showMileageRateDialog by remember { mutableStateOf(false) }

    val currencySymbol = AppCurrency.fromCode(uiState.currency).symbol
    val distanceUnit = UnitSystem.fromString(uiState.units).distanceUnit

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_user_preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScrollbar(listState),
        ) {
            item {
                val currentLocales = remember(showLanguageDialog) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales
                        } else null
                    } catch (_: Throwable) {
                        null
                    }
                }
                val currentLang = if (currentLocales == null || currentLocales.isEmpty) "" else currentLocales.get(0)?.language ?: ""
                val languageName = when (currentLang) {
                    "en" -> stringResource(R.string.language_english)
                    "fr" -> stringResource(R.string.language_french)
                    "de" -> stringResource(R.string.language_german)
                    "pl" -> stringResource(R.string.language_polish)
                    "es" -> stringResource(R.string.language_spanish)
                    "it" -> stringResource(R.string.language_italian)
                    "pt" -> stringResource(R.string.language_portuguese)
                    "nl" -> stringResource(R.string.language_dutch)
                    "ru" -> stringResource(R.string.language_russian)
                    "tr" -> stringResource(R.string.language_turkish)
                    else -> stringResource(R.string.language_system_default)
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_selection)) },
                    supportingContent = { Text(languageName) },
                    leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true },
                )
            }
            item {
                val unitLabel = when (uiState.units) {
                    "uk_mixed" -> "UK (Mixed)"
                    else -> uiState.units.replaceFirstChar { it.uppercase() }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_units)) },
                    supportingContent = { Text(unitLabel) },
                    leadingContent = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUnitsDialog = true },
                )
            }
            item {
                val selectedCurrencyObj = AppCurrency.fromCode(uiState.currency)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_currency)) },
                    supportingContent = { Text(selectedCurrencyObj.displayName) },
                    leadingContent = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Mileage Reimbursement Rate") },
                    supportingContent = {
                        Text(
                            "Default for new trips: $currencySymbol${
                                uiState.mileageReimbursementRate.toBigDecimal().stripTrailingZeros().toPlainString()
                            }/$distanceUnit"
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Commute, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMileageRateDialog = true },
                )
            }
            item {
                val selectedVehicleName = uiState.vehicles.find { it.id == uiState.defaultVehicleId }?.let { vehicle ->
                    listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
                        .joinToString(" ")
                        .ifBlank { vehicle.vin }
                } ?: "First vehicle added (default)"

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_vehicle)) },
                    supportingContent = { Text("Used for widget shortcuts ($selectedVehicleName)") },
                    leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDefaultVehicleDialog = true },
                )
            }
            item {
                val adapterText = uiState.savedObdAdapter?.let { adapter ->
                    stringResource(
                        R.string.settings_obd_adapter_summary,
                        adapter.name,
                        adapter.type.displayName,
                        adapter.address,
                    )
                } ?: stringResource(R.string.settings_obd_adapter_not_set)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_obd_adapter_title)) },
                    supportingContent = { Text(adapterText) },
                    leadingContent = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (hasObdBluetoothPermissions(context)) viewModel.loadPairedObdDevices()
                            showObdAdapterDialog = true
                        },
                )
            }
        }
    }

    if (showLanguageDialog) {
        val currentLocales = remember {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales
                } else null
            } catch (_: Throwable) {
                null
            }
        }
        val currentLang = if (currentLocales == null || currentLocales.isEmpty) "" else currentLocales.get(0)?.language ?: ""

        LanguageSelectionDialog(
            selectedLangCode = currentLang,
            onLanguageSelected = { langCode ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                        localeManager?.applicationLocales = LocaleListCompat.forLanguageTags(langCode).unwrap() as android.os.LocaleList
                    }
                } catch (_: Throwable) {
                    // Ignore if unsupported
                }
                showLanguageDialog = false
            },
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    if (showUnitsDialog) {
        SingleChoiceDialog(
            title = "Units of Measurement",
            options = listOf("metric", "imperial", "uk_mixed"),
            selectedOption = uiState.units,
            onOptionSelected = { viewModel.setUnits(it) },
            onDismissRequest = { showUnitsDialog = false }
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            selectedCurrencyCode = uiState.currency,
            onCurrencySelected = { viewModel.setCurrency(it) },
            onDismissRequest = { showCurrencyDialog = false },
        )
    }

    if (showMileageRateDialog) {
        MileageRateDialog(
            currentRate = uiState.mileageReimbursementRate,
            currencySymbol = currencySymbol,
            distanceUnit = distanceUnit,
            onSave = { rate ->
                viewModel.setMileageReimbursementRate(rate)
                showMileageRateDialog = false
            },
            onDismissRequest = { showMileageRateDialog = false },
        )
    }

    if (showDefaultVehicleDialog) {
        DefaultVehicleDialog(
            vehicles = uiState.vehicles,
            selectedVehicleId = uiState.defaultVehicleId,
            onVehicleSelected = { vehicleId ->
                viewModel.setDefaultVehicleId(vehicleId)
                showDefaultVehicleDialog = false
            },
            onDismissRequest = { showDefaultVehicleDialog = false },
        )
    }

    if (showObdAdapterDialog) {
        ObdDevicePickerDialog(
            pairedDevices = uiState.pairedObdDevices,
            bleDevices = uiState.bleObdDevices,
            isBleScanning = uiState.isObdBleScanning,
            selected = uiState.savedObdAdapter,
            onAdapterSelected = { config ->
                viewModel.setSavedObdAdapter(config)
                showObdAdapterDialog = false
            },
            onDismissRequest = {
                viewModel.stopObdBleScan()
                showObdAdapterDialog = false
            },
            onRefreshPaired = viewModel::loadPairedObdDevices,
            onStartBleScan = viewModel::startObdBleScan,
            onClearSelection = if (uiState.savedObdAdapter != null) {
                {
                    viewModel.clearSavedObdAdapter()
                    showObdAdapterDialog = false
                }
            } else {
                null
            },
        )
    }
}

/** Parses a user-entered rate, accepting either '.' or ',' as the decimal separator. */
internal fun parseReimbursementRate(input: String): Double? =
    input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 && it.isFinite() }

@Composable
internal fun MileageRateDialog(
    currentRate: Double,
    currencySymbol: String,
    distanceUnit: String,
    onSave: (Double) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var text by remember {
        mutableStateOf(currentRate.toBigDecimal().stripTrailingZeros().toPlainString())
    }
    val parsed = parseReimbursementRate(text)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Mileage Reimbursement Rate") },
        text = {
            Column {
                Text(
                    "Default rate prefilled when logging a new trip, in your selected currency " +
                        "and distance unit. Existing trips keep the rate they were saved with.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Rate ($currencySymbol/$distanceUnit)") },
                    prefix = { Text(currencySymbol) },
                    suffix = { Text("/$distanceUnit") },
                    isError = parsed == null,
                    supportingText = if (parsed == null) {
                        { Text("Enter a valid non-negative number") }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
