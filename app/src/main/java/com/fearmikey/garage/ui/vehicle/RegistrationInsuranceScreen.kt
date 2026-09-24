package com.fearmikey.garage.ui.vehicle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.theme.StatusOk
import com.fearmikey.garage.ui.theme.StatusOverdue
import com.fearmikey.garage.ui.theme.StatusUpcoming
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun RegistrationInsuranceScreen(
    viewModel: RegistrationInsuranceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegistrationInsuranceContent(
        uiState = uiState,
        imageFileProvider = viewModel::imageFileFor,
        onAddOrEditClicked = viewModel::onAddOrEditClicked,
        onDismissSheet = viewModel::onDismissSheet,
        onLicensePlateChanged = viewModel::onLicensePlateChanged,
        onRegistrationStateChanged = viewModel::onRegistrationStateChanged,
        onRegistrationExpirationChanged = viewModel::onRegistrationExpirationChanged,
        onRegistrationFeeChanged = viewModel::onRegistrationFeeChanged,
        onRegistrationNotesChanged = viewModel::onRegistrationNotesChanged,
        onRegistrationImagePicked = viewModel::onRegistrationImagePicked,
        onRemoveRegistrationImage = viewModel::onRemoveRegistrationImage,
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
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationInsuranceContent(
    uiState: RegistrationInsuranceUiState,
    imageFileProvider: (String) -> File,
    onAddOrEditClicked: () -> Unit,
    onDismissSheet: () -> Unit,
    onLicensePlateChanged: (String) -> Unit,
    onRegistrationStateChanged: (String) -> Unit,
    onRegistrationExpirationChanged: (Long?) -> Unit,
    onRegistrationFeeChanged: (String) -> Unit,
    onRegistrationNotesChanged: (String) -> Unit,
    onRegistrationImagePicked: (Uri) -> Unit,
    onRemoveRegistrationImage: () -> Unit,
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
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var previewImageFile by remember { mutableStateOf<File?>(null) }

    val record = uiState.record
    val hasData = (record != null) && !record.isEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddOrEditClicked,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = if (hasData) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (hasData) "Edit details" else "Add vehicle documents",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!hasData) {
                EmptyState(
                    message = "No vehicle documents added yet.\nTap + to track tag registration, state inspection, emissions, stickers, insurance, or toll passes.",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                )
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        RegistrationCard(
                            record = record,
                            currencySymbol = uiState.currencySymbol,
                            imageFileProvider = imageFileProvider,
                            onImageClicked = { previewImageFile = it },
                        )
                    }

                    item {
                        InspectionCard(
                            record = record,
                        )
                    }

                    item {
                        EmissionsCard(
                            record = record,
                        )
                    }

                    item {
                        InspectionStickerCard(
                            record = record,
                        )
                    }

                    item {
                        InsuranceCard(
                            record = record,
                            currencySymbol = uiState.currencySymbol,
                            imageFileProvider = imageFileProvider,
                            onImageClicked = { previewImageFile = it },
                        )
                    }

                    item {
                        TollParkingCard(
                            record = record,
                        )
                    }
                }
            }
        }
    }

    if (previewImageFile != null) {
        ImagePreviewDialog(
            imageFile = previewImageFile!!,
            onDismiss = { previewImageFile = null },
        )
    }

    if (uiState.isSheetOpen) {
        AddEditRegistrationInsuranceSheet(
            uiState = uiState,
            imageFileProvider = imageFileProvider,
            onDismiss = onDismissSheet,
            onLicensePlateChanged = onLicensePlateChanged,
            onRegistrationStateChanged = onRegistrationStateChanged,
            onRegistrationExpirationChanged = onRegistrationExpirationChanged,
            onRegistrationFeeChanged = onRegistrationFeeChanged,
            onRegistrationNotesChanged = onRegistrationNotesChanged,
            onRegistrationImagePicked = onRegistrationImagePicked,
            onRemoveRegistrationImage = onRemoveRegistrationImage,
            onInspectionExpirationChanged = onInspectionExpirationChanged,
            onInspectionDateChanged = onInspectionDateChanged,
            onInspectionResultChanged = onInspectionResultChanged,
            onInspectionNotesChanged = onInspectionNotesChanged,
            onEmissionsExpirationChanged = onEmissionsExpirationChanged,
            onEmissionsDateChanged = onEmissionsDateChanged,
            onEmissionsResultChanged = onEmissionsResultChanged,
            onEmissionsNotesChanged = onEmissionsNotesChanged,
            onInspectionStickerExpirationChanged = onInspectionStickerExpirationChanged,
            onInspectionStickerNumberChanged = onInspectionStickerNumberChanged,
            onInspectionStickerNotesChanged = onInspectionStickerNotesChanged,
            onInsuranceProviderChanged = onInsuranceProviderChanged,
            onPolicyNumberChanged = onPolicyNumberChanged,
            onInsuranceExpirationChanged = onInsuranceExpirationChanged,
            onInsurancePremiumChanged = onInsurancePremiumChanged,
            onInsuranceAgentContactChanged = onInsuranceAgentContactChanged,
            onInsuranceNotesChanged = onInsuranceNotesChanged,
            onInsuranceImagePicked = onInsuranceImagePicked,
            onRemoveInsuranceImage = onRemoveInsuranceImage,
            onTollPassParkingExpirationChanged = onTollPassParkingExpirationChanged,
            onTollPassParkingAccountChanged = onTollPassParkingAccountChanged,
            onTollPassParkingNotesChanged = onTollPassParkingNotesChanged,
            onDeleteRegistrationSection = onDeleteRegistrationSection,
            onDeleteInspectionSection = onDeleteInspectionSection,
            onDeleteEmissionsSection = onDeleteEmissionsSection,
            onDeleteInspectionStickerSection = onDeleteInspectionStickerSection,
            onDeleteInsuranceSection = onDeleteInsuranceSection,
            onDeleteTollPassParkingSection = onDeleteTollPassParkingSection,
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun RegistrationCard(
    record: VehicleRegistrationInsurance,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onImageClicked: (File) -> Unit,
) {
    if (record.licensePlate.isNullOrBlank() &&
        record.registrationState.isNullOrBlank() &&
        record.registrationExpiration == null &&
        record.registrationFee == null &&
        record.registrationNotes.isNullOrBlank() &&
        record.registrationImageUri.isNullOrBlank()
    ) return

    val regImageFile = remember(record.registrationImageUri) {
        record.registrationImageUri?.let(imageFileProvider)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Vehicle & Tag Registration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.registrationExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            if (!record.licensePlate.isNullOrBlank() || !record.registrationState.isNullOrBlank()) {
                val plateText = listOfNotNull(
                    record.registrationState?.uppercase()?.takeIf { it.isNotBlank() },
                    record.licensePlate?.uppercase()?.takeIf { it.isNotBlank() },
                ).joinToString(" ")
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { copyToClipboard(context, "License Plate", plateText) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (!record.registrationState.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                Text(
                                    text = record.registrationState.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = record.licensePlate?.uppercase() ?: "NO PLATE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy License Plate",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            record.registrationExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Registration Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            record.registrationFee?.let { fee ->
                InfoRow(
                    icon = Icons.Default.CreditCard,
                    label = "Renewal Fee",
                    value = "%s%.2f".format(currencySymbol, fee),
                    isCopyable = false,
                )
            }

            if (!record.registrationNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes",
                    value = record.registrationNotes,
                )
            }

            if (regImageFile?.exists() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Registration Document / Card",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onImageClicked(regImageFile) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = regImageFile,
                        contentDescription = "Registration document photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionCard(
    record: VehicleRegistrationInsurance,
) {
    val hasInspectionData = (record.inspectionExpiration != null) ||
        (record.inspectionDate != null) ||
        !record.inspectionResult.isNullOrBlank() ||
        !record.inspectionNotes.isNullOrBlank()

    if (!hasInspectionData) return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "State Vehicle Inspection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.inspectionExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            record.inspectionExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Inspection Due / Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            record.inspectionDate?.let { date ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Last Inspected Date",
                    value = date.toDisplayDate(),
                    isCopyable = false,
                )
            }

            if (!record.inspectionResult.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Inspection Result",
                    value = record.inspectionResult,
                )
            }

            if (!record.inspectionNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes / Station Info",
                    value = record.inspectionNotes,
                )
            }
        }
    }
}

@Composable
private fun EmissionsCard(
    record: VehicleRegistrationInsurance,
) {
    val hasEmissionsData = (record.emissionsExpiration != null) ||
        (record.emissionsDate != null) ||
        !record.emissionsResult.isNullOrBlank() ||
        !record.emissionsNotes.isNullOrBlank()

    if (!hasEmissionsData) return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Emissions & Smog Testing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.emissionsExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            record.emissionsExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Emissions Due / Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            record.emissionsDate?.let { date ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Last Smog Test Date",
                    value = date.toDisplayDate(),
                    isCopyable = false,
                )
            }

            if (!record.emissionsResult.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Test Result",
                    value = record.emissionsResult,
                )
            }

            if (!record.emissionsNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes / Station Info",
                    value = record.emissionsNotes,
                )
            }
        }
    }
}

