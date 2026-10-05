package com.fearmikey.garage.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.VehicleCard
import com.fearmikey.garage.ui.settings.DeveloperFavoritesContent
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.theme.StatusOk
import com.fearmikey.garage.ui.theme.StatusOverdue
import com.fearmikey.garage.ui.theme.StatusUpcoming
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import com.fearmikey.garage.ui.vehicle.ImagePickerBox
import com.fearmikey.garage.ui.vehicle.ImagePreviewDialog
import com.fearmikey.garage.ui.vehicle.VehicleTab
import java.util.concurrent.TimeUnit
import java.io.File
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    initialTab: Int = 0,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long, tab: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateMileage: (vehicleId: Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val fleetSummary by viewModel.fleetSummary.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val affiliateLinksEnabled by viewModel.affiliateLinksEnabled.collectAsStateWithLifecycle()
    val showFuelTrendGraph by viewModel.showFuelTrendGraph.collectAsStateWithLifecycle()
    val showFleetOverview by viewModel.showFleetOverview.collectAsStateWithLifecycle()
    val driversLicenseState by viewModel.driversLicenseState.collectAsStateWithLifecycle()

    DashboardContent(
        initialTab = initialTab,
        vehicles = vehicles,
        fleetSummary = fleetSummary,
        unitSystem = unitSystem,
        affiliateLinksEnabled = affiliateLinksEnabled,
        showFuelTrendGraph = showFuelTrendGraph,
        showFleetOverview = showFleetOverview,
        driversLicenseState = driversLicenseState,
        imageFileProvider = viewModel::imageFileFor,
        onAddVehicle = onAddVehicle,
        onOpenVehicle = onOpenVehicle,
        onOpenSettings = onOpenSettings,
        onUpdateMileage = onUpdateMileage,
        onSaveDriversLicense = viewModel::onSaveDriversLicense,
        onDeleteDriversLicense = viewModel::onDeleteDriversLicense,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DashboardContent(
    initialTab: Int,
    vehicles: List<VehicleListItem>,
    fleetSummary: FleetSummary,
    unitSystem: UnitSystem,
    affiliateLinksEnabled: Boolean,
    showFuelTrendGraph: Boolean,
    showFleetOverview: Boolean,
    driversLicenseState: DriversLicenseState,
    imageFileProvider: (String) -> File,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long, tab: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateMileage: (vehicleId: Long) -> Unit,
    onSaveDriversLicense: (
        number: String,
        state: String,
        expiration: Long?,
        notes: String,
        pickedFrontUri: Uri?,
        pickedBackUri: Uri?,
        existingFrontFilename: String?,
        existingBackFilename: String?,
    ) -> Unit,
    onDeleteDriversLicense: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Garage") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAddVehicle) {
                    Icon(Icons.Filled.Add, contentDescription = "Add vehicle")
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Vehicles") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Driver's License") },
                )
                if (affiliateLinksEnabled) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Support Store") },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (vehicles.isEmpty()) {
                        EmptyState(
                            message = "No vehicles yet.\nTap + to add your first one.",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .verticalScrollbar(listState),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (showFleetOverview) {
                                item(key = "fleet_summary") {
                                    FleetSummaryCard(
                                        summary = fleetSummary,
                                        unitSystem = unitSystem,
                                        onOpenOverdueReminders = {
                                            vehicles.firstOrNull { (it.overdueReminderCount > 0) || (it.upcomingReminderCount > 0) }
                                                ?.let { onOpenVehicle(it.vehicle.id, VehicleTab.SCHEDULE.ordinal) }
                                        },
                                    )
                                }
                            }

                            items(vehicles, key = { it.vehicle.id }) { item ->
                                VehicleCard(
                                    vehicle = item.vehicle,
                                    latestMileage = item.latestMileage,
                                    imageFile = item.imageFile,
                                    unitSystem = unitSystem,
                                    avgMpg = item.avgMpg,
                                    fuelEntries = if (showFuelTrendGraph) item.fuelEntries else emptyList(),
                                    overdueReminderCount = item.overdueReminderCount,
                                    upcomingReminderCount = item.upcomingReminderCount,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { onOpenVehicle(item.vehicle.id, 0) },
                                    onOpenReminders = { onOpenVehicle(item.vehicle.id, VehicleTab.SCHEDULE.ordinal) },
                                    onFuelGraphClick = { onOpenVehicle(item.vehicle.id, VehicleTab.FUEL.ordinal) },
                                    onUpdateMileage = { onUpdateMileage(item.vehicle.id) },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    DriversLicenseTab(
                        state = driversLicenseState,
                        imageFileProvider = imageFileProvider,
                        onSaveDriversLicense = onSaveDriversLicense,
                        onDeleteDriversLicense = onDeleteDriversLicense,
                    )
                }
                2 -> {
                    if (affiliateLinksEnabled) {
                        DeveloperFavoritesContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun DriversLicenseTab(
    state: DriversLicenseState,
    imageFileProvider: (String) -> File,
    onSaveDriversLicense: (
        number: String,
        state: String,
        expiration: Long?,
        notes: String,
        pickedFrontUri: Uri?,
        pickedBackUri: Uri?,
        existingFrontFilename: String?,
        existingBackFilename: String?,
    ) -> Unit,
    onDeleteDriversLicense: () -> Unit,
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var previewImageFile by remember { mutableStateOf<File?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (state.isEmpty) {
            EmptyState(
                message = "No Driver's License details added yet.\nTrack your driver's license expiration and get timely renewal reminders.",
                icon = Icons.Default.Badge,
            )
            Button(
                onClick = { isSheetOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Driver's License")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Driver's License",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            if (state.expiration != null) {
                                ExpirationStatusChip(expirationDate = state.expiration)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.expiration != null) {
                            InfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Expiration Date",
                                value = state.expiration.toDisplayDate(),
                                isCopyable = false,
                            )
                        }

                        if (state.number.isNotBlank()) {
                            InfoRow(
                                icon = Icons.Default.ContactPage,
                                label = "Driver's License Number",
                                value = state.number,
                            )
                        }

                        if (state.state.isNotBlank()) {
                            InfoRow(
                                icon = Icons.Default.Badge,
                                label = "Issuing State / Jurisdiction",
                                value = state.state,
                                isCopyable = false,
                            )
                        }

                        if (state.notes.isNotBlank()) {
                            InfoRow(
                                icon = Icons.AutoMirrored.Filled.Notes,
                                label = "Notes / Endorsements",
                                value = state.notes,
                            )
                        }

                        if (state.imageFilenameFront != null || state.imageFilenameBack != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "License Photos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val frontFile = state.imageFilenameFront?.let { imageFileProvider(it) }
                                if (frontFile?.exists() == true) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { previewImageFile = frontFile },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AsyncImage(
                                            model = frontFile,
                                            contentDescription = "Front of License",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }

                                val backFile = state.imageFilenameBack?.let { imageFileProvider(it) }
                                if (backFile?.exists() == true) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { previewImageFile = backFile },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AsyncImage(
                                            model = backFile,
                                            contentDescription = "Back of License",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { isSheetOpen = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit License")
                    }

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
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

    if (isSheetOpen) {
        EditDriversLicenseSheet(
            currentState = state,
            imageFileProvider = imageFileProvider,
            onDismiss = { isSheetOpen = false },
            onSave = { num, st, exp, notes, frontUri, backUri, currentFront, currentBack ->
                onSaveDriversLicense(num, st, exp, notes, frontUri, backUri, currentFront, currentBack)
                isSheetOpen = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Driver's License Info?") },
            text = { Text("Are you sure you want to delete your driver's license record? This includes any saved photos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteDriversLicense()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDriversLicenseSheet(
    currentState: DriversLicenseState,
    imageFileProvider: (String) -> File,
    onDismiss: () -> Unit,
    onSave: (
        number: String,
        state: String,
        expiration: Long?,
        notes: String,
        pickedFrontUri: Uri?,
        pickedBackUri: Uri?,
        existingFrontFilename: String?,
        existingBackFilename: String?,
    ) -> Unit,
) {
    var number by remember { mutableStateOf(currentState.number) }
    var state by remember { mutableStateOf(currentState.state) }
    var expiration by remember { mutableStateOf(currentState.expiration) }
    var notes by remember { mutableStateOf(currentState.notes) }

    var pickedFrontUri by remember { mutableStateOf<Uri?>(null) }
    var pickedBackUri by remember { mutableStateOf<Uri?>(null) }
    var existingFrontFilename by remember { mutableStateOf(currentState.imageFilenameFront) }
    var existingBackFilename by remember { mutableStateOf(currentState.imageFilenameBack) }

    val frontPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { pickedFrontUri = it } }

    val backPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { pickedBackUri = it } }

    var showDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Driver's License Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = expiration?.toDisplayDate().orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiration Date") },
                placeholder = { Text("Select expiration date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Expiration Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("License Number") },
                    placeholder = { Text("e.g., D1234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1.8f),
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("Issuing State") },
                    placeholder = { Text("e.g., CA") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Endorsements") },
                placeholder = { Text("Class C, motorcycle endorsement, REAL ID notes...") },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            ImagePickerBox(
                title = "Front of License",
                pickedUri = pickedFrontUri,
                existingFilename = existingFrontFilename,
                imageFileProvider = imageFileProvider,
                onPickImage = {
                    frontPhotoLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onRemoveImage = {
                    pickedFrontUri = null
                    existingFrontFilename = null
                },
            )

            com.fearmikey.garage.ui.vehicle.ImagePickerBox(
                title = "Back of License",
                pickedUri = pickedBackUri,
                existingFilename = existingBackFilename,
                imageFileProvider = imageFileProvider,
                onPickImage = {
                    backPhotoLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onRemoveImage = {
                    pickedBackUri = null
                    existingBackFilename = null
                },
            )

            Button(
                onClick = {
                    onSave(
                        number,
                        state,
                        expiration,
                        notes,
                        pickedFrontUri,
                        pickedBackUri,
                        existingFrontFilename,
                        existingBackFilename,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Driver's License")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiration?.toUtcDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            expiration = utcMillis.fromUtcDatePickerMillis()
                        }
                        showDatePicker = false
                    },
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    GarageTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = Unit, label = "DashboardScreenPreview") { target ->
                if (target == Unit) {
                    DashboardContent(
                        initialTab = 0,
                        vehicles = listOf(
                            VehicleListItem(
                                vehicle = SampleData.tacoma,
                                latestMileage = SampleData.TACOMA_LATEST_MILEAGE,
                                imageFile = null,
                                avgMpg = SampleData.tacomaAverageMpg,
                                overdueReminderCount = 1,
                                upcomingReminderCount = 1,
                            ),
                            VehicleListItem(
                                vehicle = SampleData.civic,
                                latestMileage = 42000,
                                imageFile = null,
                                avgMpg = 34.2,
                                overdueReminderCount = 0,
                                upcomingReminderCount = 0,
                            ),
                        ),
                        fleetSummary = FleetSummary(
                            totalVehicles = 2,
                            fleetAvgMpg = 26.8,
                            totalOverdueReminders = 1,
                            totalUpcomingReminders = 1,
                        ),
                        unitSystem = UnitSystem.IMPERIAL,
                        affiliateLinksEnabled = true,
                        showFuelTrendGraph = true,
                        showFleetOverview = true,
                        driversLicenseState = DriversLicenseState(
                            number = "D1234567",
                            state = "CA",
                            expiration = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(180),
                            notes = "Class C / REAL ID",
                        ),
                        imageFileProvider = { File("") },
                        onAddVehicle = {},
                        onOpenVehicle = { _, _ -> },
                        onOpenSettings = {},
                        onUpdateMileage = {},
                        onSaveDriversLicense = { _, _, _, _, _, _, _, _ -> },
                        onDeleteDriversLicense = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    GarageTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = Unit, label = "DashboardScreenEmptyPreview") { target ->
                if (target == Unit) {
                    DashboardContent(
                        initialTab = 0,
                        vehicles = emptyList(),
                        fleetSummary = FleetSummary(),
                        unitSystem = UnitSystem.IMPERIAL,
                        affiliateLinksEnabled = true,
                        showFuelTrendGraph = true,
                        showFleetOverview = true,
                        driversLicenseState = DriversLicenseState(),
                        imageFileProvider = { File("") },
                        onAddVehicle = {},
                        onOpenVehicle = { _, _ -> },
                        onOpenSettings = {},
                        onUpdateMileage = {},
                        onSaveDriversLicense = { _, _, _, _, _, _, _, _ -> },
                        onDeleteDriversLicense = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                }
            }
        }
    }
}
