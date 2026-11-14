package com.fearmikey.garage.ui.recall

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fearmikey.garage.ui.components.verticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.util.UrlLauncher
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.RecallState
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.components.ScrollToTopOnInsert

@Composable
fun RecallsScreen(
    viewModel: RecallsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    RecallsContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onStateChange = viewModel::updateRecallState,
    ) { vin ->
        val trimmedVin = vin.trim()
        if (trimmedVin.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Vehicle VIN", trimmedVin))
            Toast.makeText(
                context,
                resources.getString(R.string.recalls_vin_copied, trimmedVin),
                Toast.LENGTH_LONG,
            ).show()
        }
        UrlLauncher.openUrl(context, "https://www.nhtsa.gov/recalls?vin=$trimmedVin")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecallsContent(
    uiState: RecallsUiState,
    onRefresh: () -> Unit,
    onStateChange: (String, RecallState) -> Unit,
    onOpenVinCheck: (String) -> Unit = {},
) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when {
                uiState.missingVehicleInfo -> EmptyState(
                    message = stringResource(R.string.recalls_missing_info),
                )
                uiState.errorMessage != null -> RecallsErrorState(
                    message = uiState.errorMessage,
                    isRetrying = uiState.isLoading,
                    onRetry = onRefresh,
                )
                else -> {
                    val listState = rememberLazyListState()
                    ScrollToTopOnInsert(listState, uiState.recalls.firstOrNull()?.recall?.campaignNumber)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.verticalScrollbar(listState),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    item {
                        VinRecallCard(
                            vin = uiState.vin,
                            onOpenVinCheck = onOpenVinCheck,
                        )
                    }

                    item {
                        val titleSuffix = formatVehicleLabel(uiState.year, uiState.make, uiState.model)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.recalls_ymm_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (titleSuffix.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.recalls_ymm_subtitle, titleSuffix),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (uiState.recalls.isEmpty() && !uiState.isLoading) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                val vehicleLabel = formatVehicleLabel(uiState.year, uiState.make, uiState.model)
                                val emptyMsg = if (vehicleLabel.isNotBlank()) {
                                    stringResource(R.string.recalls_none_for_model, vehicleLabel)
                                } else {
                                    stringResource(R.string.recalls_none)
                                }
                                Text(
                                    text = emptyMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        items(uiState.recalls, key = { it.recall.campaignNumber }) { recallItem ->
                            RecallCard(
                                recallItem = recallItem,
                                onStateChange = { newState ->
                                    onStateChange(recallItem.recall.campaignNumber, newState)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun VinRecallCard(
    vin: String,
    onOpenVinCheck: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.recalls_vin_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (vin.isNotBlank()) {
                Text(
                    text = stringResource(R.string.recalls_vin_desc, vin),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { onOpenVinCheck(vin) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(stringResource(R.string.recalls_nhtsa_check))
                }
            } else {
                Text(
                    text = stringResource(R.string.recalls_no_vin),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RecallsErrorState(message: String, isRetrying: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onRetry, enabled = !isRetrying) {
            Text(stringResource(R.string.recalls_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecallCard(
    recallItem: RecallItemUiState,
    onStateChange: (RecallState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recall = recallItem.recall
    val isUrgent = recall.parkIt || recall.parkOutside
    var expanded by remember { mutableStateOf(value = false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (isUrgent) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.recalls_campaign, recall.campaignNumber),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (isUrgent) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(if (recall.parkIt) R.string.recalls_park_it else R.string.recalls_park_outside)) },
                            leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledLabelColor = MaterialTheme.colorScheme.error,
                                disabledLeadingIconContentColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    val labelText = when (recallItem.state) {
                        RecallState.OPEN -> stringResource(R.string.recalls_state_open)
                        RecallState.SERVICED -> stringResource(R.string.recalls_state_serviced)
                        RecallState.DOES_NOT_AFFECT -> stringResource(R.string.recalls_state_not_affected)
                    }
                    val (chipContainerColor, chipContentColor) = when (recallItem.state) {
                        RecallState.OPEN -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        RecallState.SERVICED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        RecallState.DOES_NOT_AFFECT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    AssistChip(
                        onClick = { expanded = true },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = chipContainerColor,
                            labelColor = chipContentColor,
                            trailingIconContentColor = chipContentColor,
                        ),
                        border = AssistChipDefaults.assistChipBorder(true, borderColor = chipContainerColor),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        matchAnchorWidth = false,
                    ) {
                        RecallState.entries.forEach { state ->
                            val itemLabel = when (state) {
                                RecallState.OPEN -> stringResource(R.string.recalls_state_open)
                                RecallState.SERVICED -> stringResource(R.string.recalls_state_serviced_long)
                                RecallState.DOES_NOT_AFFECT -> stringResource(R.string.recalls_state_not_affected_long)
                            }
                            DropdownMenuItem(
                                text = { Text(itemLabel) },
                                onClick = {
                                    onStateChange(state)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            Text(recall.component, style = MaterialTheme.typography.titleMedium)
            recall.reportedDate?.let {
                Text(
                    stringResource(R.string.recalls_reported, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recall.summary.isNotBlank()) {
                Text(recall.summary, style = MaterialTheme.typography.bodyMedium)
            }
            if (recall.consequence.isNotBlank()) {
                Text(
                    stringResource(R.string.recalls_consequence, recall.consequence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recall.remedy.isNotBlank()) {
                Text(
                    stringResource(R.string.recalls_remedy, recall.remedy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecallsScreenPreview() {
    GarageTheme {
        RecallsContent(
            uiState = RecallsUiState(
                recalls = SampleData.tacomaRecalls.map { 
                    RecallItemUiState(
                        recall = it,
                        state = RecallState.OPEN,
                    )
                },
                vin = "5TBET30137S400000",
                year = 2021,
                make = "Toyota",
                model = "Tacoma",
            ),
            onRefresh = {},
            onStateChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecallsScreenEmptyPreview() {
    GarageTheme {
        RecallsContent(
            uiState = RecallsUiState(
                recalls = emptyList(),
                vin = "5TBET30137S400000",
                year = 2021,
                make = "Toyota",
                model = "Tacoma",
            ),
            onRefresh = {},
            onStateChange = { _, _ -> },
        )
    }
}

private fun formatVehicleLabel(year: Int?, make: String, model: String): String {
    val yearStr = year?.toString().orEmpty()
    return sequenceOf(yearStr, make, model)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
