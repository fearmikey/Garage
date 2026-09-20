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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.config.FlavorConfig
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
        onImagesPicked = viewModel::onImagesPicked,
        onReplaceImagePicked = viewModel::onReplaceImagePicked,
        onRemovePhoto = viewModel::onRemovePhoto,
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
    onImagesPicked: (List<Uri>) -> Unit = {},
    onReplaceImagePicked: (Int, Uri) -> Unit = { _, _ -> },
    onRemovePhoto: (Int) -> Unit = {},
    onImageOffsetYChanged: (Int, Float) -> Unit = { _, _ -> },
    onSave: () -> Unit,
    onDelete: () -> Unit = {},
) {
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
    ) { uris ->
        if (uris.isNotEmpty()) onImagesPicked(uris)
    }

    var replacingIndex by remember { mutableStateOf<Int?>(null) }
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val index = replacingIndex
        if ((uri != null) && (index != null)) {
            onReplaceImagePicked(index, uri)
        }
        replacingIndex = null
    }

    var showDeleteDialog by remember { mutableStateOf(value = false) }

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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.photos.isNotEmpty()) {
                Column {
                    val pagerState = rememberPagerState(pageCount = { uiState.photos.size })
                    val currentPage = pagerState.currentPage.coerceIn(0, (uiState.photos.size - 1).coerceAtLeast(0))
                    val currentPhoto = uiState.photos.getOrNull(currentPage)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(21f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(currentPage) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (currentPage in uiState.photos.indices) {
                                        val currentOffsetY = uiState.photos[currentPage].offsetY
                                        onImageOffsetYChanged(currentPage, currentOffsetY + (dragAmount * 0.008f))
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val photoItem = uiState.photos[page]
                            if (photoItem.file?.exists() == true) {
                                AsyncImage(
                                    model = photoItem.file,
                                    contentDescription = "Vehicle photo ${page + 1}",
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, photoItem.offsetY),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        if (uiState.photos.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(uiState.photos.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(color, shape = CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (uiState.photos.size > 1) {
                                    "Photo ${currentPage + 1} of ${uiState.photos.size} - Drag up/down to adjust"
                                } else {
                                    "Drag photo up/down to adjust position"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.photos.size < 3) {
                                    IconButton(
                                        onClick = {
                                            multiplePhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                    ) {
                                        Icon(
                                            Icons.Filled.AddAPhoto,
                                            contentDescription = "Add photo",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        replacingIndex = currentPage
                                        singlePhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Change photo",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { onRemovePhoto(currentPage) },
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove photo",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        currentPhoto?.let { photo ->
                            Slider(
                                value = photo.offsetY,
                                onValueChange = { offsetY -> onImageOffsetYChanged(currentPage, offsetY) },
                                valueRange = -1f..1f,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text("Add photos (up to 3)", style = MaterialTheme.typography.bodyMedium)
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
                    if (FlavorConfig.isVinScannerSupported) {
                        IconButton(onClick = onScanVinClicked) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan VIN")
                        }
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
            onSave = {},
            onDelete = {},
        )
    }
}
