package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.SupplyRecord
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleType
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

/**
 * "Parts & Supplies" tab: the per-vehicle quick reference ([VehiclePartsInfo], local only) on top,
 * and the Supplies list ([SupplyRecord], synced with LubeLogger) below.
 */
@Composable
fun PartsScreen(
    onEditParts: () -> Unit = {},
    viewModel: PartsViewModel = hiltViewModel(),
) {
    val parts by viewModel.parts.collectAsStateWithLifecycle()
    val supplies by viewModel.supplies.collectAsStateWithLifecycle()
    val unitSystem by viewModel.unitSystem.collectAsStateWithLifecycle()
    val vehicleType by viewModel.vehicleType.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    PartsContent(
        parts = parts,
        supplies = supplies,
        unitSystem = unitSystem,
        vehicleType = vehicleType,
        currencySymbol = currencySymbol,
        onEditClicked = onEditParts,
        onSaveSupply = viewModel::saveSupply,
        onDeleteSupply = viewModel::deleteSupply,
    )
}

/** One row of the quick reference. [supplyDescription] is set on part-number rows ("Add as supply"). */
private data class PartsRow(
    val label: String,
    val value: String,
    val supplyDescription: String? = null,
)

/** A single group of related parts info shown together under one heading. */
private data class PartsGroup(
    val title: String,
    val rows: List<PartsRow>,
)

@Composable
private fun VehiclePartsInfo.toGroups(
    unitSystem: UnitSystem,
    vehicleType: VehicleType = VehicleType.CAR,
): List<PartsGroup> {
    // Motorcycles have no cabin air filter or wipers; hide those rows even if
    // stale values were stored before the vehicle was marked as a motorcycle.
    val isCar = vehicleType == VehicleType.CAR
    return listOf(
        PartsGroup(
            title = stringResource(R.string.parts_group_oil),
            rows = listOfNotNull(
                oilViscosity?.let { PartsRow(stringResource(R.string.parts_oil_viscosity), it) },
                oilCapacity?.let {
                    UnitConverter.formatOilCapacity(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_oil_capacity), formatted)
                    }
                },
                oilFilterPartNumber?.let {
                    PartsRow(stringResource(R.string.parts_oil_filter_num), it, stringResource(R.string.supply_desc_oil_filter))
                },
            ),
        ),
        PartsGroup(
            title = stringResource(R.string.parts_group_ignition),
            rows = listOfNotNull(
                sparkPlugPartNumber?.let {
                    PartsRow(stringResource(R.string.parts_spark_plug_num), it, stringResource(R.string.supply_desc_spark_plug))
                },
                sparkPlugGap?.let {
                    UnitConverter.formatSparkPlugGap(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_spark_plug_gap), formatted)
                    }
                },
            ),
        ),
        PartsGroup(
            title = stringResource(R.string.parts_group_filters),
            rows = listOfNotNull(
                engineAirFilterPartNumber?.let {
                    PartsRow(
                        stringResource(R.string.parts_engine_air_filter_num), it,
                        stringResource(R.string.supply_desc_engine_air_filter),
                    )
                },
                cabinAirFilterPartNumber?.takeIf { isCar }?.let {
                    PartsRow(
                        stringResource(R.string.parts_cabin_air_filter_num), it,
                        stringResource(R.string.supply_desc_cabin_air_filter),
                    )
                },
            ),
        ),
        PartsGroup(
            title = stringResource(R.string.parts_group_tires),
            rows = listOfNotNull(
                tireSizeFront?.let { PartsRow(stringResource(R.string.parts_front_tire_size), it) },
                tireSizeRear?.let { PartsRow(stringResource(R.string.parts_rear_tire_size), it) },
                tirePsiFront?.let {
                    UnitConverter.formatTirePressure(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_front_tire_psi), formatted)
                    }
                },
                tirePsiRear?.let {
                    UnitConverter.formatTirePressure(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_rear_tire_psi), formatted)
                    }
                },
            ),
        ),
        PartsGroup(
            title = stringResource(R.string.parts_group_wipers),
            rows = if (!isCar) emptyList() else listOfNotNull(
                wiperBladeSizeDriver?.let {
                    UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_driver_wiper), formatted)
                    }
                },
                wiperBladeSizePassenger?.let {
                    UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_passenger_wiper), formatted)
                    }
                },
                wiperBladeSizeRear?.let {
                    UnitConverter.formatWiperSize(it, unitSystem)?.let { formatted ->
                        PartsRow(stringResource(R.string.parts_rear_wiper), formatted)
                    }
                },
            ),
        ),
    ).filter { it.rows.isNotEmpty() }
}

