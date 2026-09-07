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
import com.fearmikey.garage.ui.fuel.FuelScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceSuggestionsScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceTimelineScreen
import com.fearmikey.garage.ui.recall.RecallsScreen
import com.fearmikey.garage.ui.reminder.RemindersScreen

private val TAB_TITLES = listOf("Timeline", "Suggested", "Reminders", "Fuel", "Recalls", "Specs", "Parts")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onEditParts: (Long) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val imageFile by viewModel.imageFile.collectAsStateWithLifecycle()
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
                    if (currentImageFile != null && currentImageFile.exists()) {
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
            // Scrollable (rather than fixed-width) since there are enough tabs now
            // that a fixed row would squeeze/wrap labels like "Suggested" or "Reminders".
            SecondaryScrollableTabRow(selectedTabIndex = selectedTab) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
            when (selectedTab) {
                0 -> MaintenanceTimelineScreen(autoOpenAddSheet = viewModel.initialOpenAdd)
                1 -> MaintenanceSuggestionsScreen()
                2 -> RemindersScreen()
                3 -> FuelScreen(autoOpenAddSheet = viewModel.initialOpenAdd)
                4 -> RecallsScreen()
                5 -> VehicleSpecsScreen()
                6 -> PartsScreen(onEditParts = { onEditParts(viewModel.vehicleId) })
            }
        }
    }
}
