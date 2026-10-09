package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fearmikey.garage.ui.components.verticalScrollbar
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

@Composable
fun PartsScreen(
    onEditParts: () -> Unit = {},
    viewModel: PartsViewModel = hiltViewModel(),
) {
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()

    PartsContent(
        parts = parts,
        unitSystem = unitSystem,
        onEditClicked = onEditParts,
    )
}

/** A single group of related parts info shown together under one heading. */
private data class PartsGroup(
    val title: String,
    val rows: List<Pair<String, String>>,
)

@Composable
private fun VehiclePartsInfo.toGroups(unitSystem: UnitSystem): List<PartsGroup> = listOf(
    PartsGroup(
        title = stringResource(R.string.parts_group_oil),
        rows = listOfNotNull(
            oilViscosity?.let { stringResource(R.string.parts_oil_viscosity) to it },
            oilCapacity?.let { UnitConverter.formatOilCapacity(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_oil_capacity) to formatted } },
            oilFilterPartNumber?.let { stringResource(R.string.parts_oil_filter_num) to it },
        ),
    ),
    PartsGroup(
        title = stringResource(R.string.parts_group_ignition),
        rows = listOfNotNull(
            sparkPlugPartNumber?.let { stringResource(R.string.parts_spark_plug_num) to it },
            sparkPlugGap?.let { UnitConverter.formatSparkPlugGap(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_spark_plug_gap) to formatted } },
        ),
    ),
    PartsGroup(
        title = stringResource(R.string.parts_group_filters),
        rows = listOfNotNull(
            engineAirFilterPartNumber?.let { stringResource(R.string.parts_engine_air_filter_num) to it },
            cabinAirFilterPartNumber?.let { stringResource(R.string.parts_cabin_air_filter_num) to it },
        ),
    ),
    PartsGroup(
        title = stringResource(R.string.parts_group_tires),
        rows = listOfNotNull(
            tireSizeFront?.let { stringResource(R.string.parts_front_tire_size) to it },
            tireSizeRear?.let { stringResource(R.string.parts_rear_tire_size) to it },
            tirePsiFront?.let { UnitConverter.formatTirePressure(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_front_tire_psi) to formatted } },
            tirePsiRear?.let { UnitConverter.formatTirePressure(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_rear_tire_psi) to formatted } },
        ),
    ),
    PartsGroup(
        title = stringResource(R.string.parts_group_wipers),
        rows = listOfNotNull(
            wiperBladeSizeDriver?.let { UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_driver_wiper) to formatted } },
            wiperBladeSizePassenger?.let { UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_passenger_wiper) to formatted } },
            wiperBladeSizeRear?.let { UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted -> stringResource(R.string.parts_rear_wiper) to formatted } },
        ),
    ),
).filter { it.rows.isNotEmpty() }

@Composable
private fun PartsContent(
    parts: VehiclePartsInfo?,
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    onEditClicked: () -> Unit,
) {
    val groups = parts?.takeUnless { it.isEmpty() }?.toGroups(unitSystem).orEmpty()
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
                message = stringResource(R.string.parts_empty_message),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScrollbar(listState),
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
                                    text = stringResource(R.string.parts_manual_verification_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.parts_manual_verification_desc),
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
            title = { Text(stringResource(R.string.parts_verify_title)) },
            text = {
                Text(
                    stringResource(R.string.parts_verify_msg),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(onClick = { showManualWarningDialog = false }) {
                    Text(stringResource(R.string.parts_got_it))
                }
            },
        )
    }
}

@Composable
private fun PartsGroupCard(group: PartsGroup) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

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
                        .clickable {
                            if (value.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(value))
                            }
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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
        PartsContent(parts = SampleData.tacomaPartsInfo) {}
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PartsScreenEmptyPreview() {
    GarageTheme {
        PartsContent(parts = null) {}
    }
}
