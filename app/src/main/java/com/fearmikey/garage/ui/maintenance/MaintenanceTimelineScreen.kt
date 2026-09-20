package com.fearmikey.garage.ui.maintenance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.schedule.MaintenanceScheduleRules
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.formatMileageInput
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.io.File

@Composable
fun MaintenanceTimelineScreen(
    autoOpenAddSheet: Boolean = false,
    onAddSheetConsumed: () -> Unit = {},
    viewModel: MaintenanceTimelineViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val latestMileage by viewModel.latestMileage.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(value = false) }
    var editingRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }

    LaunchedEffect(autoOpenAddSheet) {
        if (autoOpenAddSheet) {
            showAddSheet = true
            onAddSheetConsumed()
        }
    }

    MaintenanceTimelineContent(
        records = records,
        unitSystem = unitSystem,
        currencySymbol = currencySymbol,
        receiptFileProvider = viewModel::receiptFileFor,
        onAddClicked = { editingRecord = null; showAddSheet = true },
        onEditRecord = { record -> editingRecord = record; showAddSheet = true },
        onDeleteRecord = viewModel::deleteRecord,
    )

    if (showAddSheet) {
        AddEditMaintenanceRecordSheet(
            unitSystem = unitSystem,
            currencySymbol = currencySymbol,
            latestMileage = latestMileage,
            initial = editingRecord,
            receiptFileProvider = viewModel::receiptFileFor,
            onDismiss = { showAddSheet = false; editingRecord = null },
            onSave = { record, pickedUri, deleteExisting ->
                viewModel.saveRecord(record, pickedUri, deleteExisting)
                showAddSheet = false
                editingRecord = null
            },
        )
    }
}

