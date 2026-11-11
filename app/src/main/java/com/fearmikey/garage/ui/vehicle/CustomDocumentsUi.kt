package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.local.entity.CustomDocument
import com.fearmikey.garage.ui.maintenance.openReceiptFile
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.parseToDoubleOrNull
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.io.File
import java.util.concurrent.TimeUnit

/** Read-only card for a user-named document on the Documents tab. */
@Composable
internal fun CustomDocumentCard(
    document: CustomDocument,
    currencySymbol: String,
    attachmentFileProvider: (String) -> File,
    onImageClicked: (File) -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val attachment = remember(document.imageUri) {
        document.imageUri?.let(attachmentFileProvider)?.takeIf { it.exists() }
    }
    val isPdf = attachment?.extension.equals("pdf", ignoreCase = true)

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit_item, document.title))
                }
            }

            ExpirationStatusChip(expirationDate = document.expiration)

            val hasDetails = (document.expiration != null) || !document.referenceNumber.isNullOrBlank() ||
                (document.cost != null) || !document.notes.isNullOrBlank() || (attachment != null)
            if (hasDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }

            document.expiration?.let { exp ->
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.custom_doc_expiration),
                    value = exp.toDisplayDate(),
                    isCopyable = false,
                )
            }
            if (!document.referenceNumber.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.ConfirmationNumber,
                    label = stringResource(R.string.custom_doc_reference_short),
                    value = document.referenceNumber,
                )
            }
            document.cost?.let { cost ->
                InfoRow(
                    icon = Icons.Default.Payments,
                    label = stringResource(R.string.common_cost),
                    value = "%s%.2f".format(currencySymbol, cost),
                    isCopyable = false,
                )
            }
            if (!document.notes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = stringResource(R.string.common_notes),
                    value = document.notes,
                )
            }
            if (attachment != null) {
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { if (isPdf) openReceiptFile(context, attachment) else onImageClicked(attachment) },
                    label = { Text(stringResource(if (isPdf) R.string.custom_doc_view_pdf else R.string.custom_doc_view_photo)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Bottom sheet for adding/editing a [CustomDocument]. [onSave] receives the edited document,
 * a newly picked attachment (image or PDF) if any, and whether the existing one was removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditCustomDocumentSheet(
    initial: CustomDocument?,
    currencySymbol: String,
    attachmentFileProvider: (String) -> File,
    onDismiss: () -> Unit,
    onSave: (document: CustomDocument, pickedAttachment: Uri?, removeAttachment: Boolean) -> Unit,
    onDelete: (CustomDocument) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var referenceNumber by remember { mutableStateOf(initial?.referenceNumber.orEmpty()) }
    var expiration by remember { mutableStateOf(initial?.expiration) }
    var cost by remember { mutableStateOf(initial?.cost?.let { "%.2f".format(it) }.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var pickedAttachment by remember { mutableStateOf<Uri?>(null) }
    var removeAttachment by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val existingAttachment = remember(initial?.imageUri) {
        initial?.imageUri?.let(attachmentFileProvider)?.takeIf { it.exists() }
    }
    val hasAttachment = (pickedAttachment != null) || ((existingAttachment != null) && !removeAttachment)

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pickedAttachment = uri
            removeAttachment = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (initial == null) R.string.custom_doc_add_title else R.string.custom_doc_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.custom_doc_name_label)) },
                placeholder = { Text(stringResource(R.string.custom_doc_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = { Text(stringResource(R.string.custom_doc_reference_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = expiration?.toDisplayDate().orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.custom_doc_expiration_optional)) },
                    trailingIcon = {
                        Row {
                            if (expiration != null) {
                                IconButton(onClick = { expiration = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.custom_doc_clear_expiration))
                                }
                            }
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.doc_select_expiration_date))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (expiration == null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true },
                    )
                }
            }

            OutlinedTextField(
                value = cost,
                onValueChange = { value -> cost = value.filter { it.isDigit() || it == '.' || it == ',' } },
                label = { Text(stringResource(R.string.common_cost_optional)) },
                prefix = { Text(currencySymbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.common_notes)) },
                placeholder = { Text(stringResource(R.string.custom_doc_notes_placeholder)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (hasAttachment) R.string.custom_doc_replace_attachment else R.string.custom_doc_attach))
                }
                if (hasAttachment) {
                    TextButton(
                        onClick = {
                            pickedAttachment = null
                            removeAttachment = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.common_remove)) }
                }
            }

            Button(
                onClick = {
                    val document = (initial ?: CustomDocument(vehicleId = 0L, title = "")).copy(
                        title = title.trim(),
                        referenceNumber = referenceNumber.trim().ifBlank { null },
                        expiration = expiration,
                        cost = cost.parseToDoubleOrNull(),
                        notes = notes.trim().ifBlank { null },
                    )
                    onSave(document, pickedAttachment, removeAttachment)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.custom_doc_save)) }

            if (initial != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.custom_doc_delete))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expiration?.toUtcDatePickerMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { expiration = it.fromUtcDatePickerMillis() }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteConfirm && (initial != null)) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.custom_doc_delete_title, initial.title)) },
            text = { Text(stringResource(R.string.custom_doc_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initial)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomDocumentCardPreview() {
    GarageTheme {
        CustomDocumentCard(
            document = CustomDocument(
                id = 1,
                vehicleId = 1,
                title = "Extended Warranty",
                referenceNumber = "EW-55821",
                expiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(20),
                cost = 1899.0,
                notes = "Powertrain + electrical, \$100 deductible. Call 1-800-555-0100.",
            ),
            currencySymbol = "$",
            attachmentFileProvider = { File("") },
            onImageClicked = {},
            onEdit = {},
        )
    }
}
