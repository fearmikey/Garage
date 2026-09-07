package com.fearmikey.garage.ui.vehicle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.ui.theme.GarageTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onScanVinClicked: () -> Unit = {},
    scannedVin: String? = null,
    onScannedVinConsumed: () -> Unit = {},
    viewModel: AddEditVehicleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveComplete, uiState.deleteComplete) {
        if (uiState.saveComplete || uiState.deleteComplete) onDone()
    }

    LaunchedEffect(scannedVin) {
        if (!scannedVin.isNullOrBlank()) {
            viewModel.onVinChanged(scannedVin)
            onScannedVinConsumed()
        }
    }

    AddEditVehicleContent(
        uiState = uiState,
        onBack = onBack,
        onVinChanged = viewModel::onVinChanged,
        onDecodeVinClicked = viewModel::onDecodeVinClicked,
        onScanVinClicked = onScanVinClicked,
        onYearChanged = viewModel::onYearChanged,
        onMakeChanged = viewModel::onMakeChanged,
        onModelChanged = viewModel::onModelChanged,
        onTrimChanged = viewModel::onTrimChanged,
        onDrivetrainChanged = viewModel::onDrivetrainChanged,
        onImagePicked = viewModel::onImagePicked,
        onImageOffsetYChanged = viewModel::onImageOffsetYChanged,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDeleteVehicle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditVehicleContent(
    uiState: AddEditVehicleUiState,
    onBack: () -> Unit,
    onVinChanged: (String) -> Unit,
    onDecodeVinClicked: () -> Unit,
    onScanVinClicked: () -> Unit = {},
    onYearChanged: (String) -> Unit,
    onMakeChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onTrimChanged: (String) -> Unit,
    onDrivetrainChanged: (Drivetrain) -> Unit = {},
    onImagePicked: (Uri) -> Unit,
    onImageOffsetYChanged: (Float) -> Unit = {},
    onSave: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onImagePicked) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Vehicle" else "Add Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete vehicle",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if ((uiState.imageFile != null) && uiState.imageFile.exists()) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(21f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    onImageOffsetYChanged(uiState.imageOffsetY + dragAmount * 0.008f)
                                }
                            }
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = uiState.imageFile,
                            contentDescription = "Vehicle photo",
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(0f, uiState.imageOffsetY),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Drag photo up/down to adjust position",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            ) {
                                Text("Change photo")
                            }
                        }
                        Slider(
                            value = uiState.imageOffsetY,
                            onValueChange = onImageOffsetYChanged,
                            valueRange = -1f..1f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(21f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text("Add photo", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedTextField(
                value = uiState.vin,
                onValueChange = onVinChanged,
                label = { Text("VIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                leadingIcon = {
                    IconButton(onClick = onScanVinClicked) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan VIN")
                    }
                },
                trailingIcon = {
                    if (uiState.isDecodingVin) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        TextButton(onClick = onDecodeVinClicked, enabled = uiState.vin.isNotBlank()) {
                            Text("Decode")
                        }
                    }
                },
                supportingText = uiState.vinDecodeError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.year,
                onValueChange = onYearChanged,
                label = { Text("Year") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.make,
                onValueChange = onMakeChanged,
                label = { Text("Make") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.model,
                onValueChange = onModelChanged,
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.trim,
                onValueChange = onTrimChanged,
                label = { Text("Trim") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            var drivetrainMenuExpanded by remember { mutableStateOf(value = false) }
            ExposedDropdownMenuBox(
                expanded = drivetrainMenuExpanded,
                onExpandedChange = { drivetrainMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.drivetrain.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Drivetrain") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = drivetrainMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = drivetrainMenuExpanded,
                    onDismissRequest = { drivetrainMenuExpanded = false },
                ) {
                    Drivetrain.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.displayName) },
                            onClick = {
                                onDrivetrainChanged(entry)
                                drivetrainMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && uiState.make.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save")
            }

            if (uiState.isEditing) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Delete vehicle")
                }
            }
        }
    }

    if (showDeleteDialog) {
        val vehicleTitle = listOfNotNull(uiState.year.ifBlank { null }, uiState.make.ifBlank { null }, uiState.model.ifBlank { null })
            .joinToString(" ").ifBlank { "this vehicle" }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vehicle?") },
            text = { Text("Are you sure you want to delete $vehicleTitle? This will remove this vehicle and all its maintenance records, fuel logs, and reminders from the app.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddEditVehicleScreenPreview() {
    GarageTheme {
        AddEditVehicleContent(
            uiState = AddEditVehicleUiState(
                vin = "3TYCZ5AN0PT000001",
                year = "2023",
                make = "Toyota",
                model = "Tacoma",
                trim = "TRD Off-Road",
                isEditing = true,
            ),
            onBack = {},
            onVinChanged = {},
            onDecodeVinClicked = {},
            onYearChanged = {},
            onMakeChanged = {},
            onModelChanged = {},
            onTrimChanged = {},
            onImagePicked = {},
            onSave = {},
            onDelete = {},
        )
    }
}