@Composable
private fun MaintenanceTimelineContent(
    records: List<MaintenanceRecord>,
    unitSystem: UnitSystem,
    currencySymbol: String = "$",
    receiptFileProvider: (String) -> File? = { null },
    onAddClicked: () -> Unit,
    onEditRecord: (MaintenanceRecord) -> Unit,
    onDeleteRecord: (MaintenanceRecord) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Add maintenance record")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (records.isEmpty()) {
                EmptyState(message = "No maintenance logged yet.\nTap + to add your first record.")
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.verticalScrollbar(listState),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        MaintenanceRecordRow(
                            record = record,
                            unitSystem = unitSystem,
                            currencySymbol = currencySymbol,
                            receiptFileProvider = receiptFileProvider,
                            onEdit = { onEditRecord(record) },
                            onDelete = { onDeleteRecord(record) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceRecordRow(
    record: MaintenanceRecord,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    receiptFileProvider: (String) -> File? = { null },
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(value = false) }
    var showImagePreviewDialog by remember { mutableStateOf(value = false) }
    val context = LocalContext.current

    val receiptFile = remember(record.receiptUri) {
        record.receiptUri?.let(receiptFileProvider)
    }
    val hasReceipt = receiptFile?.exists() == true
    val isPdf = receiptFile?.name?.endsWith(".pdf", ignoreCase = true) == true

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if ((showImagePreviewDialog && (receiptFile != null)) && !isPdf) {
        AlertDialog(
            onDismissRequest = { showImagePreviewDialog = false },
            title = { Text("Receipt / Invoice") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = receiptFile,
                        contentDescription = "Receipt",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImagePreviewDialog = false
                        openReceiptFile(context, receiptFile)
                    },
                ) {
                    Text("Open with app")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePreviewDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.category.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(record.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.date.toDisplayDate()} · ${UnitConverter.formatDistance(record.mileage, unitSystem)} · %s%s".format(currencySymbol, "%.2f".format(record.cost)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (hasReceipt) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(
                        onClick = {
                            if (isPdf) {
                                openReceiptFile(context, receiptFile)
                            } else {
                                showImagePreviewDialog = true
                            }
                        },
                        label = { Text(if (isPdf) "PDF Receipt" else "Receipt Image") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit record")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete record")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditMaintenanceRecordSheet(
    onDismiss: () -> Unit,
    onSave: (record: MaintenanceRecord, pickedUri: Uri?, deleteExisting: Boolean) -> Unit,
    initial: MaintenanceRecord? = null,
    latestMileage: Int? = null,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    currencySymbol: String = "$",
    receiptFileProvider: (String) -> File? = { null },
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var mileage by remember {
        mutableStateOf(
            if ((initial != null) && (initial.id != 0L)) {
                if (initial.mileage > 0) {
                    formatMileageInput(UnitConverter.displayDistanceValue(initial.mileage, unitSystem).toString())
                } else ""
            } else {
                val defaultMileage = initial?.mileage?.takeIf { it > 0 } ?: latestMileage?.takeIf { it > 0 }
                defaultMileage?.let { formatMileageInput(UnitConverter.displayDistanceValue(it, unitSystem).toString()) }.orEmpty()
            }
        )
    }
    var cost by remember { mutableStateOf(initial?.cost?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: MaintenanceCategory.OTHER) }
    var categoryMenuExpanded by remember { mutableStateOf(value = false) }
    var taskName by remember { mutableStateOf(initial?.taskName) }
    var taskMenuExpanded by remember { mutableStateOf(value = false) }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(value = false) }

    var pickedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var isReceiptRemoved by remember { mutableStateOf(value = false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pickedReceiptUri = uri
            isReceiptRemoved = false
        }
    }

    val currentReceiptFilename = initial?.receiptUri
    val receiptFile = remember(currentReceiptFilename) {
        currentReceiptFilename?.let(receiptFileProvider)
    }

    val hasReceipt = ((pickedReceiptUri != null) || (receiptFile?.exists() == true)) && !isReceiptRemoved
    val isPdf = when {
        pickedReceiptUri != null -> {
            val type = context.contentResolver.getType(pickedReceiptUri!!)
            (type == "application/pdf") || pickedReceiptUri.toString().endsWith(".pdf", ignoreCase = true)
        }
        receiptFile != null -> receiptFile.name.endsWith(".pdf", ignoreCase = true)
        else -> false
    }

    // The known task names for the selected category, e.g. "Brake fluid flush" vs.
    // "Coolant flush" both under Fluids. Picking one of these (rather than "Other / custom")
    // is what lets MaintenanceScheduleEngine track each task's due date independently instead
    // of conflating every task in the same category.
    val taskOptions = remember(category) {
        MaintenanceScheduleRules.rules
            .asSequence()
            .filter { it.category == category }
            .map { it.taskName }
            .distinctBy { it.lowercase() }
            .sorted()
            .toList()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcDatePickerMillis(),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            date = utcMillis.fromUtcDatePickerMillis()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .imePadding()
                .verticalScroll(sheetScrollState)
                .verticalScrollbar(sheetScrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Maintenance Record", style = MaterialTheme.typography.titleLarge)

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = date.toDisplayDate(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = false },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true },
                )
            }

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .focusProperties { canFocus = false },
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    MaintenanceCategory.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.displayName) },
                            onClick = {
                                category = entry
                                taskName = null
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }

            if (taskOptions.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = taskMenuExpanded,
                    onExpandedChange = { taskMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = taskName ?: "Other / custom",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Specific task") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .focusProperties { canFocus = false },
                    )
                    ExposedDropdownMenu(
                        expanded = taskMenuExpanded,
                        onDismissRequest = { taskMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Other / custom") },
                            onClick = {
                                taskName = null
                                taskMenuExpanded = false
                            },
                        )
                        taskOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    taskName = option
                                    if (description.isBlank()) description = option
                                    taskMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = formatMileageInput(it) },
                label = { Text("Mileage (${unitSystem.distanceUnit})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it.filter { c -> c.isDigit() || (c == '.') } },
                label = { Text("Cost ($currencySymbol)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Receipt / Invoice section
            Text("Receipt / Invoice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (hasReceipt) {
                Column {
                    if (isPdf) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Attached PDF Receipt",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "PDF Document",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = pickedReceiptUri ?: receiptFile,
                                contentDescription = "Receipt Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                            },
                        ) {
                            Text("Change Attachment")
                        }

                        TextButton(
                            onClick = {
                                pickedReceiptUri = null
                                isReceiptRemoved = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("Remove Attachment")
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attach Receipt / Invoice (Image or PDF)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val inputMileage = mileage.filter(Char::isDigit).toIntOrNull() ?: 0
                    val canonicalMileage = UnitConverter.canonicalMilesFromInput(inputMileage, unitSystem)
                    onSave(
                        MaintenanceRecord(
                            id = initial?.id ?: 0,
                            vehicleId = initial?.vehicleId ?: 0,
                            date = date,
                            mileage = canonicalMileage,
                            description = description,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            category = category,
                            taskName = taskName,
                            receiptUri = initial?.receiptUri,
                        ),
                        pickedReceiptUri,
                        isReceiptRemoved,
                    )
                },
                enabled = description.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

private fun openReceiptFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open Receipt"))
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MaintenanceTimelineScreenPreview() {
    GarageTheme {
        MaintenanceTimelineContent(
            records = SampleData.tacomaMaintenanceRecords,
            unitSystem = UnitSystem.IMPERIAL,
            onAddClicked = {},
            onEditRecord = {},
            onDeleteRecord = {},
        )
    }
}
