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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.VehicleCard
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.vehicle.VehicleTab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long, tab: Int) -> Unit,
    onOpenSettings: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val fleetSummary by viewModel.fleetSummary.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    DashboardContent(
        vehicles = vehicles,
        fleetSummary = fleetSummary,
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
    fleetSummary: FleetSummary,
    unitSystem: UnitSystem,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (vehicleId: Long, tab: Int) -> Unit,
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
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScrollbar(listState),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "fleet_summary") {
                    FleetSummaryCard(
                        summary = fleetSummary,
                        unitSystem = unitSystem,
                        onOpenOverdueReminders = {
                            vehicles.firstOrNull { it.overdueReminderCount > 0 || it.upcomingReminderCount > 0 }
                                ?.let { onOpenVehicle(it.vehicle.id, VehicleTab.SCHEDULE.ordinal) }
                        },
                    )
                }

                items(vehicles, key = { it.vehicle.id }) { item ->
                    VehicleCard(
                        vehicle = item.vehicle,
                        latestMileage = item.latestMileage,
                        imageFile = item.imageFile,
                        unitSystem = unitSystem,
                        avgMpg = item.avgMpg,
                        overdueReminderCount = item.overdueReminderCount,
                        upcomingReminderCount = item.upcomingReminderCount,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onOpenVehicle(item.vehicle.id, 0) },
                        onOpenReminders = { onOpenVehicle(item.vehicle.id, VehicleTab.SCHEDULE.ordinal) },
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
            AnimatedContent(targetState = Unit, label = "DashboardScreenPreview") { target ->
                if (target == Unit) {
                    DashboardContent(
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
                        onAddVehicle = {},
                        onOpenVehicle = { _, _ -> },
                        onOpenSettings = {},
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
                        vehicles = emptyList(),
                        fleetSummary = FleetSummary(),
                        unitSystem = UnitSystem.IMPERIAL,
                        onAddVehicle = {},
                        onOpenVehicle = { _, _ -> },
                        onOpenSettings = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                }
            }
        }
    }
}
