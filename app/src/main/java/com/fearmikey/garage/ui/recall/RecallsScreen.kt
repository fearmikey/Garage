package com.fearmikey.garage.ui.recall

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.ui.components.EmptyState
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData

@Composable
fun RecallsScreen(
    viewModel: RecallsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RecallsContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
    ) { vin ->
        val trimmedVin = vin.trim()
        if (trimmedVin.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Vehicle VIN", trimmedVin))
            Toast.makeText(
                context,
                "VIN ($trimmedVin) copied to clipboard for easy pasting",
                Toast.LENGTH_LONG,
            ).show()
        }
        val url = "https://www.nhtsa.gov/recalls?vin=$trimmedVin"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecallsContent(
    uiState: RecallsUiState,
    onRefresh: () -> Unit,
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
                    message = "Add this vehicle's year, make and model on the Edit screen to check for recalls.",
                )
                uiState.errorMessage != null -> RecallsErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRefresh,
                )
                else -> LazyColumn(
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
                                text = "Year / Make / Model Campaigns",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (titleSuffix.isNotBlank()) {
                                Text(
                                    text = "General safety recalls applicable to $titleSuffix:",
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
                                    "No open safety recalls reported for general $vehicleLabel."
                                } else {
                                    "No open safety recalls found for this vehicle."
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
                        items(uiState.recalls, key = { it.campaignNumber }) { recall ->
                            RecallCard(recall = recall)
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
                    text = "Official VIN Recall Check",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (vin.isNotBlank()) {
                Text(
                    text = "Verify exact uncompleted recall records for VIN $vin directly on NHTSA.gov. (Your VIN is automatically copied to your clipboard so you can easily paste it on NHTSA's website).",
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
                    Text("Check VIN on NHTSA.gov")
                }
            } else {
                Text(
                    text = "No VIN saved for this vehicle. Add a VIN on the Edit screen to verify exact VIN recall status directly on NHTSA.gov.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RecallsErrorState(message: String, onRetry: () -> Unit) {
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
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun RecallCard(recall: VehicleRecall, modifier: Modifier = Modifier) {
    val isUrgent = recall.parkIt || recall.parkOutside
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Campaign #${recall.campaignNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isUrgent) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(if (recall.parkIt) "Park it" else "Park outside") },
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.error,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
            Text(recall.component, style = MaterialTheme.typography.titleMedium)
            recall.reportedDate?.let {
                Text(
                    "Reported $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recall.summary.isNotBlank()) {
                Text(recall.summary, style = MaterialTheme.typography.bodyMedium)
            }
            if (recall.consequence.isNotBlank()) {
                Text(
                    "Consequence: ${recall.consequence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recall.remedy.isNotBlank()) {
                Text(
                    "Remedy: ${recall.remedy}",
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
                recalls = SampleData.tacomaRecalls,
                vin = "5TBET30137S400000",
                year = 2021,
                make = "Toyota",
                model = "Tacoma",
            ),
            onRefresh = {},
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
        )
    }
}

private fun formatVehicleLabel(year: Int?, make: String, model: String): String {
    val yearStr = year?.toString().orEmpty()
    return sequenceOf(yearStr, make, model)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
