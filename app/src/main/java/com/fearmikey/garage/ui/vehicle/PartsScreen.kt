package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData

@Composable
fun PartsScreen(
    onEditParts: () -> Unit = {},
    viewModel: PartsViewModel = hiltViewModel(),
) {
    val parts by viewModel.parts.collectAsStateWithLifecycle()

    PartsContent(
        parts = parts,
        onEditClicked = onEditParts,
    )
}

/** A single group of related parts info shown together under one heading. */
private data class PartsGroup(
    val title: String,
    val rows: List<Pair<String, String>>,
)

private fun VehiclePartsInfo.toGroups(): List<PartsGroup> = listOf(
    PartsGroup(
        title = "Oil",
        rows = listOfNotNull(
            oilViscosity?.let { "Oil viscosity" to it },
            oilCapacity?.let { "Oil capacity" to it },
            oilFilterPartNumber?.let { "Oil filter part #" to it },
        ),
    ),
    PartsGroup(
        title = "Ignition",
        rows = listOfNotNull(
            sparkPlugPartNumber?.let { "Spark plug part #" to it },
            sparkPlugGap?.let { "Spark plug gap" to it },
        ),
    ),
    PartsGroup(
        title = "Tires",
        rows = listOfNotNull(
            tireSizeFront?.let { "Front tire size" to it },
            tireSizeRear?.let { "Rear tire size" to it },
            tirePsiFront?.let { "Front tire PSI" to it },
            tirePsiRear?.let { "Rear tire PSI" to it },
        ),
    ),
    PartsGroup(
        title = "Wipers",
        rows = listOfNotNull(
            wiperBladeSizeDriver?.let { "Driver wiper size" to it },
            wiperBladeSizePassenger?.let { "Passenger wiper size" to it },
            wiperBladeSizeRear?.let { "Rear wiper size" to it },
        ),
    ),
).filter { it.rows.isNotEmpty() }

@Composable
private fun PartsContent(
    parts: VehiclePartsInfo?,
    onEditClicked: () -> Unit,
) {
    val groups = parts?.takeUnless { it.isEmpty() }?.toGroups().orEmpty()
    var showManualWarningDialog by remember { mutableStateOf(value = false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onEditClicked) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit parts & fluids")
            }
        },
    ) { innerPadding ->
        if (groups.isEmpty()) {
            EmptyState(
                message = "No parts & fluids info yet.\nTap the edit button to add your vehicle's specs.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "manual_notice") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        onClick = { showManualWarningDialog = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Owner's Manual Verification",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Double check specs with your vehicle's manual to ensure accuracy. Tap for details.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                items(groups, key = { it.title }) { group ->
                    PartsGroupCard(group)
                }
            }
        }
    }

    if (showManualWarningDialog) {
        AlertDialog(
            onDismissRequest = { showManualWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("Verify with Owner's Manual") },
            text = {
                Text(
                    "The fluid capacities (e.g. oil capacity & viscosity) and part specifications shown on this screen are estimated using decoded VIN and engine specifications.\n\n" +
                        "Factory options, engine revisions, and regional variations can differ. Always double check these values with your vehicle's official owner's manual or factory service documentation to ensure exact accuracy before purchasing fluids or performing service.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(onClick = { showManualWarningDialog = false }) {
                    Text("Got It")
                }
            },
        )
    }
}

@Composable
private fun PartsGroupCard(group: PartsGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                group.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            group.rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PartsScreenPreview() {
    GarageTheme {
        PartsContent(parts = SampleData.tacomaPartsInfo, onEditClicked = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PartsScreenEmptyPreview() {
    GarageTheme {
        PartsContent(parts = null, onEditClicked = {})
    }
}
