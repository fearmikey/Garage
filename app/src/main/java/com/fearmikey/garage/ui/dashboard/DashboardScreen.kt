package com.fearmikey.garage.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.VehicleCard
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    DashboardContent(
        vehicles = vehicles,
        unitSystem = unitSystem,
        onAddVehicle = onAddVehicle,
        onOpenVehicle = onOpenVehicle,
        onOpenSettings = onOpenSettings,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DashboardContent(
    vehicles: List<VehicleListItem>,
    unitSystem: UnitSystem,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
) {
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
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Filled.Add, contentDescription = "Add vehicle")
            }
        },
    ) { innerPadding ->
        if (vehicles.isEmpty()) {
            EmptyState(
                message = "No vehicles yet.\nTap + to add your first one.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vehicles, key = { it.vehicle.id }) { item ->
                    VehicleCard(
                        vehicle = item.vehicle,
                        latestMileage = item.latestMileage,
                        imageFile = item.imageFile,
                        unitSystem = unitSystem,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onOpenVehicle(item.vehicle.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    GarageTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = Unit, label = "DashboardScreenPreview") { _ ->
                DashboardContent(
                    vehicles = listOf(
                        VehicleListItem(SampleData.tacoma, SampleData.TACOMA_LATEST_MILEAGE, null),
                        VehicleListItem(SampleData.civic, 42000, null),
                    ),
                    unitSystem = UnitSystem.IMPERIAL,
                    onAddVehicle = {},
                    onOpenVehicle = {},
                    onOpenSettings = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                )
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
            AnimatedContent(targetState = Unit, label = "DashboardScreenEmptyPreview") { _ ->
                DashboardContent(
                    vehicles = emptyList(),
                    unitSystem = UnitSystem.IMPERIAL,
                    onAddVehicle = {},
                    onOpenVehicle = {},
                    onOpenSettings = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                )
            }
        }
    }
}


