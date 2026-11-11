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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.util.label
import com.fearmikey.garage.ui.util.toDisplayDate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.fearmikey.garage.service.TripTrackingService
import com.fearmikey.garage.data.local.entity.TripRecord
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.ui.components.EmptyState

@Composable
fun TripLoggerScreen(
    viewModel: TripViewModel = hiltViewModel(),
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val defaultRate by viewModel.defaultReimbursementRate.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(value = false) }
    var editingTrip by remember { mutableStateOf<TripRecord?>(null) }
    var selectedFilter by remember { mutableStateOf<TripCategory?>(null) }

    // After a GPS trip is stopped and saved, open it for editing (category, notes, etc.).
    val lastSavedGpsTrip by viewModel.lastSavedGpsTrip.collectAsStateWithLifecycle()
    LaunchedEffect(lastSavedGpsTrip) {
        val saved = lastSavedGpsTrip ?: return@LaunchedEffect
        if (saved.vehicleId == viewModel.vehicleId) {
            editingTrip = saved
        }
        viewModel.consumeLastSavedGpsTrip()
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trip_cd_add))
            }
        },
    ) { innerPadding ->
        if (showAddDialog || (editingTrip != null)) {
            AddTripDialog(
                initialTrip = editingTrip,
                defaultRate = defaultRate,
                currencySymbol = currency.symbol,
                distanceUnit = unitSystem.distanceUnit,
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

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ActiveTripCard(viewModel = viewModel, distanceUnit = unitSystem.distanceUnit)
            }

            if (trips.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text(stringResource(R.string.trend_range_all)) }
                            )
                        }
                        items(TripCategory.entries) { category ->
                            FilterChip(
                                selected = selectedFilter == category,
                                onClick = { selectedFilter = category },
                                label = { Text(category.label()) }
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
                        currencySymbol = currency.symbol,
                        distanceUnit = unitSystem.distanceUnit,
                        onEdit = { editingTrip = trip },
                        onDelete = { viewModel.deleteTrip(trip) },
                    )
                }
            } else {
                item {
                    EmptyState(
                        message = stringResource(R.string.trip_empty),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TripRecordCard(
    trip: TripRecord,
    currencySymbol: String,
    distanceUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(value = false) }
    val currentLocale = androidx.compose.ui.text.intl.Locale.current.platformLocale

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
                        text = trip.category.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${trip.startDate.toDisplayDate()} · ${trip.calculatedDistance} $distanceUnit",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.trip_cd_more))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_edit)) },
                            onClick = { 
                                expanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete)) },
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
                    text = stringResource(
                        R.string.trip_deduction,
                        currencySymbol + String.format(currentLocale, "%.2f", trip.calculatedDistance * trip.reimbursementRate),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveTripCard(
    viewModel: TripViewModel,
    distanceUnit: String,
) {
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        Log.d("TripLoggerScreen", "Permission result: fine=$fineGranted, coarse=$coarseGranted")
        if (fineGranted || coarseGranted) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = TripTrackingService.ACTION_START
                putExtra(TripTrackingService.EXTRA_VEHICLE_ID, viewModel.vehicleId)
            }
            context.startForegroundService(intent)
        }
    }
    
    when (val state = tripState) {
        is TripTrackingService.TripState.Stopped -> {
            Button(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    Log.d("TripLoggerScreen", "Start clicked: hasFine=$hasFine, hasCoarse=$hasCoarse")

                    val permissionsToRequest = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )

                    if (hasFine || hasCoarse) {
                        val intent = Intent(context, TripTrackingService::class.java).apply {
                            action = TripTrackingService.ACTION_START
                            putExtra(TripTrackingService.EXTRA_VEHICLE_ID, viewModel.vehicleId)
                        }
                        context.startForegroundService(intent)
                    } else {
                        locationPermissionLauncher.launch(permissionsToRequest)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.trip_start_gps))
            }
        }
        is TripTrackingService.TripState.Active -> {
            val distance = state.distanceMeters / if (distanceUnit == "km") 1000.0 else 1609.344
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(if (state.isPaused) R.string.trip_paused else R.string.trip_tracking),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "%.2f %s".format(distance, distanceUnit),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (state.isPaused) {
                            Button(onClick = {
                                context.startService(Intent(context, TripTrackingService::class.java).apply { action = TripTrackingService.ACTION_RESUME })
                            }) {
                                Text(stringResource(R.string.trip_resume))
                            }
                        } else {
                            OutlinedButton(onClick = {
                                context.startService(Intent(context, TripTrackingService::class.java).apply { action = TripTrackingService.ACTION_PAUSE })
                            }) {
                                Text(stringResource(R.string.trip_pause))
                            }
                        }
                        
                        Button(onClick = {
                            // The service saves the trip; the screen then opens it for editing.
                            context.startService(Intent(context, TripTrackingService::class.java).apply { action = TripTrackingService.ACTION_STOP })
                        }) {
                            Text(stringResource(R.string.trip_stop_save))
                        }
                    }
                }
            }
        }
    }
}
