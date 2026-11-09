package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.fearmikey.garage.data.local.entity.CustomSpec
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
    var showEditor by rememberSaveable { mutableStateOf(false) }

    VehicleSpecsContent(specs = specs, onEditClicked = { showEditor = true })

    if (showEditor) {
        EditSpecsSheet(
            initial = specs ?: VehicleSpecs(vehicleId = viewModel.vehicleId),
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.saveSpecs(it)
                showEditor = false
            },
        )
    }
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

/** Appends [unit] only when the user entered a bare number (so "1.8L" doesn't become "1.8L L"). */
private fun withUnit(value: String, unit: String): String =
    if (value.trim().toDoubleOrNull() != null) "${value.trim()} $unit" else value

@Composable
private fun VehicleSpecs.toGroups(): List<SpecGroup> {
    val hasTowingOrWeightSpecs = listOf(trailerBrakedCapacity, trailerUnbrakedCapacity, gcwr, curbWeight)
        .any { !it.isNullOrBlank() }

    return listOf(
        SpecGroup(
            title = stringResource(R.string.specs_group_engine),
            rows = listOfNotNull(
                engineCylinders?.let { stringResource(R.string.specs_cylinders) to it },
                displacementL?.let { stringResource(R.string.specs_displacement) to withUnit(it, "L") },
                engineHp?.let { stringResource(R.string.specs_horsepower) to withUnit(it, "hp") },
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
        SpecGroup(
            title = stringResource(R.string.specs_group_custom),
            rows = customSpecs
                .filter { it.label.isNotBlank() || it.value.isNotBlank() }
                .map { it.label to it.value },
        ),
    ).filter { it.rows.isNotEmpty() }
}

@Composable
private fun VehicleSpecsContent(specs: VehicleSpecs?, onEditClicked: () -> Unit = {}) {
    val groups = specs?.takeUnless { it.isEmpty() }?.toGroups().orEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onEditClicked) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.specs_edit))
            }
        },
    ) { innerPadding ->
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
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
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

/** One editable standard spec field: how to read it from and write it into [VehicleSpecs]. */
private data class EditableSpecField(
    val labelRes: Int,
    val get: (VehicleSpecs) -> String?,
    val set: (VehicleSpecs, String) -> VehicleSpecs,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val suffix: String? = null,
)

private val editableSpecGroups: List<Pair<Int, List<EditableSpecField>>> = listOf(
    R.string.specs_group_engine to listOf(
        EditableSpecField(R.string.specs_cylinders, { it.engineCylinders }, { s, v -> s.copy(engineCylinders = v) }, KeyboardType.Number),
        EditableSpecField(R.string.specs_displacement, { it.displacementL }, { s, v -> s.copy(displacementL = v) }, KeyboardType.Decimal, "L"),
        EditableSpecField(R.string.specs_horsepower, { it.engineHp }, { s, v -> s.copy(engineHp = v) }, KeyboardType.Decimal, "hp"),
        EditableSpecField(R.string.specs_fuel_type, { it.fuelType }, { s, v -> s.copy(fuelType = v) }),
        EditableSpecField(R.string.specs_transmission, { it.transmissionStyle }, { s, v -> s.copy(transmissionStyle = v) }),
        EditableSpecField(R.string.specs_transmission_speeds, { it.transmissionSpeeds }, { s, v -> s.copy(transmissionSpeeds = v) }, KeyboardType.Number),
    ),
    R.string.specs_group_body to listOf(
        EditableSpecField(R.string.specs_vehicle_type, { it.vehicleType }, { s, v -> s.copy(vehicleType = v) }),
        EditableSpecField(R.string.specs_body_class, { it.bodyClass }, { s, v -> s.copy(bodyClass = v) }),
        EditableSpecField(R.string.specs_doors, { it.doors }, { s, v -> s.copy(doors = v) }, KeyboardType.Number),
        EditableSpecField(R.string.specs_series, { it.series }, { s, v -> s.copy(series = v) }),
    ),
    R.string.specs_group_weights to listOf(
        EditableSpecField(R.string.specs_curb_weight, { it.curbWeight }, { s, v -> s.copy(curbWeight = v) }),
        EditableSpecField(R.string.specs_gvwr, { it.gvwr }, { s, v -> s.copy(gvwr = v) }),
        EditableSpecField(R.string.specs_gcwr, { it.gcwr }, { s, v -> s.copy(gcwr = v) }),
        EditableSpecField(R.string.specs_towing_braked, { it.trailerBrakedCapacity }, { s, v -> s.copy(trailerBrakedCapacity = v) }),
        EditableSpecField(R.string.specs_towing_unbraked, { it.trailerUnbrakedCapacity }, { s, v -> s.copy(trailerUnbrakedCapacity = v) }),
    ),
    R.string.specs_group_manufacturing to listOf(
        EditableSpecField(R.string.specs_manufacturer, { it.manufacturer }, { s, v -> s.copy(manufacturer = v) }),
        EditableSpecField(R.string.specs_plant_city, { it.plantCity }, { s, v -> s.copy(plantCity = v) }),
        EditableSpecField(R.string.specs_plant_state, { it.plantState }, { s, v -> s.copy(plantState = v) }),
        EditableSpecField(R.string.specs_plant_country, { it.plantCountry }, { s, v -> s.copy(plantCountry = v) }),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSpecsSheet(
    initial: VehicleSpecs,
    onDismiss: () -> Unit,
    onSave: (VehicleSpecs) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.specs_edit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.specs_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            editableSpecGroups.forEach { (titleRes, fields) ->
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                fields.forEach { field ->
                    OutlinedTextField(
                        value = field.get(draft).orEmpty(),
                        onValueChange = { draft = field.set(draft, it) },
                        label = { Text(stringResource(field.labelRes)) },
                        suffix = field.suffix?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Text(
                stringResource(R.string.specs_group_custom),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            draft.customSpecs.forEachIndexed { index, custom ->
                fun update(newSpec: CustomSpec) {
                    draft = draft.copy(
                        customSpecs = draft.customSpecs.toMutableList().also { it[index] = newSpec },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = custom.label,
                        onValueChange = { update(custom.copy(label = it)) },
                        label = { Text(stringResource(R.string.specs_custom_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = custom.value,
                        onValueChange = { update(custom.copy(value = it)) },
                        label = { Text(stringResource(R.string.specs_custom_value)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        draft = draft.copy(
                            customSpecs = draft.customSpecs.toMutableList().also { it.removeAt(index) },
                        )
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.specs_remove_custom))
                    }
                }
            }
            OutlinedButton(
                onClick = { draft = draft.copy(customSpecs = draft.customSpecs + CustomSpec()) },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.specs_add_custom), modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.specs_cancel)) }
                Button(onClick = { onSave(draft) }) { Text(stringResource(R.string.specs_save)) }
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
