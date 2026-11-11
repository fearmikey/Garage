package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.theme.AppThemePalette
import com.fearmikey.garage.ui.util.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScrollbar(listState),
        ) {
            item {
                SettingsCategoryHeader(stringResource(R.string.appearance_header_theme))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme)) },
                    supportingContent = { Text(themeLabel(uiState.theme)) },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_amoled)) },
                    supportingContent = { Text(stringResource(R.string.appearance_amoled_summary)) },
                    leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.amoledDark,
                            onCheckedChange = { viewModel.setAmoledDark(it) },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_dynamic)) },
                    supportingContent = { Text(stringResource(R.string.appearance_dynamic_summary)) },
                    leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!uiState.dynamicColor) {
                item {
                    val currentPalette = AppThemePalette.fromId(uiState.appPalette)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.appearance_accent)) },
                        supportingContent = { Text(currentPalette.label()) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(currentPalette.primaryColor, CircleShape),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPaletteDialog = true },
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsCategoryHeader(stringResource(R.string.appearance_header_display))
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_tint)) },
                    supportingContent = { Text(stringResource(R.string.appearance_tint_summary)) },
                    leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.vehicleThemingEnabled,
                            onCheckedChange = { viewModel.setVehicleThemingEnabled(it) },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_fuel_graph)) },
                    supportingContent = { Text(stringResource(R.string.appearance_fuel_graph_summary)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFuelTrendGraph,
                            onCheckedChange = { viewModel.setShowFuelTrendGraph(it) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowFuelTrendGraph(!uiState.showFuelTrendGraph) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_fleet)) },
                    supportingContent = { Text(stringResource(R.string.appearance_fleet_summary)) },
                    leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.showFleetOverview,
                            onCheckedChange = { viewModel.setShowFleetOverview(it) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowFleetOverview(!uiState.showFleetOverview) },
                )
            }
        }
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf("light", "dark", "system"),
            selectedOption = uiState.theme,
            onOptionSelected = { viewModel.setTheme(it) },
            onDismissRequest = { showThemeDialog = false },
            optionLabel = { themeLabel(it) },
        )
    }

    if (showPaletteDialog) {
        PaletteSelectionDialog(
            selectedPaletteId = uiState.appPalette,
            onPaletteSelected = {
                viewModel.setAppPalette(it)
                showPaletteDialog = false
            },
            onDismissRequest = { showPaletteDialog = false },
        )
    }
}
