package com.fearmikey.garage.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem

@Composable
fun FleetSummaryCard(
    summary: FleetSummary,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    onOpenOverdueReminders: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fleet Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(
                    icon = Icons.Outlined.DirectionsCar,
                    label = "Vehicles",
                    value = summary.totalVehicles.toString(),
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    icon = Icons.Outlined.LocalGasStation,
                    label = "Fleet Avg",
                    value = UnitConverter.formatFuelEconomy(summary.fleetAvgMpg, unitSystem),
                    modifier = Modifier.weight(1f),
                )

                val (alertText, alertColor) = when {
                    summary.totalOverdueReminders > 0 -> {
                        "${summary.totalOverdueReminders} Overdue" to MaterialTheme.colorScheme.error
                    }
                    summary.totalUpcomingReminders > 0 -> {
                        "${summary.totalUpcomingReminders} Due Soon" to MaterialTheme.colorScheme.tertiary
                    }
                    else -> {
                        "All Clear" to MaterialTheme.colorScheme.primary
                    }
                }

                val hasAlerts = summary.totalOverdueReminders > 0 || summary.totalUpcomingReminders > 0
                val statusModifier = if (onOpenOverdueReminders != null && hasAlerts) {
                    Modifier
                        .weight(1f)
                        .clickable { onOpenOverdueReminders() }
                } else {
                    Modifier.weight(1f)
                }

                StatItem(
                    icon = if (summary.totalOverdueReminders > 0) Icons.Outlined.WarningAmber else Icons.Outlined.Notifications,
                    label = "Status",
                    value = alertText,
                    valueColor = alertColor,
                    modifier = statusModifier,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FleetSummaryCardPreview() {
    GarageTheme {
        FleetSummaryCard(
            summary = FleetSummary(
                totalVehicles = 2,
                fleetAvgMpg = 21.5,
                totalOverdueReminders = 1,
                totalUpcomingReminders = 2,
            ),
            unitSystem = UnitSystem.IMPERIAL,
            modifier = Modifier.padding(16.dp),
        )
    }
}