@Composable
private fun PartsContent(
    parts: VehiclePartsInfo?,
    supplies: List<SupplyRecord> = emptyList(),
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    vehicleType: VehicleType = VehicleType.CAR,
    currencySymbol: String = "$",
    onEditClicked: () -> Unit = {},
    onSaveSupply: (SupplyRecord) -> Unit = {},
    onDeleteSupply: (SupplyRecord) -> Unit = {},
) {
    val groups = parts?.takeUnless { it.isEmpty() }?.toGroups(unitSystem, vehicleType).orEmpty()
    var showManualWarningDialog by remember { mutableStateOf(value = false) }
    // Supply being added (id 0, possibly pre-filled from a part number) or edited.
    var editingSupply by remember { mutableStateOf<SupplyRecord?>(null) }
    var deletingSupply by remember { mutableStateOf<SupplyRecord?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { editingSupply = SupplyRecord(vehicleId = 0, description = "") }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.supplies_cd_add))
            }
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScrollbar(listState),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "reference_header") {
                SectionHeader(title = stringResource(R.string.parts_reference_title)) {
                    IconButton(onClick = onEditClicked) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.parts_edit_title))
                    }
                }
            }

            if (groups.isEmpty()) {
                item(key = "reference_empty") {
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onEditClicked) {
                        Text(
                            text = stringResource(R.string.parts_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            } else {
                item(key = "manual_notice") {
                    ManualNoticeCard(onClick = { showManualWarningDialog = true })
                }
                items(groups, key = { "group_${it.title}" }) { group ->
                    PartsGroupCard(
                        group = group,
                        onAddAsSupply = { row ->
                            editingSupply = SupplyRecord(
                                vehicleId = 0,
                                description = row.supplyDescription.orEmpty(),
                                partNumber = row.value,
                            )
                        },
                    )
                }
            }

            item(key = "supplies_header") {
                SectionHeader(
                    title = stringResource(R.string.supplies_title) +
                        if (supplies.isNotEmpty()) " (${supplies.size})" else "",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (supplies.isEmpty()) {
                item(key = "supplies_empty") {
                    Text(
                        text = stringResource(R.string.supplies_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(supplies, key = { "supply_${it.id}" }) { supply ->
                    SupplyCard(
                        supply = supply,
                        currencySymbol = currencySymbol,
                        onClick = { editingSupply = supply },
                    )
                }
            }
        }
    }

    editingSupply?.let { supply ->
        SupplyEditDialog(
            initial = supply,
            currencySymbol = currencySymbol,
            onDismiss = { editingSupply = null },
            onSave = {
                onSaveSupply(it)
                editingSupply = null
            },
            onDelete = {
                editingSupply = null
                deletingSupply = it
            },
        )
    }

    deletingSupply?.let { supply ->
        AlertDialog(
            onDismissRequest = { deletingSupply = null },
            title = { Text(stringResource(R.string.supplies_delete_title)) },
            text = { Text(stringResource(R.string.supplies_delete_message, supply.description)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSupply(supply)
                        deletingSupply = null
                    },
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSupply = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
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
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
        action?.invoke()
    }
}

@Composable
private fun ManualNoticeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        onClick = onClick,
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

@Composable
private fun PartsGroupCard(
    group: PartsGroup,
    onAddAsSupply: (PartsRow) -> Unit,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val addAsSupplyLabel = stringResource(R.string.supplies_add_as_supply)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                group.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            group.rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (row.value.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(row.value))
                            }
                        }
                        .padding(vertical = if (row.supplyDescription != null) 0.dp else 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        row.value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                    if (row.supplyDescription != null) {
                        IconButton(onClick = { onAddAsSupply(row) }) {
                            Icon(
                                Icons.Outlined.AddShoppingCart,
                                contentDescription = "$addAsSupplyLabel: ${row.label}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PartsScreenPreview() {
    GarageTheme {
        PartsContent(
            parts = SampleData.tacomaPartsInfo,
            supplies = listOf(
                SupplyRecord(
                    id = 1, vehicleId = 1, description = "Oil filter", partNumber = "PF48",
                    supplier = "AutoZone", quantity = 2.0, cost = 17.98,
                ),
                SupplyRecord(id = 2, vehicleId = 1, description = "5W-30 synthetic oil (5 qt)", quantity = 1.0, cost = 32.5),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PartsScreenEmptyPreview() {
    GarageTheme {
        PartsContent(parts = null)
    }
}
