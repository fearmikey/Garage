package com.fearmikey.garage.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote
import com.fearmikey.garage.ui.components.EmptyState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.MoreVert

import com.mikepenz.markdown.m3.Markdown

@Composable
fun VehicleKnowledgeScreen(
    viewModel: KnowledgeViewModel = hiltViewModel()
) {
    val torqueSpecs by viewModel.torqueSpecs.collectAsStateWithLifecycle()
    val vehicleNotes by viewModel.vehicleNotes.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showAddSpec by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    
    var editingSpec by remember { mutableStateOf<TorqueSpec?>(null) }
    var editingNote by remember { mutableStateOf<VehicleNote?>(null) }

    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column {
                if (showMenu) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Torque Spec") },
                            onClick = { showMenu = false; showAddSpec = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Repair Note") },
                            onClick = { showMenu = false; showAddNote = true }
                        )
                    }
                }
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        },
    ) { innerPadding ->
        if (showAddSpec || editingSpec != null) {
            AddTorqueSpecDialog(
                initialSpec = editingSpec,
                onDismiss = { 
                    showAddSpec = false 
                    editingSpec = null
                },
                onSave = { 
                    viewModel.saveTorqueSpec(it)
                    showAddSpec = false 
                    editingSpec = null
                }
            )
        }
        if (showAddNote || editingNote != null) {
            AddVehicleNoteDialog(
                initialNote = editingNote,
                onDismiss = { 
                    showAddNote = false 
                    editingNote = null
                },
                onSave = { 
                    viewModel.saveVehicleNote(it)
                    showAddNote = false 
                    editingNote = null
                }
            )
        }

        if (torqueSpecs.isEmpty() && vehicleNotes.isEmpty()) {
            EmptyState(
                message = "No torque specs or notes found.\nTap the + button to add one.",
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
                                selected = selectedFilter == "All",
                                onClick = { selectedFilter = "All" },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "Torque Specs",
                                onClick = { selectedFilter = "Torque Specs" },
                                label = { Text("Torque Specs") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "Notes",
                                onClick = { selectedFilter = "Notes" },
                                label = { Text("Notes") }
                            )
                        }
                    }
                }

                if (torqueSpecs.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Torque Specs")) {
                    item {
                        Text(
                            text = "Torque Specs",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(torqueSpecs) { spec ->
                        TorqueSpecCard(
                            spec = spec,
                            onEdit = { editingSpec = spec },
                            onDelete = { viewModel.deleteTorqueSpec(spec) }
                        )
                    }
                }

                if (vehicleNotes.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Notes")) {
                    item {
                        Text(
                            text = "Repair Notes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(vehicleNotes) { note ->
                        VehicleNoteCard(
                            note = note,
                            onEdit = { editingNote = note },
                            onDelete = { viewModel.deleteVehicleNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueSpecCard(
    spec: TorqueSpec,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = spec.component, style = MaterialTheme.typography.titleSmall)
                Box {
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (spec.torqueNm != null) {
                    Text(text = "${spec.torqueNm} Nm", style = MaterialTheme.typography.bodyMedium)
                }
                if (spec.torqueFtLbs != null) {
                    Text(text = "${spec.torqueFtLbs} ft-lbs", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (spec.notes.isNotBlank()) {
                Text(
                    text = spec.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VehicleNoteCard(
    note: VehicleNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = note.title, style = MaterialTheme.typography.titleSmall)
                Box {
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
            Box(modifier = Modifier.padding(top = 8.dp)) {
                Markdown(
                    content = note.content
                )
            }
        }
    }
}
