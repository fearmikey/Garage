package com.fearmikey.garage.ui.vehicle

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fearmikey.garage.ui.cost.CostOfOwnershipScreen
import com.fearmikey.garage.ui.fuel.FuelScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceSuggestionsScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceTimelineScreen
import com.fearmikey.garage.ui.mod.ModsScreen
import com.fearmikey.garage.ui.recall.RecallsScreen
import com.fearmikey.garage.ui.reminder.RemindersScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onEditParts: (Long) -> Unit = {},
    onExportMaintenance: (Long) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val imageFile by viewModel.imageFile.collectAsStateWithLifecycle()
    val shouldOpenAddSheet by viewModel.shouldOpenAddSheet.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(viewModel.initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(vehicle?.let { "${it.year ?: ""} ${it.make} ${it.model}".trim() } ?: "Vehicle")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    vehicle?.let {
                        IconButton(onClick = { onExportMaintenance(it.id) }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export Maintenance Log")
                        }
                        IconButton(onClick = { onEditVehicle(it.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit vehicle")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(21f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .sharedElement(
                            rememberSharedContentState(key = "vehicle-image-${viewModel.vehicleId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val currentImageFile = imageFile
                    if ((currentImageFile != null) && currentImageFile.exists()) {
                        AsyncImage(
                            model = currentImageFile,
                            contentDescription = vehicle?.let { "${it.year ?: ""} ${it.make} ${it.model}".trim() },
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(0f, vehicle?.imageOffsetY ?: 0f),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SecondaryScrollableTabRow(selectedTabIndex = selectedTab) {
                VehicleTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.title, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
            when (selectedTab) {
                VehicleTab.TIMELINE.ordinal -> MaintenanceTimelineScreen(
                    autoOpenAddSheet = shouldOpenAddSheet && (viewModel.initialTab == VehicleTab.TIMELINE.ordinal),
                    onAddSheetConsumed = viewModel::consumeAddSheet,
                )
                VehicleTab.FUEL.ordinal -> FuelScreen(
                    autoOpenAddSheet = shouldOpenAddSheet && (viewModel.initialTab == VehicleTab.FUEL.ordinal),
                    onAddSheetConsumed = viewModel::consumeAddSheet,
                )
                VehicleTab.REMINDERS.ordinal -> RemindersScreen()
                VehicleTab.SCHEDULE.ordinal -> MaintenanceSuggestionsScreen()
                VehicleTab.EXPENSES.ordinal -> CostOfOwnershipScreen()
                VehicleTab.SPECS.ordinal -> VehicleSpecsScreen()
                VehicleTab.PARTS.ordinal -> PartsScreen(onEditParts = { onEditParts(viewModel.vehicleId) })
                VehicleTab.MODS.ordinal -> ModsScreen()
                VehicleTab.DOCUMENTS.ordinal -> RegistrationInsuranceScreen()
                VehicleTab.RECALLS.ordinal -> RecallsScreen()
            }
        }
    }
}
