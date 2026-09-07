package com.fearmikey.garage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import java.io.File

/** Summary card for a [Vehicle], used in the Dashboard grid/list. */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    latestMileage: Int?,
    imageFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (imageFile != null && imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = vehicleLabel(vehicle),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = vehicleLabel(vehicle),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (vehicle.trim.isNotBlank()) {
                    Text(
                        text = vehicle.trim,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val mileageText = latestMileage?.let { "$it mi" } ?: "No mileage logged yet"
                Text(
                    text = mileageText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun vehicleLabel(vehicle: Vehicle): String =
    listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
        .joinToString(" ")
        .ifBlank { vehicle.vin.ifBlank { "Unnamed vehicle" } }

@Preview(showBackground = true)
@Composable
private fun VehicleCardPreview() {
    GarageTheme {
        VehicleCard(
            vehicle = SampleData.tacoma,
            latestMileage = SampleData.TACOMA_LATEST_MILEAGE,
            imageFile = null,
            onClick = {},
        )
    }
}
