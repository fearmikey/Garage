package com.fearmikey.garage.ui.cost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.components.SectionHeader
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.toDisplayDate

@Composable
fun CostOfOwnershipScreen(
    viewModel: CostOfOwnershipViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CostOfOwnershipContent(
        uiState = uiState,
        onTimeFilterSelected = viewModel::setTimeFilter,
    )
}

@Composable
private fun CostOfOwnershipContent(
    uiState: CostOfOwnershipUiState,
    onTimeFilterSelected: (TimeFilter) -> Unit,
) {
    if (uiState.categories.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TimeFilterRow(
                selectedFilter = uiState.selectedTimeFilter,
                onFilterSelected = onTimeFilterSelected,
            )
            EmptyState(
                message = "No cost records logged yet.\nAdd maintenance or fuel entries to track cost of ownership.",
                icon = Icons.Outlined.Calculate,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TotalCostCard(uiState = uiState)
            }

            item {
                TimeFilterRow(
                    selectedFilter = uiState.selectedTimeFilter,
                    onFilterSelected = onTimeFilterSelected,
                )
            }

            item {
                SectionHeader(title = "Category Breakdown")
            }

            items(uiState.categories, key = { it.key }) { category ->
                CategoryCostCard(
                    category = category,
                    unitSystem = uiState.unitSystem,
                    currencySymbol = uiState.currencySymbol,
                )
            }
        }
    }
}

@Composable
private fun TotalCostCard(
    uiState: CostOfOwnershipUiState,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "Total Cost of Ownership",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%s%.2f".format(uiState.currencySymbol, uiState.totalCost),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CostSubStat(
                    title = "Maintenance & Repairs",
                    amount = uiState.maintenanceCost,
                    countText = "${uiState.maintenanceRecordCount} records",
                    icon = Icons.Default.Handyman,
                    currencySymbol = uiState.currencySymbol,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                Spacer(modifier = Modifier.width(16.dp))

                CostSubStat(
                    title = "Fuel Spent",
                    amount = uiState.fuelCost,
                    countText = "${uiState.fuelRecordCount} fill-ups",
                    icon = Icons.Default.LocalGasStation,
                    currencySymbol = uiState.currencySymbol,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun CostSubStat(
    title: String,
    amount: Double,
    countText: String,
    icon: ImageVector,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                )
                Text(
                    text = "%s%.2f".format(currencySymbol, amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun TimeFilterRow(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimeFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
            )
        }
    }
}

@Composable
private fun CategoryCostCard(
    category: CategoryCostItem,
    unitSystem: UnitSystem,
    currencySymbol: String = "$",
) {
    var isExpanded by remember { mutableStateOf(false) }
    val icon = getCategoryIcon(category.key, category.category)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${category.recordCount} ${if (category.recordCount == 1) "entry" else "entries"} · %.1f%% of total".format(category.percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "%s%.2f".format(currencySymbol, category.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (category.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    category.entries.forEach { entry ->
                        CostEntryRow(entry = entry, unitSystem = unitSystem, currencySymbol = currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
private fun CostEntryRow(
    entry: CostEntry,
    unitSystem: UnitSystem,
    currencySymbol: String = "$",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row {
                Text(
                    text = "${entry.date.toDisplayDate()} · ${UnitConverter.formatDistance(entry.mileage, unitSystem)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!entry.detail.isNullOrBlank()) {
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "%s%.2f".format(currencySymbol, entry.cost),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun getCategoryIcon(key: String, category: MaintenanceCategory?): ImageVector {
    if (key == "FUEL") return Icons.Default.LocalGasStation
    return when (category) {
        MaintenanceCategory.TIRES -> Icons.Default.Autorenew
        MaintenanceCategory.BRAKES -> Icons.Default.Handyman
        MaintenanceCategory.BATTERY -> Icons.Default.ElectricalServices
        MaintenanceCategory.FLUIDS -> Icons.Default.WaterDrop
        MaintenanceCategory.INSPECTION -> Icons.Default.Search
        MaintenanceCategory.REPAIR -> Icons.Default.BuildCircle
        MaintenanceCategory.OTHER, null -> Icons.Default.Build
    }
}

@Preview(showBackground = true)
@Composable
private fun CostOfOwnershipPreview() {
    GarageTheme {
        val sampleUiState = CostOfOwnershipUiState(
            totalCost = 559.42,
            maintenanceCost = 340.49,
            fuelCost = 218.93,
            maintenanceRecordCount = 3,
            fuelRecordCount = 4,
            categories = listOf(
                CategoryCostItem(
                    key = "FUEL",
                    title = "Fuel",
                    totalCost = 218.93,
                    percentage = 39.1f,
                    recordCount = 4,
                    entries = listOf(
                        CostEntry(1, System.currentTimeMillis(), "Fuel Fill-Up", 62.48, 15230, "17.2 gal @ $3.63/gal"),
                        CostEntry(2, System.currentTimeMillis() - 864000000, "Fuel Fill-Up", 34.03, 14890, "9.4 gal @ $3.62/gal"),
                    ),
                ),
                CategoryCostItem(
                    key = "BRAKES",
                    title = "Brakes",
                    totalCost = 210.50,
                    percentage = 37.6f,
                    recordCount = 1,
                    entries = listOf(
                        CostEntry(3, System.currentTimeMillis() - 17280000000, "Front Brake Pads", 210.50, 8000, "Replaced front brake pads"),
                    ),
                    category = MaintenanceCategory.BRAKES,
                ),
                CategoryCostItem(
                    key = "FLUIDS",
                    title = "Fluids",
                    totalCost = 89.99,
                    percentage = 16.1f,
                    recordCount = 1,
                    entries = listOf(
                        CostEntry(4, System.currentTimeMillis() - 864000000, "Engine Oil Change", 89.99, 15230, "Full synthetic engine oil change + filter"),
                    ),
                    category = MaintenanceCategory.FLUIDS,
                ),
                CategoryCostItem(
                    key = "TIRES",
                    title = "Tires",
                    totalCost = 40.00,
                    percentage = 7.2f,
                    recordCount = 1,
                    entries = listOf(
                        CostEntry(5, System.currentTimeMillis() - 8208000000, "Rotation and Balance", 40.00, 12100, "Rotated tires, checked tread depth"),
                    ),
                    category = MaintenanceCategory.TIRES,
                ),
            ),
            unitSystem = UnitSystem.IMPERIAL,
        )
        CostOfOwnershipContent(
            uiState = sampleUiState,
            onTimeFilterSelected = {},
        )
    }
}
