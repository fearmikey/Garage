package com.fearmikey.garage.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    DashboardContent(
        vehicles = vehicles,
        onAddVehicle = onAddVehicle,
        onOpenVehicle = onOpenVehicle,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    vehicles: List<VehicleListItem>,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onOpenSettings: () -> Unit,
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
                contentPadding = PaddingValues(16.dp),
            ) {
                items(vehicles, key = { it.vehicle.id }) { item ->
                    VehicleCard(
                        vehicle = item.vehicle,
                        latestMileage = item.latestMileage,
                        imageFile = item.imageFile,
                        onClick = { onOpenVehicle(item.vehicle.id) },
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    GarageTheme {
        DashboardContent(
            vehicles = listOf(
                VehicleListItem(SampleData.tacoma, SampleData.TACOMA_LATEST_MILEAGE, null),
                VehicleListItem(SampleData.civic, 42000, null),
            ),
            onAddVehicle = {},
            onOpenVehicle = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    GarageTheme {
        DashboardContent(
            vehicles = emptyList(),
            onAddVehicle = {},
            onOpenVehicle = {},
            onOpenSettings = {},
        )
    }
}



