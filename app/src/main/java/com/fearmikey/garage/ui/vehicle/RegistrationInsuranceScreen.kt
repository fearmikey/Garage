package com.fearmikey.garage.ui.vehicle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.fearmikey.garage.obd.ObdScanSummary
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.foundation.lazy.items
import com.fearmikey.garage.data.local.entity.CustomDocument
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.ui.res.stringResource
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
import com.fearmikey.garage.R
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
import com.fearmikey.garage.ui.components.ScrollToTopOnInsert

@Composable
fun RegistrationInsuranceScreen(
    viewModel: RegistrationInsuranceViewModel = hiltViewModel(),
    onEditDocuments: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegistrationInsuranceContent(
        uiState = uiState,
        imageFileProvider = viewModel::imageFileFor,
        onEditDocuments = onEditDocuments,
        onSaveCustomDocument = viewModel::saveCustomDocument,
        onDeleteCustomDocument = viewModel::deleteCustomDocument,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationInsuranceContent(
    uiState: RegistrationInsuranceUiState,
    imageFileProvider: (String) -> File,
    onEditDocuments: () -> Unit,
    onSaveCustomDocument: (CustomDocument, Uri?, Boolean) -> Unit = { _, _, _ -> },
    onDeleteCustomDocument: (CustomDocument) -> Unit = {},
) {
    var previewImageFile by remember { mutableStateOf<File?>(null) }
    var showCustomDocumentSheet by remember { mutableStateOf(false) }
    var editingCustomDocument by remember { mutableStateOf<CustomDocument?>(null) }

    val record = uiState.record
    val hasRecordData = (record != null) && !record.isEmpty()
    val hasData = hasRecordData || uiState.customDocuments.isNotEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        editingCustomDocument = null
                        showCustomDocumentSheet = true
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add other document")
                }
                FloatingActionButton(
                    onClick = onEditDocuments,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = if (hasRecordData) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = if (hasRecordData) "Edit details" else "Add vehicle documents",
                    )
                }
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
                    message = "No vehicle documents added yet.\nTap + to track registration, inspection, emissions, stickers, insurance, toll passes, or purchase details.\nUse the small button to add any other document (warranty, title, roadside assistance...).",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                )
            } else {
                val listState = rememberLazyListState()
                ScrollToTopOnInsert(listState, uiState.customDocuments.firstOrNull()?.id)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (record != null) {
                        item {
                            RegistrationCard(
                                record = record,
                                currencySymbol = uiState.currencySymbol,
                                imageFileProvider = imageFileProvider,
                                onImageClicked = { previewImageFile = it },
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
                            InspectionCard(record = record)
                        }

                        item {
                            EmissionsCard(
                                record = record,
                                obdReadiness = uiState.obdReadiness,
                                obdReadinessDate = uiState.obdReadinessDate,
                            )
                        }

                        item {
                            InspectionStickerCard(record = record)
                        }

                        item {
                            TollParkingCard(record = record)
                        }

                        if (record.hasPurchaseData) {
                            item {
                                PurchaseCard(
                                    record = record,
                                    currencySymbol = uiState.currencySymbol,
                                    imageFileProvider = imageFileProvider,
                                    onImageClicked = { previewImageFile = it },
                                )
                            }
                        }
                    }

                    if (uiState.customDocuments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Other Documents",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(uiState.customDocuments, key = { "custom_${it.id}" }) { document ->
                            CustomDocumentCard(
                                document = document,
                                currencySymbol = uiState.currencySymbol,
                                attachmentFileProvider = imageFileProvider,
                                onImageClicked = { previewImageFile = it },
                                onEdit = {
                                    editingCustomDocument = document
                                    showCustomDocumentSheet = true
                                },
                            )
                        }
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

    if (showCustomDocumentSheet) {
        AddEditCustomDocumentSheet(
            initial = editingCustomDocument,
            currencySymbol = uiState.currencySymbol,
            attachmentFileProvider = imageFileProvider,
            onDismiss = {
                showCustomDocumentSheet = false
                editingCustomDocument = null
            },
            onSave = { document, picked, remove ->
                onSaveCustomDocument(document, picked, remove)
                showCustomDocumentSheet = false
                editingCustomDocument = null
            },
            onDelete = { document ->
                onDeleteCustomDocument(document)
                showCustomDocumentSheet = false
                editingCustomDocument = null
            },
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
        record.registrationImageUri.isNullOrBlank() &&
        record.registrationImageUri2.isNullOrBlank()
    ) return

    val regImageFiles = remember(record.registrationImageUri, record.registrationImageUri2) {
        listOfNotNull(record.registrationImageUri, record.registrationImageUri2)
            .filter { it.isNotBlank() }
            .map(imageFileProvider)
            .filter { it.exists() }
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

            if (regImageFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Registration Document / Card",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    regImageFiles.forEachIndexed { index, file ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onImageClicked(file) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Registration document photo ${index + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
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
    obdReadiness: ObdScanSummary.SavedReadiness? = null,
    obdReadinessDate: Long? = null,
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

            if (obdReadiness != null) {
                val verdict = when {
                    obdReadiness.milOn == true -> stringResource(R.string.documents_obd_readiness_mil_on)
                    obdReadiness.likelyReady -> stringResource(R.string.documents_obd_readiness_likely_ready)
                    else -> stringResource(R.string.documents_obd_readiness_not_ready)
                }
                val notReady = obdReadiness.notReady.takeIf { it.isNotEmpty() }
                    ?.let { "\n" + stringResource(R.string.documents_obd_readiness_not_ready_list, it.joinToString()) }
                    .orEmpty()
                val readinessLabel = obdReadinessDate?.let {
                    stringResource(R.string.documents_obd_readiness_label_scanned, it.toDisplayDate())
                } ?: stringResource(R.string.documents_obd_readiness_label)
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    label = readinessLabel,
                    value = stringResource(
                        R.string.documents_obd_readiness_value,
                        obdReadiness.complete,
                        obdReadiness.total,
                        verdict,
                    ) + notReady,
                    isCopyable = false,
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
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseCard(
    record: VehicleRegistrationInsurance,
    currencySymbol: String,
    imageFileProvider: (String) -> File,
    onImageClicked: (File) -> Unit,
) {
    val billOfSale = remember(record.purchaseImageUri) {
        record.purchaseImageUri?.let(imageFileProvider)?.takeIf { it.exists() }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Purchase",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            record.purchasePrice?.let { price ->
                InfoRow(
                    icon = Icons.Default.Payments,
                    label = "Purchase Price",
                    value = "%s%.2f".format(currencySymbol, price),
                    isCopyable = false,
                )
            }
            record.purchaseDate?.let { date ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Purchase Date",
                    value = date.toDisplayDate(),
                    isCopyable = false,
                )
            }
            if (!record.purchaseSeller.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.Store,
                    label = "Seller / Dealer",
                    value = record.purchaseSeller,
                )
            }
            if (!record.purchaseNotes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Notes",
                    value = record.purchaseNotes,
                )
            }
            if (billOfSale != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onImageClicked(billOfSale) },
                ) {
                    AsyncImage(
                        model = billOfSale,
                        contentDescription = "Bill of sale",
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
internal fun InfoRow(
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
internal fun ExpirationStatusChip(expirationDate: Long?) {
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
                        contentScale = ContentScale.Fit,
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
            onEditDocuments = {},
        )
    }
}
