package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData

@Composable
fun VehicleSpecsScreen(
    viewModel: VehicleSpecsViewModel = hiltViewModel(),
) {
    val specs by viewModel.specs.collectAsStateWithLifecycle()
    VehicleSpecsContent(specs = specs)
}

/** A single group of related specs shown together under one heading. */
private data class SpecGroup(
    val title: String,
    val rows: List<Pair<String, String>>,
)

private fun VehicleSpecs.toGroups(): List<SpecGroup> = listOf(
    SpecGroup(
        title = "Engine & Drivetrain",
        rows = listOfNotNull(
            engineCylinders?.let { "Cylinders" to it },
            displacementL?.let { "Displacement" to "$it L" },
            engineHp?.let { "Horsepower" to "$it hp" },
            fuelType?.let { "Fuel type" to it },
            transmissionStyle?.let { "Transmission" to it },
            transmissionSpeeds?.let { "Transmission speeds" to it },
        ),
    ),
    SpecGroup(
        title = "Body",
        rows = listOfNotNull(
            vehicleType?.let { "Vehicle type" to it },
            bodyClass?.let { "Body class" to it },
            doors?.let { "Doors" to it },
            series?.let { "Series" to it },
            gvwr?.let { "GVWR" to it },
        ),
    ),
    SpecGroup(
        title = "Manufacturing",
        rows = listOfNotNull(
            manufacturer?.let { "Manufacturer" to it },
            listOfNotNull(plantCity, plantState, plantCountry).joinToString(", ").takeIf { it.isNotBlank() }
                ?.let { "Plant" to it },
        ),
    ),
).filter { it.rows.isNotEmpty() }

@Composable
private fun VehicleSpecsContent(specs: VehicleSpecs?) {
    val groups = specs?.takeUnless { it.isEmpty() }?.toGroups().orEmpty()

    Scaffold { innerPadding ->
        if (groups.isEmpty()) {
            EmptyState(
                message = "No specs yet.\nDecode this vehicle's VIN on the Edit screen to see full specs.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.title }) { group ->
                    SpecGroupCard(group)
                }
            }
        }
    }
}

@Composable
private fun SpecGroupCard(group: SpecGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                group.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            group.rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
private fun VehicleSpecsScreenPreview() {
    GarageTheme {
        VehicleSpecsContent(specs = SampleData.tacomaSpecs)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VehicleSpecsScreenEmptyPreview() {
    GarageTheme {
        VehicleSpecsContent(specs = null)
    }
}
