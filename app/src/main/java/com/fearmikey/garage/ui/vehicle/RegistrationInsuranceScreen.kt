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
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
        onInsuranceProviderChanged = viewModel::onInsuranceProviderChanged,
        onPolicyNumberChanged = viewModel::onPolicyNumberChanged,
        onInsuranceExpirationChanged = viewModel::onInsuranceExpirationChanged,
        onInsurancePremiumChanged = viewModel::onInsurancePremiumChanged,
        onInsuranceAgentContactChanged = viewModel::onInsuranceAgentContactChanged,
        onInsuranceNotesChanged = viewModel::onInsuranceNotesChanged,
        onInsuranceImagePicked = viewModel::onInsuranceImagePicked,
        onRemoveInsuranceImage = viewModel::onRemoveInsuranceImage,
        onDeleteRegistrationSection = viewModel::onDeleteRegistrationSection,
        onDeleteInspectionSection = viewModel::onDeleteInspectionSection,
        onDeleteInsuranceSection = viewModel::onDeleteInsuranceSection,
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
    onInsuranceProviderChanged: (String) -> Unit,
    onPolicyNumberChanged: (String) -> Unit,
    onInsuranceExpirationChanged: (Long?) -> Unit,
    onInsurancePremiumChanged: (String) -> Unit,
    onInsuranceAgentContactChanged: (String) -> Unit,
    onInsuranceNotesChanged: (String) -> Unit,
    onInsuranceImagePicked: (Uri) -> Unit,
    onRemoveInsuranceImage: () -> Unit,
    onDeleteRegistrationSection: () -> Unit,
    onDeleteInspectionSection: () -> Unit,
    onDeleteInsuranceSection: () -> Unit,
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
                    contentDescription = if (hasData) "Edit details" else "Add registration & insurance",
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
                    message = "No registration, inspection, or insurance info added yet.\nTap + to track tag registration, state inspection, and policy details.",
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
                        InsuranceCard(
                            record = record,
                            currencySymbol = uiState.currencySymbol,
                            imageFileProvider = imageFileProvider,
                            onImageClicked = { previewImageFile = it },
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
            onInsuranceProviderChanged = onInsuranceProviderChanged,
            onPolicyNumberChanged = onPolicyNumberChanged,
            onInsuranceExpirationChanged = onInsuranceExpirationChanged,
            onInsurancePremiumChanged = onInsurancePremiumChanged,
            onInsuranceAgentContactChanged = onInsuranceAgentContactChanged,
            onInsuranceNotesChanged = onInsuranceNotesChanged,
            onInsuranceImagePicked = onInsuranceImagePicked,
            onRemoveInsuranceImage = onRemoveInsuranceImage,
            onDeleteRegistrationSection = onDeleteRegistrationSection,
            onDeleteInspectionSection = onDeleteInspectionSection,
            onDeleteInsuranceSection = onDeleteInsuranceSection,
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

            // License Plate Badge Display
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

            // Details list
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
                    label = "Notes / Sticker #",
                    value = record.inspectionNotes,
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    copyableText: String? = value,
    isCopyable: Boolean = true,
) {
    val context = LocalContext.current
    val textToCopy = copyableText ?: value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCopyable) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { copyToClipboard(context, label, textToCopy) }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (isCopyable) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
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
        daysLeft < 0 -> "Expired" to StatusOverdue
        daysLeft <= 30 -> "Expires Soon" to StatusUpcoming
        else -> "Active" to StatusOk
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
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
    onInsuranceProviderChanged: (String) -> Unit,
    onPolicyNumberChanged: (String) -> Unit,
    onInsuranceExpirationChanged: (Long?) -> Unit,
    onInsurancePremiumChanged: (String) -> Unit,
    onInsuranceAgentContactChanged: (String) -> Unit,
    onInsuranceNotesChanged: (String) -> Unit,
    onInsuranceImagePicked: (Uri) -> Unit,
    onRemoveInsuranceImage: () -> Unit,
    onDeleteRegistrationSection: () -> Unit,
    onDeleteInspectionSection: () -> Unit,
    onDeleteInsuranceSection: () -> Unit,
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

    var showRegDatePicker by remember { mutableStateOf(value = false) }
    var showInspExpDatePicker by remember { mutableStateOf(value = false) }
    var showInspDateDatePicker by remember { mutableStateOf(value = false) }
    var showInsDatePicker by remember { mutableStateOf(value = false) }

    var showDeleteRegConfirmDialog by remember { mutableStateOf(value = false) }
    var showDeleteInspConfirmDialog by remember { mutableStateOf(value = false) }
    var showDeleteInsConfirmDialog by remember { mutableStateOf(value = false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(value = false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
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
                text = "Registration & Insurance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SegmentedButton(
                    selected = selectedSheetSegment == 0,
                    onClick = { selectedSheetSegment = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) {
                    Text("Tag & Reg")
                }
                SegmentedButton(
                    selected = selectedSheetSegment == 1,
                    onClick = { selectedSheetSegment = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) {
                    Text("Inspection")
                }
                SegmentedButton(
                    selected = selectedSheetSegment == 2,
                    onClick = { selectedSheetSegment = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) {
                    Text("Insurance")
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

                    // Registration card photo picker
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
                        label = { Text("Inspection Notes / Sticker #") },
                        placeholder = { Text("Station name, sticker/certificate #...") },
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
                    // Section 3: Insurance Policy
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

                    // Insurance card photo picker
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Registration & Insurance")
            }

            if ((uiState.record != null) && !uiState.record.isEmpty()) {
                OutlinedButton(
                    onClick = { showDeleteAllConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Categories")
                }
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

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("Delete All Categories?") },
            text = { Text("Are you sure you want to delete all registration, inspection, and policy records for this vehicle? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ImagePickerBox(
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
private fun ImagePreviewDialog(
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
            inspectionNotes = "Smog Station #102, Certificate #A98124",
            insuranceProvider = "Geico Insurance",
            policyNumber = "POL-987654321",
            insuranceExpiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(180),
            insurancePremium = 650.00,
            insuranceAgentContact = "John Smith 1-800-841-3000",
            insuranceNotes = "Comprehensive & collision with $500 deductible.",
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
            onInsuranceProviderChanged = {},
            onPolicyNumberChanged = {},
            onInsuranceExpirationChanged = {},
            onInsurancePremiumChanged = {},
            onInsuranceAgentContactChanged = {},
            onInsuranceNotesChanged = {},
            onInsuranceImagePicked = {},
            onRemoveInsuranceImage = {},
            onDeleteRegistrationSection = {},
            onDeleteInspectionSection = {},
            onDeleteInsuranceSection = {},
            onSave = {},
            onDelete = {},
        )
    }
}
