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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private fun formatWeight(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return value
    if (trimmed.contains("lb", ignoreCase = true) || trimmed.contains("kg", ignoreCase = true)) {
        return trimmed
    }
    return if (trimmed.toDoubleOrNull() != null) "$trimmed lbs" else trimmed
}

@Composable
private fun VehicleSpecs.toGroups(): List<SpecGroup> {
    val hasTowingOrWeightSpecs = listOf(trailerBrakedCapacity, trailerUnbrakedCapacity, gcwr, curbWeight)
        .any { !it.isNullOrBlank() }

    return listOf(
        SpecGroup(
            title = stringResource(R.string.specs_group_engine),
            rows = listOfNotNull(
                engineCylinders?.let { stringResource(R.string.specs_cylinders) to it },
                displacementL?.let { stringResource(R.string.specs_displacement) to "$it L" },
                engineHp?.let { stringResource(R.string.specs_horsepower) to "$it hp" },
                fuelType?.let { stringResource(R.string.specs_fuel_type) to it },
                transmissionStyle?.let { stringResource(R.string.specs_transmission) to it },
                transmissionSpeeds?.let { stringResource(R.string.specs_transmission_speeds) to it },
            ),
        ),
        SpecGroup(
            title = stringResource(R.string.specs_group_weights),
            rows = if (hasTowingOrWeightSpecs) {
                listOfNotNull(
                    trailerBrakedCapacity?.let { stringResource(R.string.specs_towing_braked) to formatWeight(it) },
                    trailerUnbrakedCapacity?.let { stringResource(R.string.specs_towing_unbraked) to formatWeight(it) },
                    gcwr?.let { stringResource(R.string.specs_gcwr) to formatWeight(it) },
                    gvwr?.let { stringResource(R.string.specs_gvwr) to it },
                    curbWeight?.let { stringResource(R.string.specs_curb_weight) to formatWeight(it) },
                )
            } else {
                emptyList()
            },
        ),
        SpecGroup(
            title = stringResource(R.string.specs_group_body),
            rows = listOfNotNull(
                vehicleType?.let { stringResource(R.string.specs_vehicle_type) to it },
                bodyClass?.let { stringResource(R.string.specs_body_class) to it },
                doors?.let { stringResource(R.string.specs_doors) to it },
                series?.let { stringResource(R.string.specs_series) to it },
                if (!hasTowingOrWeightSpecs) gvwr?.let { stringResource(R.string.specs_gvwr) to it } else null,
            ),
        ),
        SpecGroup(
            title = stringResource(R.string.specs_group_manufacturing),
            rows = listOfNotNull(
                manufacturer?.let { stringResource(R.string.specs_manufacturer) to it },
                listOfNotNull(plantCity, plantState, plantCountry).joinToString(", ").takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.specs_plant) to it },
            ),
        ),
    ).filter { it.rows.isNotEmpty() }
}

@Composable
private fun VehicleSpecsContent(specs: VehicleSpecs?) {
    val groups = specs?.takeUnless { it.isEmpty() }?.toGroups().orEmpty()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        if (groups.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.specs_empty_message),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScrollbar(listState),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
