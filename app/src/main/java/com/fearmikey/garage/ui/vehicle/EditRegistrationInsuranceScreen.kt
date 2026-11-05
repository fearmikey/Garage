package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.io.File

@Composable
fun EditRegistrationInsuranceScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditRegistrationInsuranceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAddOrEditClicked()
    }

    EditRegistrationInsuranceScreenContent(
        uiState = uiState,
        imageFileProvider = viewModel::imageFileFor,
        onBack = onBack,
        onLicensePlateChanged = viewModel::onLicensePlateChanged,
        onRegistrationStateChanged = viewModel::onRegistrationStateChanged,
        onRegistrationExpirationChanged = viewModel::onRegistrationExpirationChanged,
        onRegistrationFeeChanged = viewModel::onRegistrationFeeChanged,
        onRegistrationNotesChanged = viewModel::onRegistrationNotesChanged,
        onRegistrationImagePicked = viewModel::onRegistrationImagePicked,
        onRemoveRegistrationImage = viewModel::onRemoveRegistrationImage,
        onRegistrationImage2Picked = viewModel::onRegistrationImage2Picked,
        onRemoveRegistrationImage2 = viewModel::onRemoveRegistrationImage2,
        onInspectionExpirationChanged = viewModel::onInspectionExpirationChanged,
        onInspectionDateChanged = viewModel::onInspectionDateChanged,
        onInspectionResultChanged = viewModel::onInspectionResultChanged,
        onInspectionNotesChanged = viewModel::onInspectionNotesChanged,
        onEmissionsExpirationChanged = viewModel::onEmissionsExpirationChanged,
        onEmissionsDateChanged = viewModel::onEmissionsDateChanged,
        onEmissionsResultChanged = viewModel::onEmissionsResultChanged,
        onEmissionsNotesChanged = viewModel::onEmissionsNotesChanged,
        onInspectionStickerExpirationChanged = viewModel::onInspectionStickerExpirationChanged,
        onInspectionStickerNumberChanged = viewModel::onInspectionStickerNumberChanged,
        onInspectionStickerNotesChanged = viewModel::onInspectionStickerNotesChanged,
        onInsuranceProviderChanged = viewModel::onInsuranceProviderChanged,
        onPolicyNumberChanged = viewModel::onPolicyNumberChanged,
        onInsuranceExpirationChanged = viewModel::onInsuranceExpirationChanged,
        onInsurancePremiumChanged = viewModel::onInsurancePremiumChanged,
        onInsuranceAgentContactChanged = viewModel::onInsuranceAgentContactChanged,
        onInsuranceNotesChanged = viewModel::onInsuranceNotesChanged,
        onInsuranceImagePicked = viewModel::onInsuranceImagePicked,
        onRemoveInsuranceImage = viewModel::onRemoveInsuranceImage,
        onTollPassParkingExpirationChanged = viewModel::onTollPassParkingExpirationChanged,
        onTollPassParkingAccountChanged = viewModel::onTollPassParkingAccountChanged,
        onTollPassParkingNotesChanged = viewModel::onTollPassParkingNotesChanged,
        onDeleteRegistrationSection = viewModel::onDeleteRegistrationSection,
        onDeleteInspectionSection = viewModel::onDeleteInspectionSection,
        onDeleteEmissionsSection = viewModel::onDeleteEmissionsSection,
        onDeleteInspectionStickerSection = viewModel::onDeleteInspectionStickerSection,
        onDeleteInsuranceSection = viewModel::onDeleteInsuranceSection,
        onDeleteTollPassParkingSection = viewModel::onDeleteTollPassParkingSection,
        onPurchasePriceChanged = viewModel::onPurchasePriceChanged,
        onPurchaseDateChanged = viewModel::onPurchaseDateChanged,
        onPurchaseSellerChanged = viewModel::onPurchaseSellerChanged,
        onPurchaseNotesChanged = viewModel::onPurchaseNotesChanged,
        onPurchaseImagePicked = viewModel::onPurchaseImagePicked,
        onRemovePurchaseImage = viewModel::onRemovePurchaseImage,
        onDeletePurchaseSection = viewModel::onDeletePurchaseSection,
        onSave = {
            viewModel.onSave()
            onDone()
        },
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRegistrationInsuranceScreenContent(
    uiState: EditRegistrationInsuranceUiState,
    imageFileProvider: (String) -> File,
    onBack: () -> Unit,
    onLicensePlateChanged: (String) -> Unit,
    onRegistrationStateChanged: (String) -> Unit,
    onRegistrationExpirationChanged: (Long?) -> Unit,
    onRegistrationFeeChanged: (String) -> Unit,
    onRegistrationNotesChanged: (String) -> Unit,
    onRegistrationImagePicked: (Uri) -> Unit,
    onRemoveRegistrationImage: () -> Unit,
    onRegistrationImage2Picked: (Uri) -> Unit,
    onRemoveRegistrationImage2: () -> Unit,
    onInspectionExpirationChanged: (Long?) -> Unit,
    onInspectionDateChanged: (Long?) -> Unit,
    onInspectionResultChanged: (String) -> Unit,
    onInspectionNotesChanged: (String) -> Unit,
    onEmissionsExpirationChanged: (Long?) -> Unit,
    onEmissionsDateChanged: (Long?) -> Unit,
    onEmissionsResultChanged: (String) -> Unit,
    onEmissionsNotesChanged: (String) -> Unit,
    onInspectionStickerExpirationChanged: (Long?) -> Unit,
    onInspectionStickerNumberChanged: (String) -> Unit,
    onInspectionStickerNotesChanged: (String) -> Unit,
    onInsuranceProviderChanged: (String) -> Unit,
    onPolicyNumberChanged: (String) -> Unit,
    onInsuranceExpirationChanged: (Long?) -> Unit,
    onInsurancePremiumChanged: (String) -> Unit,
    onInsuranceAgentContactChanged: (String) -> Unit,
    onInsuranceNotesChanged: (String) -> Unit,
    onInsuranceImagePicked: (Uri) -> Unit,
    onRemoveInsuranceImage: () -> Unit,
    onTollPassParkingExpirationChanged: (Long?) -> Unit,
    onTollPassParkingAccountChanged: (String) -> Unit,
    onTollPassParkingNotesChanged: (String) -> Unit,
    onDeleteRegistrationSection: () -> Unit,
    onDeleteInspectionSection: () -> Unit,
    onDeleteEmissionsSection: () -> Unit,
    onDeleteInspectionStickerSection: () -> Unit,
    onDeleteInsuranceSection: () -> Unit,
    onDeleteTollPassParkingSection: () -> Unit,
    onPurchasePriceChanged: (String) -> Unit = {},
    onPurchaseDateChanged: (Long?) -> Unit = {},
    onPurchaseSellerChanged: (String) -> Unit = {},
    onPurchaseNotesChanged: (String) -> Unit = {},
    onPurchaseImagePicked: (Uri) -> Unit = {},
    onRemovePurchaseImage: () -> Unit = {},
    onDeletePurchaseSection: () -> Unit = {},
    onSave: () -> Unit,
) {
    var selectedSheetSegment by remember { mutableIntStateOf(0) }

    val regPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onRegistrationImagePicked) }

    val regPhoto2PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onRegistrationImage2Picked) }

    val insPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onInsuranceImagePicked) }

    val purchasePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onPurchaseImagePicked) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showDeletePurchaseConfirmDialog by remember { mutableStateOf(false) }

    var showRegDatePicker by remember { mutableStateOf(false) }
    var showInspExpDatePicker by remember { mutableStateOf(false) }
    var showInspDateDatePicker by remember { mutableStateOf(false) }
    var showEmissionsExpDatePicker by remember { mutableStateOf(false) }
    var showEmissionsDateDatePicker by remember { mutableStateOf(false) }
    var showStickerExpDatePicker by remember { mutableStateOf(false) }
    var showInsDatePicker by remember { mutableStateOf(false) }
    var showTollParkingExpDatePicker by remember { mutableStateOf(false) }

    var showDeleteRegConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteInspConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteEmissionsConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteStickerConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteInsConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteTollParkingConfirmDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Documents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedSheetSegment,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val tabTitles = listOf(
                    "Tag & Reg",
                    "Insurance",
                    "Inspection",
                    "Smog / Emissions",
                    "Stickers",
                    "Toll / Parking",
                    "Purchase",
                )
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSheetSegment == index,
                        onClick = { selectedSheetSegment = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedSheetSegment) {
                0 -> {
                    // Section 1: Tag & Registration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = uiState.licensePlate,
                            onValueChange = onLicensePlateChanged,
                            label = { Text(stringResource(R.string.doc_plate_label)) },
                            placeholder = { Text(stringResource(R.string.doc_plate_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1.8f),
                        )

                        OutlinedTextField(
                            value = uiState.registrationState,
                            onValueChange = onRegistrationStateChanged,
                            label = { Text(stringResource(R.string.doc_state_label)) },
                            placeholder = { Text(stringResource(R.string.doc_state_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = uiState.registrationExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.doc_reg_exp_label)) },
                        placeholder = { Text(stringResource(R.string.doc_select_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showRegDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Expiration Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRegDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.registrationFee,
                        onValueChange = onRegistrationFeeChanged,
                        label = { Text(stringResource(R.string.doc_renewal_fee_label)) },
                        prefix = { Text(uiState.currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.registrationNotes,
                        onValueChange = onRegistrationNotesChanged,
                        label = { Text(stringResource(R.string.doc_reg_notes_label)) },
                        placeholder = { Text("VIN on title, registration office notes...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ImagePickerBox(
                        title = "Registration Card – Front / Page 1",
                        pickedUri = uiState.pickedRegistrationImageUri,
                        existingFilename = uiState.registrationImageFilename,
                        imageFileProvider = imageFileProvider,
                        onPickImage = {
                            regPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemoveImage = onRemoveRegistrationImage,
                    )

                    ImagePickerBox(
                        title = "Registration Card – Back / Page 2",
                        pickedUri = uiState.pickedRegistrationImageUri2,
                        existingFilename = uiState.registrationImageFilename2,
                        imageFileProvider = imageFileProvider,
                        onPickImage = {
                            regPhoto2PickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemoveImage = onRemoveRegistrationImage2,
                    )

                    if (uiState.hasRegistrationData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteRegConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.doc_delete_reg_btn))
                        }
                    }
                }
                2 -> {
                    // Section 2: State Vehicle Inspection
                    OutlinedTextField(
                        value = uiState.inspectionExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.doc_insp_exp_label)) },
                        placeholder = { Text(stringResource(R.string.license_select_exp_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showInspExpDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Inspection Expiration")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInspExpDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.inspectionDate?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.doc_last_inspected_label)) },
                        placeholder = { Text(stringResource(R.string.doc_select_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showInspDateDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Last Inspected Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInspDateDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.inspectionResult,
                        onValueChange = onInspectionResultChanged,
                        label = { Text(stringResource(R.string.doc_insp_result_label)) },
                        placeholder = { Text(stringResource(R.string.doc_insp_result_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.inspectionNotes,
                        onValueChange = onInspectionNotesChanged,
                        label = { Text(stringResource(R.string.doc_insp_notes_label)) },
                        placeholder = { Text("Station name, certificate #...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.hasInspectionData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteInspConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.doc_delete_insp_btn))
                        }
                    }
                }
                3 -> {
                    // Section 3: Emissions / Smog Testing
                    OutlinedTextField(
                        value = uiState.emissionsExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Emissions Expiration / Due Date") },
                        placeholder = { Text("Select due date") },
                        trailingIcon = {
                            IconButton(onClick = { showEmissionsExpDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Emissions Expiration")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEmissionsExpDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.emissionsDate?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Last Smog Test Date") },
                        placeholder = { Text("Select date tested") },
                        trailingIcon = {
                            IconButton(onClick = { showEmissionsDateDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Last Smog Test Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEmissionsDateDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.emissionsResult,
                        onValueChange = onEmissionsResultChanged,
                        label = { Text("Test Result") },
                        placeholder = { Text("e.g., Passed, Failed, Exempt") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.emissionsNotes,
                        onValueChange = onEmissionsNotesChanged,
                        label = { Text("Emissions Notes / Station Details") },
                        placeholder = { Text("Station location, certificate #, notes...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.hasEmissionsData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteEmissionsConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Emissions & Smog Info")
                        }
                    }
                }
                4 -> {
                    // Section 4: Inspection Stickers & Decals
                    OutlinedTextField(
                        value = uiState.inspectionStickerExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sticker Renewal / Expiration Date") },
                        placeholder = { Text("Select expiration date") },
                        trailingIcon = {
                            IconButton(onClick = { showStickerExpDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Sticker Expiration")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStickerExpDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.inspectionStickerNumber,
                        onValueChange = onInspectionStickerNumberChanged,
                        label = { Text("Sticker / Decal Number") },
                        placeholder = { Text("e.g., STK-2025-9988") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.inspectionStickerNotes,
                        onValueChange = onInspectionStickerNotesChanged,
                        label = { Text("Sticker Notes / Location") },
                        placeholder = { Text("e.g., Windshield bottom left, local county decal...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.hasInspectionStickerData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteStickerConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Inspection Sticker Info")
                        }
                    }
                }
                1 -> {
                    // Section 5: Insurance Policy
                    OutlinedTextField(
                        value = uiState.insuranceProvider,
                        onValueChange = onInsuranceProviderChanged,
                        label = { Text("Insurance Company / Provider") },
                        placeholder = { Text("e.g., Geico, State Farm, Progressive") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.policyNumber,
                        onValueChange = onPolicyNumberChanged,
                        label = { Text("Insurance Policy Number") },
                        placeholder = { Text("e.g., POL-987654321") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.insuranceExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Policy Expiration") },
                        placeholder = { Text("Select date") },
                        trailingIcon = {
                            IconButton(onClick = { showInsDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Policy Expiration Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInsDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.insurancePremium,
                        onValueChange = onInsurancePremiumChanged,
                        label = { Text("Premium / Cost (Optional)") },
                        prefix = { Text(uiState.currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.insuranceAgentContact,
                        onValueChange = onInsuranceAgentContactChanged,
                        label = { Text("Agent / Claims Contact Info") },
                        placeholder = { Text("e.g., John Smith 555-0199 or 1-800-CLAIM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.insuranceNotes,
                        onValueChange = onInsuranceNotesChanged,
                        label = { Text("Policy Notes / Coverages") },
                        placeholder = { Text("Deductibles, roadside assistance, coverages...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ImagePickerBox(
                        title = "Insurance Card Photo",
                        pickedUri = uiState.pickedInsuranceImageUri,
                        existingFilename = uiState.insuranceImageFilename,
                        imageFileProvider = imageFileProvider,
                        onPickImage = {
                            insPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemoveImage = onRemoveInsuranceImage,
                    )

                    if (uiState.hasInsuranceData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteInsConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Insurance Policy Info")
                        }
                    }
                }
                5 -> {
                    // Section 6: Toll Pass & Parking Permits
                    OutlinedTextField(
                        value = uiState.tollPassParkingExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pass / Permit Expiration Date") },
                        placeholder = { Text("Select expiration date") },
                        trailingIcon = {
                            IconButton(onClick = { showTollParkingExpDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Toll Pass Expiration")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTollParkingExpDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.tollPassParkingAccount,
                        onValueChange = onTollPassParkingAccountChanged,
                        label = { Text("Account / Tag / Permit #") },
                        placeholder = { Text("e.g., EZPass #1234567, Resident Permit #88") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.tollPassParkingNotes,
                        onValueChange = onTollPassParkingNotesChanged,
                        label = { Text("Notes / Provider Details") },
                        placeholder = { Text("Transponder serial, parking spot #, zone notes...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.hasTollPassParkingData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteTollParkingConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Toll Pass & Parking Info")
                        }
                    }
                }
                6 -> {
                    // Section 7: Purchase / Bill of Sale
                    OutlinedTextField(
                        value = uiState.purchasePrice,
                        onValueChange = onPurchasePriceChanged,
                        label = { Text("Purchase Price") },
                        prefix = { Text(uiState.currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.purchaseDate?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Purchase Date") },
                        placeholder = { Text(stringResource(R.string.doc_select_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showPurchaseDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Purchase Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPurchaseDatePicker = true },
                    )

                    OutlinedTextField(
                        value = uiState.purchaseSeller,
                        onValueChange = onPurchaseSellerChanged,
                        label = { Text("Seller / Dealer") },
                        placeholder = { Text("e.g., Main Street Toyota, private sale") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.purchaseNotes,
                        onValueChange = onPurchaseNotesChanged,
                        label = { Text("Purchase Notes") },
                        placeholder = { Text("Trade-in, financing, taxes & fees...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ImagePickerBox(
                        title = "Bill of Sale",
                        pickedUri = uiState.pickedPurchaseImageUri,
                        existingFilename = uiState.purchaseImageFilename,
                        imageFileProvider = imageFileProvider,
                        onPickImage = {
                            purchasePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemoveImage = onRemovePurchaseImage,
                    )

                    if (uiState.hasPurchaseData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeletePurchaseConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Purchase Info")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Vehicle Documents")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRegDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.registrationExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showRegDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onRegistrationExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showRegDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showInspExpDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.inspectionExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showInspExpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onInspectionExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showInspExpDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInspExpDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showInspDateDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.inspectionDate?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showInspDateDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onInspectionDateChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showInspDateDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInspDateDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEmissionsExpDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.emissionsExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEmissionsExpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onEmissionsExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showEmissionsExpDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmissionsExpDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEmissionsDateDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.emissionsDate?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEmissionsDateDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onEmissionsDateChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showEmissionsDateDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmissionsDateDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStickerExpDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.inspectionStickerExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showStickerExpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onInspectionStickerExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showStickerExpDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickerExpDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showInsDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.insuranceExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showInsDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onInsuranceExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showInsDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTollParkingExpDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.tollPassParkingExpiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showTollParkingExpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onTollPassParkingExpirationChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showTollParkingExpDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTollParkingExpDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteRegConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRegConfirmDialog = false },
            title = { Text("Delete Tag & Registration Info?") },
            text = { Text("Are you sure you want to delete tag and registration details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteRegConfirmDialog = false
                        onDeleteRegistrationSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRegConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteInspConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteInspConfirmDialog = false },
            title = { Text("Delete State Inspection Info?") },
            text = { Text("Are you sure you want to delete state inspection details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteInspConfirmDialog = false
                        onDeleteInspectionSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteInspConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteEmissionsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteEmissionsConfirmDialog = false },
            title = { Text("Delete Emissions & Smog Info?") },
            text = { Text("Are you sure you want to delete emissions / smog test details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteEmissionsConfirmDialog = false
                        onDeleteEmissionsSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteEmissionsConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteStickerConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStickerConfirmDialog = false },
            title = { Text("Delete Inspection Sticker Info?") },
            text = { Text("Are you sure you want to delete inspection sticker details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStickerConfirmDialog = false
                        onDeleteInspectionStickerSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStickerConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteInsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteInsConfirmDialog = false },
            title = { Text("Delete Insurance Policy Info?") },
            text = { Text("Are you sure you want to delete insurance policy details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteInsConfirmDialog = false
                        onDeleteInsuranceSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteInsConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showPurchaseDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.purchaseDate?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPurchaseDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onPurchaseDateChanged(utcMillis.fromUtcDatePickerMillis())
                        }
                        showPurchaseDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeletePurchaseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePurchaseConfirmDialog = false },
            title = { Text("Delete Purchase Info?") },
            text = { Text("Are you sure you want to delete the purchase details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePurchaseConfirmDialog = false
                        onDeletePurchaseSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePurchaseConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteTollParkingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTollParkingConfirmDialog = false },
            title = { Text("Delete Toll Pass & Parking Info?") },
            text = { Text("Are you sure you want to delete toll pass and parking permit details for this vehicle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTollParkingConfirmDialog = false
                        onDeleteTollPassParkingSection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTollParkingConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