@Composable
private fun InspectionStickerCard(
    record: VehicleRegistrationInsurance,
) {
    val hasStickerData = (record.inspectionStickerExpiration != null) ||
        !record.inspectionStickerNumber.isNullOrBlank() ||
        !record.inspectionStickerNotes.isNullOrBlank()

    if (!hasStickerData) return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "State & Local Inspection Stickers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.inspectionStickerExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            record.inspectionStickerExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Sticker Renewal / Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            if (!record.inspectionStickerNumber.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.ConfirmationNumber,
                    label = "Sticker / Decal #",
                    value = record.inspectionStickerNumber,
                )
            }

            if (!record.inspectionStickerNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes / Sticker Location",
                    value = record.inspectionStickerNotes,
                )
            }
        }
    }
}

@Composable
private fun InsuranceCard(
    record: VehicleRegistrationInsurance,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onImageClicked: (File) -> Unit,
) {
    if (record.insuranceProvider.isNullOrBlank() &&
        record.policyNumber.isNullOrBlank() &&
        record.insuranceExpiration == null &&
        record.insurancePremium == null &&
        record.insuranceAgentContact.isNullOrBlank() &&
        record.insuranceNotes.isNullOrBlank() &&
        record.insuranceImageUri.isNullOrBlank()
    ) return

    val insImageFile = remember(record.insuranceImageUri) {
        record.insuranceImageUri?.let(imageFileProvider)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Insurance Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.insuranceExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            if (!record.insuranceProvider.isNullOrBlank()) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { copyToClipboard(context, "Insurance Provider", record.insuranceProvider) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = record.insuranceProvider,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Insurance Provider",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!record.policyNumber.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Policy Number",
                    value = record.policyNumber,
                )
            }

            record.insuranceExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Policy Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            record.insurancePremium?.let { premium ->
                InfoRow(
                    icon = Icons.Default.CreditCard,
                    label = "Premium / Cost",
                    value = "%s%.2f".format(currencySymbol, premium),
                    isCopyable = false,
                )
            }

            if (!record.insuranceAgentContact.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.ContactPage,
                    label = "Agent / Contact",
                    value = record.insuranceAgentContact,
                )
            }

            if (!record.insuranceNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes",
                    value = record.insuranceNotes,
                )
            }

            if (insImageFile?.exists() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Insurance Card / Document",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onImageClicked(insImageFile) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = insImageFile,
                        contentDescription = "Insurance card photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TollParkingCard(
    record: VehicleRegistrationInsurance,
) {
    val hasTollData = (record.tollPassParkingExpiration != null) ||
        !record.tollPassParkingAccount.isNullOrBlank() ||
        !record.tollPassParkingNotes.isNullOrBlank()

    if (!hasTollData) return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalParking,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Toll Pass & Parking Permits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ExpirationStatusChip(expirationDate = record.tollPassParkingExpiration)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            record.tollPassParkingExpiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Permit / Pass Expiration",
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }

            if (!record.tollPassParkingAccount.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.CreditCard,
                    label = "Account / Tag / Permit #",
                    value = record.tollPassParkingAccount,
                )
            }

            if (!record.tollPassParkingNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes / Provider Details",
                    value = record.tollPassParkingNotes,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isCopyable: Boolean = true,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCopyable) Modifier.clickable { copyToClipboard(context, label, value) }
                else Modifier
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (isCopyable) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ExpirationStatusChip(expirationDate: Long?) {
    if (expirationDate == null) return

    val now = System.currentTimeMillis()
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expirationDate - now)

    val (label, color) = when {
        daysLeft < 0 -> "Expired (%d d ago)".format(-daysLeft) to StatusOverdue
        daysLeft <= 30 -> "Expires in %d d".format(daysLeft) to StatusUpcoming
        else -> "Active (%d d left)".format(daysLeft) to StatusOk
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledLeadingIconContentColor = color,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRegistrationInsuranceSheet(
    uiState: RegistrationInsuranceUiState,
    imageFileProvider: (String) -> File,
    onDismiss: () -> Unit,
    onLicensePlateChanged: (String) -> Unit,
    onRegistrationStateChanged: (String) -> Unit,
    onRegistrationExpirationChanged: (Long?) -> Unit,
    onRegistrationFeeChanged: (String) -> Unit,
    onRegistrationNotesChanged: (String) -> Unit,
    onRegistrationImagePicked: (Uri) -> Unit,
    onRemoveRegistrationImage: () -> Unit,
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
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedSheetSegment by remember { mutableIntStateOf(0) }

    val regPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onRegistrationImagePicked) }

    val insPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onInsuranceImagePicked) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScrollState)
                .verticalScrollbar(sheetScrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Vehicle Documents & Expirations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedSheetSegment,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val tabTitles = listOf(
                    "Tag & Reg",
                    "Inspection",
                    "Smog / Emissions",
                    "Stickers",
                    "Insurance",
                    "Toll / Parking",
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
                            label = { Text("License Plate / Tag") },
                            placeholder = { Text("e.g., 7ABC123") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1.8f),
                        )

                        OutlinedTextField(
                            value = uiState.registrationState,
                            onValueChange = onRegistrationStateChanged,
                            label = { Text("State / Prov") },
                            placeholder = { Text("e.g., CA") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = uiState.registrationExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Registration Expiration") },
                        placeholder = { Text("Select date") },
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
                        label = { Text("Renewal Fee (Optional)") },
                        prefix = { Text(uiState.currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.registrationNotes,
                        onValueChange = onRegistrationNotesChanged,
                        label = { Text("Registration Notes") },
                        placeholder = { Text("VIN on title, registration office notes...") },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ImagePickerBox(
                        title = "Registration Card Photo",
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

                    if (uiState.hasRegistrationData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteRegConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Tag & Registration Info")
                        }
                    }
                }
                1 -> {
                    // Section 2: State Vehicle Inspection
                    OutlinedTextField(
                        value = uiState.inspectionExpiration?.toDisplayDate().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inspection Expiration / Due Date") },
                        placeholder = { Text("Select expiration date") },
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
                        label = { Text("Last Inspected Date") },
                        placeholder = { Text("Select date inspected") },
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
                        label = { Text("Inspection Result") },
                        placeholder = { Text("e.g., Passed, Failed, Pending") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.inspectionNotes,
                        onValueChange = onInspectionNotesChanged,
                        label = { Text("Inspection Notes / Station Info") },
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
                            Text("Delete State Inspection Info")
                        }
                    }
                }
                2 -> {
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
                3 -> {
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
                4 -> {
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

@Composable
internal fun ImagePickerBox(
    title: String,
    pickedUri: Uri?,
    existingFilename: String?,
    imageFileProvider: (String) -> File,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    val existingFile = remember(existingFilename) { existingFilename?.let(imageFileProvider) }
    val hasPhoto = (pickedUri != null) || (existingFile?.exists() == true)

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (hasPhoto) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onPickImage),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = pickedUri ?: existingFile,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onPickImage) {
                        Text("Change Photo")
                    }

                    TextButton(
                        onClick = onRemoveImage,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Remove Photo")
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickImage),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add $title",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ImagePreviewDialog(
    imageFile: File,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close preview",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            AsyncImage(
                model = imageFile,
                contentDescription = "Document Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true)
@Composable
private fun RegistrationInsuranceScreenPreview() {
    GarageTheme {
        val sampleRecord = VehicleRegistrationInsurance(
            vehicleId = 1,
            licensePlate = "7ABC123",
            registrationState = "CA",
            registrationExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(45),
            registrationFee = 245.00,
            registrationNotes = "Registered with DMV Sacramento.",
            inspectionExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(90),
            inspectionDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(275),
            inspectionResult = "Passed",
            inspectionNotes = "Station #102, Certificate #A98124",
            emissionsExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
            emissionsDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(330),
            emissionsResult = "Passed",
            emissionsNotes = "Smog Check Star Station",
            inspectionStickerExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60),
            inspectionStickerNumber = "STK-2025-99",
            inspectionStickerNotes = "Windshield lower left",
            insuranceProvider = "Geico Insurance",
            policyNumber = "POL-987654321",
            insuranceExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(180),
            insurancePremium = 650.00,
            insuranceAgentContact = "John Smith 1-800-841-3000",
            insuranceNotes = "Comprehensive & collision with $500 deductible.",
            tollPassParkingExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(120),
            tollPassParkingAccount = "EZPass #01928374",
            tollPassParkingNotes = "Transponder in glove box",
        )

        RegistrationInsuranceContent(
            uiState = RegistrationInsuranceUiState(
                record = sampleRecord,
                currencySymbol = "$",
            ),
            imageFileProvider = { File("") },
            onAddOrEditClicked = {},
            onDismissSheet = {},
            onLicensePlateChanged = {},
            onRegistrationStateChanged = {},
            onRegistrationExpirationChanged = {},
            onRegistrationFeeChanged = {},
            onRegistrationNotesChanged = {},
            onRegistrationImagePicked = {},
            onRemoveRegistrationImage = {},
            onInspectionExpirationChanged = {},
            onInspectionDateChanged = {},
            onInspectionResultChanged = {},
            onInspectionNotesChanged = {},
            onEmissionsExpirationChanged = {},
            onEmissionsDateChanged = {},
            onEmissionsResultChanged = {},
            onEmissionsNotesChanged = {},
            onInspectionStickerExpirationChanged = {},
            onInspectionStickerNumberChanged = {},
            onInspectionStickerNotesChanged = {},
            onInsuranceProviderChanged = {},
            onPolicyNumberChanged = {},
            onInsuranceExpirationChanged = {},
            onInsurancePremiumChanged = {},
            onInsuranceAgentContactChanged = {},
            onInsuranceNotesChanged = {},
            onInsuranceImagePicked = {},
            onRemoveInsuranceImage = {},
            onTollPassParkingExpirationChanged = {},
            onTollPassParkingAccountChanged = {},
            onTollPassParkingNotesChanged = {},
            onDeleteRegistrationSection = {},
            onDeleteInspectionSection = {},
            onDeleteEmissionsSection = {},
            onDeleteInspectionStickerSection = {},
            onDeleteInsuranceSection = {},
            onDeleteTollPassParkingSection = {},
            onSave = {},
            onDelete = {},
        )
    }
}
