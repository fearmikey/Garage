package com.fearmikey.garage.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripLoggerScreen(
    viewModel: TripViewModel = hiltViewModel()
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripRecord?>(null) }
    var selectedFilter by remember { mutableStateOf<TripCategory?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Trip")
            }
        },
    ) { innerPadding ->
        if (showAddDialog || editingTrip != null) {
            AddTripDialog(
                initialTrip = editingTrip,
                onDismiss = { 
                    showAddDialog = false
                    editingTrip = null
                },
                onSave = { 
                    viewModel.saveTrip(it)
                    showAddDialog = false
                    editingTrip = null
                }
            )
        }

        if (trips.isEmpty()) {
            EmptyState(
                message = "No trips logged yet.\nTap the + button to add one.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("All") }
                            )
                        }
                        items(TripCategory.entries) { category ->
                            FilterChip(
                                selected = selectedFilter == category,
                                onClick = { selectedFilter = category },
                                label = { 
                                    Text(category.name.lowercase()
                                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }) 
                                }
                            )
                        }
                    }
                }

                val filteredTrips = if (selectedFilter != null) {
                    trips.filter { it.category == selectedFilter }
                } else {
                    trips
                }

                items(filteredTrips) { trip ->
                    TripRecordCard(
                        trip = trip,
                        onEdit = { editingTrip = trip },
                        onDelete = { viewModel.deleteTrip(trip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripRecordCard(
    trip: TripRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = trip.category.name, 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${dateFormat.format(Date(trip.startDate))} - ${trip.calculatedDistance} mi", 
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { 
                                expanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { 
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            if (trip.notes.isNotBlank()) {
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (trip.reimbursementRate > 0) {
                Text(
                    text = "Deduction: $${String.format(Locale.getDefault(), "%.2f", trip.calculatedDistance * trip.reimbursementRate)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
