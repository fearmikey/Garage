package com.fearmikey.garage.ui.vehicle

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.ui.theme.buildVehicleColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.fearmikey.garage.data.repository.ReminderStatus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.material.icons.filled.CarRepair
import com.fearmikey.garage.ui.cost.CostOfOwnershipScreen
import com.fearmikey.garage.ui.fuel.FuelScreen
import com.fearmikey.garage.ui.knowledge.VehicleKnowledgeScreen
import com.fearmikey.garage.ui.trip.TripLoggerScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceSuggestionsScreen
import com.fearmikey.garage.ui.maintenance.MaintenanceTimelineScreen
import com.fearmikey.garage.ui.mod.ModsScreen
import com.fearmikey.garage.ui.recall.RecallsScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onEditParts: (Long) -> Unit = {},
    onEditDocuments: (Long) -> Unit = {},
    onExportMaintenance: (Long) -> Unit = {},
    onOpenObd: (Long) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val imageFiles by viewModel.imageFiles.collectAsStateWithLifecycle()
    val shouldOpenAddSheet by viewModel.shouldOpenAddSheet.collectAsStateWithLifecycle()
    val vehicleThemingEnabled by viewModel.vehicleThemingEnabled.collectAsStateWithLifecycle()
    val themeType by viewModel.themeType.collectAsStateWithLifecycle()
    val amoledDark by viewModel.amoledDark.collectAsStateWithLifecycle()
    val hiddenTabs by viewModel.hiddenTabs.collectAsStateWithLifecycle()
    val tabAlerts by viewModel.tabAlerts.collectAsStateWithLifecycle()
    val tabOrder by viewModel.tabOrder.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(VehicleTab.fromOrdinal(viewModel.initialTab)) }
    var showCustomizeTabs by rememberSaveable { mutableStateOf(false) }
    val visibleTabs = remember(tabOrder, hiddenTabs, selectedTab) {
        tabOrder.filter { it !in hiddenTabs || it == selectedTab }
    }

    if (showCustomizeTabs) {
        CustomizeTabsDialog(
            tabOrder = tabOrder,
            hiddenTabs = hiddenTabs,
            onDismiss = { showCustomizeTabs = false },
            onSave = { orderedTabs, hiddenSet ->
                viewModel.setTabCustomization(orderedTabs, hiddenSet)
                showCustomizeTabs = false
            },
        )
    }

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeType) {
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    val vehicleThemeColor = vehicle?.themeColor?.let { Color(it) }
    val vehicleColorScheme = remember(vehicleThemeColor, isDark, amoledDark, vehicleThemingEnabled) {
        if (vehicleThemingEnabled && vehicleThemeColor != null) {
            buildVehicleColorScheme(
                seedColor = vehicleThemeColor,
                darkTheme = isDark,
                amoled = amoledDark,
            )
        } else {
            null
        }
    }

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val vehicleImageHeader = @Composable {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.fillMaxSize() else Modifier.aspectRatio(21f / 9f))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .sharedElement(
                        rememberSharedContentState(key = "vehicle-image-${viewModel.vehicleId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageFiles.isNotEmpty()) {
                    if (imageFiles.size == 1) {
                        val singlePhoto = imageFiles.first()
                        AsyncImage(
                            model = singlePhoto.first,
                            contentDescription = vehicle?.let { "${it.year ?: ""} ${it.make} ${it.model}".trim() },
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(0f, singlePhoto.second),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { imageFiles.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val photo = imageFiles[page]
                            AsyncImage(
                                model = photo.first,
                                contentDescription = vehicle?.let { "${it.year ?: ""} ${it.make} ${it.model}".trim() },
                                contentScale = ContentScale.Crop,
                                alignment = BiasAlignment(0f, photo.second),
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
                            repeat(imageFiles.size) { iteration ->
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
                        imageVector = if (vehicle?.isMotorcycle == true) {
                            Icons.Outlined.TwoWheeler
                        } else {
                            Icons.Outlined.DirectionsCar
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    val vehicleTabsAndContent = @Composable {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TabRowDefaults.secondaryContainerColor),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    divider = {},
                ) {
                    visibleTabs.forEach { tab ->
                        val title = stringResource(tab.titleRes)
                        val alert = tabAlerts[tab]
                        val alertDescription = when (alert) {
                            ReminderStatus.OVERDUE -> stringResource(R.string.tab_alert_overdue, title)
                            ReminderStatus.UPCOMING -> stringResource(R.string.tab_alert_upcoming, title)
                            else -> null
                        }
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            modifier = if (alertDescription != null) {
                                Modifier.semantics { contentDescription = alertDescription }
                            } else {
                                Modifier
                            },
                            text = {
                                BadgedBox(
                                    badge = {
                                        if (alert != null) {
                                            Badge(
                                                containerColor = if (alert == ReminderStatus.OVERDUE) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.tertiary
                                                },
                                                modifier = Modifier.offset(x = 6.dp, y = (-4).dp),
                                            )
                                        }
                                    },
                                ) {
                                    Text(title, style = MaterialTheme.typography.labelLarge)
                                }
                            },
                        )
                    }
                }
                IconButton(onClick = { showCustomizeTabs = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.customize_tabs))
                }
            }
            HorizontalDivider()
            when (selectedTab) {
                VehicleTab.TIMELINE -> MaintenanceTimelineScreen(
                    autoOpenAddSheet = shouldOpenAddSheet && (viewModel.initialTab == VehicleTab.TIMELINE.ordinal),
                    onAddSheetConsumed = viewModel::consumeAddSheet,
                )
                VehicleTab.SCHEDULE -> MaintenanceSuggestionsScreen()
                VehicleTab.FUEL -> FuelScreen(
                    autoOpenAddSheet = shouldOpenAddSheet && (viewModel.initialTab == VehicleTab.FUEL.ordinal),
                    onAddSheetConsumed = viewModel::consumeAddSheet,
                )
                VehicleTab.EXPENSES -> CostOfOwnershipScreen()
                VehicleTab.SPECS -> VehicleSpecsScreen()
                VehicleTab.PARTS -> PartsScreen(onEditParts = { onEditParts(viewModel.vehicleId) })
                VehicleTab.MODS -> ModsScreen()
                VehicleTab.DOCUMENTS -> RegistrationInsuranceScreen(onEditDocuments = { onEditDocuments(viewModel.vehicleId) })
                VehicleTab.RECALLS -> RecallsScreen()
                VehicleTab.KNOWLEDGE -> VehicleKnowledgeScreen()
                VehicleTab.TRIPS -> TripLoggerScreen()
            }
        }
    }

    val content = @Composable {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(vehicle?.let { "${it.year ?: ""} ${it.make} ${it.model}".trim() } ?: "Vehicle")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    vehicle?.let {
                        IconButton(onClick = { onOpenObd(it.id) }) {
                            Icon(Icons.Filled.CarRepair, contentDescription = "OBD2 Scanner")
                        }
                        IconButton(onClick = { onExportMaintenance(it.id) }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export Maintenance Log")
                        }
                        IconButton(onClick = { onEditVehicle(it.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit vehicle")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLandscape) {
            Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Box(modifier = Modifier.weight(0.4f)) {
                    vehicleImageHeader()
                }
                Box(modifier = Modifier.weight(0.6f)) {
                    vehicleTabsAndContent()
                }
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                vehicleImageHeader()
                vehicleTabsAndContent()
            }
        }
    }
    }

    if (vehicleColorScheme != null) {
        MaterialTheme(
            colorScheme = vehicleColorScheme,
            content = content,
        )
    } else {
        content()
    }
}
