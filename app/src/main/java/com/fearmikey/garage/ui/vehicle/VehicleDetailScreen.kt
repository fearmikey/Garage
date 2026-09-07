package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.ui.maintenance.MaintenanceSuggestionsScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceTimelineScreen
import com.fearmikey.garage.ui.reminder.RemindersScreen

private val TAB_TITLES = listOf("Timeline", "Suggested", "Reminders", "Specs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

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
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
            when (selectedTab) {
                0 -> MaintenanceTimelineScreen()
                1 -> MaintenanceSuggestionsScreen()
                2 -> RemindersScreen()
                3 -> VehicleSpecsScreen()
            }
        }
    }
}
