package com.fearmikey.garage.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.SampleData
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import java.io.File

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape

/** Summary card for a [Vehicle], used in the Dashboard grid/list. */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    latestMileage: Int?,
    imageFile: File? = null,
    imageFiles: List<Pair<File, Float>> = emptyList(),
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    avgMpg: Double? = null,
    overdueReminderCount: Int = 0,
    upcomingReminderCount: Int = 0,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReminders: (() -> Unit)? = null,
) {
    val effectiveImageFiles: List<Pair<File, Float>> = remember(imageFile, imageFiles, vehicle) {
        if (imageFiles.isNotEmpty()) imageFiles
        else if (imageFile != null && imageFile.exists()) listOf(Pair(imageFile, vehicle.imageOffsetY))
        else emptyList()
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(21f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .sharedElement(
                            rememberSharedContentState(key = "vehicle-image-${vehicle.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (effectiveImageFiles.isNotEmpty()) {
                        if (effectiveImageFiles.size == 1) {
                            val firstImage = effectiveImageFiles.first()
                            AsyncImage(
                                model = firstImage.first,
                                contentDescription = vehicleLabel(vehicle),
                                contentScale = ContentScale.Crop,
                                alignment = BiasAlignment(0f, firstImage.second),
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            val pagerState = rememberPagerState(pageCount = { effectiveImageFiles.size })
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                val currentImage = effectiveImageFiles[page]
                                AsyncImage(
                                    model = currentImage.first,
                                    contentDescription = vehicleLabel(vehicle),
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, currentImage.second),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(effectiveImageFiles.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(color, shape = CircleShape)
                                    )
                                }
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
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

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val mileageText = latestMileage?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "No mileage"
                    StatChip(
                        text = mileageText,
                        icon = Icons.Outlined.Speed,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                    if (avgMpg != null && avgMpg > 0.0) {
                        StatChip(
                            text = UnitConverter.formatFuelEconomy(avgMpg, unitSystem),
                            icon = Icons.Outlined.LocalGasStation,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }

                    if (overdueReminderCount > 0) {
                        StatChip(
                            text = "$overdueReminderCount Overdue",
                            icon = Icons.Outlined.WarningAmber,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = onOpenReminders,
                        )
                    } else if (upcomingReminderCount > 0) {
                        StatChip(
                            text = "$upcomingReminderCount Due Soon",
                            icon = Icons.Outlined.Notifications,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onOpenReminders,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val chipModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = chipModifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun vehicleLabel(vehicle: Vehicle): String =
    listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model)
        .joinToString(" ")
        .ifBlank { vehicle.vin.ifBlank { "Unnamed vehicle" } }

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
private fun VehicleCardPreview() {
    GarageTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = Unit, label = "VehicleCardPreview") { target ->
                if (target == Unit) {
                    VehicleCard(
                        vehicle = SampleData.tacoma,
                        latestMileage = SampleData.TACOMA_LATEST_MILEAGE,
                        imageFile = null,
                        avgMpg = 21.5,
                        overdueReminderCount = 1,
                        upcomingReminderCount = 2,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        onClick = {},
                    )
                }
            }
        }
    }
}
