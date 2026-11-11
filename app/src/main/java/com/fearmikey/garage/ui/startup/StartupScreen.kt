package com.fearmikey.garage.ui.startup

import android.Manifest
import android.app.LocaleManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import com.fearmikey.garage.ui.obd.obdBluetoothPermissions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.R
import com.fearmikey.garage.config.FlavorConfig
import com.fearmikey.garage.ui.components.CurrencySelectionDialog
import com.fearmikey.garage.ui.components.verticalScrollbar
import com.fearmikey.garage.ui.theme.AppThemePalette
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.vehicle.VehicleTab

/**
 * Guided, multi-step onboarding. Walks the user through regional preferences, appearance,
 * screen configuration (dashboard cards + default vehicle tabs), permissions and finally
 * adding their first vehicle.
 *
 * @param existingVehicleCount number of vehicles already in the garage (non-zero when the
 *   flow is re-run from Settings), used to tailor the final call to action.
 * @param onStartupFinished invoked once preferences are saved; `addVehicle` is true when the
 *   user chose to add a vehicle right away.
 */
@Composable
fun StartupScreen(
    onStartupFinished: (addVehicle: Boolean) -> Unit,
    existingVehicleCount: Int = 0,
    viewModel: StartupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // System back steps backwards through the wizard before leaving it.
    BackHandler(enabled = !uiState.currentStep.isFirst) {
        viewModel.previousStep()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshPermissionStates()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshPermissionStates()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissionStates()
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissionStates()
    }

    StartupContent(
        uiState = uiState,
        existingVehicleCount = existingVehicleCount,
        actions = StartupActions(
            onNext = viewModel::nextStep,
            onBack = viewModel::previousStep,
            onSkip = viewModel::skipToFinish,
            onGoToStep = viewModel::goToStep,
            onSelectUnits = viewModel::selectUnits,
            onSelectCurrency = viewModel::selectCurrency,
            onSelectTheme = viewModel::selectTheme,
            onSelectPalette = viewModel::selectPalette,
            onToggleDynamicColor = viewModel::toggleDynamicColor,
            onToggleAmoledDark = viewModel::toggleAmoledDark,
            onToggleAffiliateLinks = viewModel::toggleAffiliateLinks,
            onToggleFuelTrendGraph = viewModel::toggleFuelTrendGraph,
            onToggleFleetOverview = viewModel::toggleFleetOverview,
            onSetTabVisible = viewModel::setTabVisible,
            onSetDefaultHiddenTabs = viewModel::setDefaultHiddenTabs,
            onRequestNotificationPermission = {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onRequestCameraPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onRequestLocationPermission = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            },
            onRequestBluetoothPermission = {
                bluetoothPermissionLauncher.launch(obdBluetoothPermissions)
            },
            onFinish = { addVehicle ->
                viewModel.completeStartup { onStartupFinished(addVehicle) }
            },
        ),
    )
}

/** Bundles the wizard's callbacks so the stateless content stays readable and previewable. */
private data class StartupActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onGoToStep: (OnboardingStep) -> Unit = {},
    val onSelectUnits: (String) -> Unit = {},
    val onSelectCurrency: (String) -> Unit = {},
    val onSelectTheme: (String) -> Unit = {},
    val onSelectPalette: (String) -> Unit = {},
    val onToggleDynamicColor: (Boolean) -> Unit = {},
    val onToggleAmoledDark: (Boolean) -> Unit = {},
    val onToggleAffiliateLinks: (Boolean) -> Unit = {},
    val onToggleFuelTrendGraph: (Boolean) -> Unit = {},
    val onToggleFleetOverview: (Boolean) -> Unit = {},
    val onSetTabVisible: (VehicleTab, Boolean) -> Unit = { _, _ -> },
    val onSetDefaultHiddenTabs: (Set<VehicleTab>) -> Unit = {},
    val onRequestNotificationPermission: () -> Unit = {},
    val onRequestCameraPermission: () -> Unit = {},
    val onRequestLocationPermission: () -> Unit = {},
    val onRequestBluetoothPermission: () -> Unit = {},
    val onFinish: (addVehicle: Boolean) -> Unit = {},
)

@StringRes
private fun OnboardingStep.titleRes(): Int = when (this) {
    OnboardingStep.WELCOME -> R.string.onboarding_step_welcome
    OnboardingStep.REGION -> R.string.onboarding_step_region
    OnboardingStep.APPEARANCE -> R.string.onboarding_step_appearance
    OnboardingStep.SCREENS -> R.string.onboarding_step_screens
    OnboardingStep.PERMISSIONS -> R.string.onboarding_step_permissions
    OnboardingStep.FINISH -> R.string.onboarding_step_finish
}

@Composable
private fun StartupContent(
    uiState: StartupUiState,
    existingVehicleCount: Int,
    actions: StartupActions,
) {
    val step = uiState.currentStep

    Scaffold(
        topBar = {
            OnboardingTopBar(
                step = step,
                onSkip = actions.onSkip,
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                step = step,
                hasVehicles = existingVehicleCount > 0,
                onNext = actions.onNext,
                onBack = actions.onBack,
                onFinish = actions.onFinish,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) 1 else -1
                (slideInHorizontally { width -> direction * width / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -direction * width / 3 } + fadeOut())
            },
            label = "onboarding_step",
        ) { targetStep ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (targetStep) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.REGION -> RegionStep(uiState, actions)
                    OnboardingStep.APPEARANCE -> AppearanceStep(uiState, actions)
                    OnboardingStep.SCREENS -> ScreensStep(uiState, actions)
                    OnboardingStep.PERMISSIONS -> PermissionsStep(uiState, actions)
                    OnboardingStep.FINISH -> FinishStep(uiState, existingVehicleCount, actions)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// region Chrome (top/bottom bars)

@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    onSkip: () -> Unit,
) {
    val total = OnboardingStep.entries.size
    val progress by animateFloatAsState(
        targetValue = (step.ordinal + 1f) / total,
        label = "onboarding_progress",
    )
    Surface(tonalElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_step_progress, step.ordinal + 1, total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(step.titleRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!step.isLast) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    step: OnboardingStep,
    hasVehicles: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: (addVehicle: Boolean) -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                step.isLast -> {
                    Button(
                        onClick = { onFinish(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (hasVehicles) R.string.onboarding_add_another_vehicle else R.string.onboarding_add_first_vehicle,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(
                        onClick = { onFinish(false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (hasVehicles) R.string.onboarding_go_to_dashboard else R.string.onboarding_explore_first,
                            ),
                        )
                    }
                }

                step.isFirst -> {
                    PrimaryNextButton(
                        text = stringResource(R.string.onboarding_lets_go),
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.onboarding_back))
                        }
                        PrimaryNextButton(
                            text = stringResource(R.string.onboarding_next),
                            onClick = onNext,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryNextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

// endregion

// region Steps

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        HeroIcon(icon = Icons.Default.DirectionsCar, size = 96)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.startup_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeatureRow(
            icon = Icons.Default.Build,
            title = stringResource(R.string.onboarding_feature_maintenance_title),
            body = stringResource(R.string.onboarding_feature_maintenance_body),
        )
        FeatureRow(
            icon = Icons.Default.LocalGasStation,
            title = stringResource(R.string.onboarding_feature_fuel_title),
            body = stringResource(R.string.onboarding_feature_fuel_body),
        )
        FeatureRow(
            icon = Icons.Default.Event,
            title = stringResource(R.string.onboarding_feature_reminders_title),
            body = stringResource(R.string.onboarding_feature_reminders_body),
        )
        FeatureRow(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.onboarding_feature_privacy_title),
            body = stringResource(R.string.onboarding_feature_privacy_body),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionStep(
    uiState: StartupUiState,
    actions: StartupActions,
) {
    StepHeader(
        icon = Icons.Default.Language,
        title = stringResource(R.string.onboarding_region_title),
        subtitle = stringResource(R.string.onboarding_region_subtitle),
    )

    // Measurement system
    Section(
        title = stringResource(R.string.startup_measurement_system_title),
        subtitle = stringResource(R.string.startup_measurement_system_subtitle),
    ) {
        UnitOptionCard(
            title = stringResource(R.string.startup_imperial_title),
            subtitle = stringResource(R.string.startup_imperial_subtitle),
            isSelected = uiState.selectedUnits == "imperial",
            onClick = { actions.onSelectUnits("imperial") },
        )
        UnitOptionCard(
            title = stringResource(R.string.startup_metric_title),
            subtitle = stringResource(R.string.startup_metric_subtitle),
            isSelected = uiState.selectedUnits == "metric",
            onClick = { actions.onSelectUnits("metric") },
        )
    }

    HorizontalDivider()

    // Language
    Section(
        title = stringResource(R.string.language_selection),
        subtitle = stringResource(R.string.startup_language_subtitle),
    ) {
        val context = LocalContext.current
        val localeManager = remember {
            try {
                context.getSystemService(LocaleManager::class.java)
            } catch (_: Throwable) {
                null
            }
        }
        val currentLocales = localeManager?.applicationLocales
        val currentLang = if (currentLocales == null || currentLocales.isEmpty) "" else currentLocales.get(0)?.language ?: ""

        val languageOptions = listOf(
            "" to stringResource(R.string.language_system_default),
            "en" to stringResource(R.string.language_english),
            "fr" to stringResource(R.string.language_french),
            "de" to stringResource(R.string.language_german),
            "pl" to stringResource(R.string.language_polish),
            "es" to stringResource(R.string.language_spanish),
            "it" to stringResource(R.string.language_italian),
            "pt" to stringResource(R.string.language_portuguese),
            "nl" to stringResource(R.string.language_dutch),
            "ru" to stringResource(R.string.language_russian),
            "tr" to stringResource(R.string.language_turkish),
        )

        var languageExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = it },
        ) {
            val currentLangName = languageOptions.find { it.first == currentLang }?.second
                ?: stringResource(R.string.language_system_default)
            OutlinedTextField(
                value = currentLangName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false },
            ) {
                languageOptions.forEach { (langCode, langName) ->
                    DropdownMenuItem(
                        text = { Text(langName) },
                        onClick = {
                            try {
                                localeManager?.applicationLocales =
                                    LocaleListCompat.forLanguageTags(langCode).unwrap() as android.os.LocaleList
                            } catch (e: Exception) {
                                // Ignore in preview / unsupported
                            }
                            languageExpanded = false
                        },
                    )
                }
            }
        }
    }

    HorizontalDivider()

    // Currency
    Section(
        title = stringResource(R.string.startup_currency_title),
        subtitle = stringResource(R.string.startup_currency_subtitle),
    ) {
        val selectedCurrencyObj = AppCurrency.fromCode(uiState.selectedCurrency)
        var showCurrencyDialog by remember { mutableStateOf(false) }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCurrencyDialog = true },
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedCurrencyObj.code} (${selectedCurrencyObj.symbol})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedCurrencyObj.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showCurrencyDialog = true }) {
                    Text(stringResource(R.string.startup_change))
                }
            }
        }

        if (showCurrencyDialog) {
            CurrencySelectionDialog(
                selectedCurrencyCode = uiState.selectedCurrency,
                onCurrencySelected = actions.onSelectCurrency,
                onDismissRequest = { showCurrencyDialog = false },
            )
        }
    }
}

@Composable
private fun AppearanceStep(
    uiState: StartupUiState,
    actions: StartupActions,
) {
    StepHeader(
        icon = Icons.Default.Palette,
        title = stringResource(R.string.onboarding_appearance_title),
        subtitle = stringResource(R.string.onboarding_appearance_subtitle),
    )

    Section(title = stringResource(R.string.onboarding_theme_mode)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themeOptions().forEach { (mode, label) ->
                val isSelected = uiState.selectedTheme == mode
                SelectableTile(
                    isSelected = isSelected,
                    onClick = { actions.onSelectTheme(mode) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        SwitchCard(
            title = stringResource(R.string.onboarding_dynamic_color_title),
            subtitle = stringResource(R.string.onboarding_dynamic_color_subtitle),
            checked = uiState.selectedDynamicColor,
            onCheckedChange = actions.onToggleDynamicColor,
        )

        if (!uiState.selectedDynamicColor) {
            Text(
                text = stringResource(R.string.onboarding_palette_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemePalette.entries.chunked(2).forEach { rowPalettes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPalettes.forEach { palette ->
                            val isSelected = uiState.selectedPalette == palette.id
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { actions.onSelectPalette(palette.id) },
                                border = if (isSelected) {
                                    BorderStroke(2.dp, palette.primaryColor)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) {
                                        palette.primaryColor.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(palette.primaryColor, CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = palette.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        if (rowPalettes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SwitchCard(
            title = stringResource(R.string.onboarding_amoled_title),
            subtitle = stringResource(R.string.onboarding_amoled_subtitle),
            checked = uiState.selectedAmoledDark,
            onCheckedChange = actions.onToggleAmoledDark,
        )
    }
}

@Composable
private fun ScreensStep(
    uiState: StartupUiState,
    actions: StartupActions,
) {
    StepHeader(
        icon = Icons.Default.Dashboard,
        title = stringResource(R.string.onboarding_screens_title),
        subtitle = stringResource(R.string.onboarding_screens_subtitle),
    )

    Section(title = stringResource(R.string.onboarding_dashboard_section)) {
        SwitchCard(
            title = stringResource(R.string.onboarding_fuel_trend_title),
            subtitle = stringResource(R.string.onboarding_fuel_trend_subtitle),
            checked = uiState.showFuelTrendGraph,
            onCheckedChange = actions.onToggleFuelTrendGraph,
        )
        SwitchCard(
            title = stringResource(R.string.onboarding_fleet_overview_title),
            subtitle = stringResource(R.string.onboarding_fleet_overview_subtitle),
            checked = uiState.showFleetOverview,
            onCheckedChange = actions.onToggleFleetOverview,
        )
    }

    HorizontalDivider()

    Section(
        title = stringResource(R.string.onboarding_vehicle_tabs_section),
        subtitle = stringResource(R.string.onboarding_vehicle_tabs_subtitle),
    ) {
        val allHideable = VehicleTab.entries.filter { it.canHide }.toSet()
        val essentialsHidden = allHideable - ESSENTIAL_VEHICLE_TABS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.defaultHiddenTabs.isEmpty(),
                onClick = { actions.onSetDefaultHiddenTabs(emptySet()) },
                label = { Text(stringResource(R.string.onboarding_tabs_show_all)) },
            )
            FilterChip(
                selected = uiState.defaultHiddenTabs == essentialsHidden,
                onClick = { actions.onSetDefaultHiddenTabs(essentialsHidden) },
                label = { Text(stringResource(R.string.onboarding_tabs_essentials)) },
            )
        }

        val total = VehicleTab.entries.size
        Text(
            text = stringResource(
                R.string.onboarding_tabs_visible_count,
                total - uiState.defaultHiddenTabs.size,
                total,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                VehicleTab.entries.forEach { tab ->
                    val visible = tab !in uiState.defaultHiddenTabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = visible,
                                enabled = tab.canHide,
                                role = Role.Checkbox,
                                onValueChange = { show -> actions.onSetTabVisible(tab, show) },
                            )
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = visible, onCheckedChange = null, enabled = tab.canHide)
                        Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                            Text(stringResource(tab.titleRes), style = MaterialTheme.typography.bodyLarge)
                            if (!tab.canHide) {
                                Text(
                                    stringResource(R.string.customize_tabs_always_shown),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun PermissionsStep(
    uiState: StartupUiState,
    actions: StartupActions,
) {
    StepHeader(
        icon = Icons.Default.Security,
        title = stringResource(R.string.startup_permissions_title),
        subtitle = stringResource(R.string.startup_permissions_subtitle),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.startup_reminders_title),
            description = stringResource(R.string.startup_reminders_subtitle),
            isGranted = uiState.notificationPermissionGranted,
            onRequestPermission = actions.onRequestNotificationPermission,
        )

        if (FlavorConfig.isVinScannerSupported) {
            PermissionCard(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.startup_vin_title),
                description = stringResource(R.string.startup_vin_subtitle),
                isGranted = uiState.cameraPermissionGranted,
                onRequestPermission = actions.onRequestCameraPermission,
            )
        }

        PermissionCard(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.startup_location_title),
            description = stringResource(R.string.startup_location_subtitle),
            isGranted = uiState.locationPermissionGranted,
            onRequestPermission = actions.onRequestLocationPermission,
        )

        PermissionCard(
            icon = Icons.Default.Bluetooth,
            title = stringResource(R.string.startup_bluetooth_title),
            description = stringResource(R.string.startup_bluetooth_subtitle),
            isGranted = uiState.bluetoothPermissionGranted,
            onRequestPermission = actions.onRequestBluetoothPermission,
        )

        Text(
            text = stringResource(R.string.onboarding_permissions_optional),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FinishStep(
    uiState: StartupUiState,
    existingVehicleCount: Int,
    actions: StartupActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeroIcon(icon = Icons.Default.CheckCircle, size = 80)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_finish_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (existingVehicleCount > 0) R.string.onboarding_finish_subtitle_has_vehicles else R.string.onboarding_finish_subtitle,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    // Summary of choices, each linking back to its step.
    Section(title = stringResource(R.string.onboarding_summary_title)) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                val unitsLabel = stringResource(
                    if (uiState.selectedUnits == "imperial") R.string.startup_imperial_title else R.string.startup_metric_title,
                )
                val currency = AppCurrency.fromCode(uiState.selectedCurrency)
                val themeLabel = themeOptions().firstOrNull { it.first == uiState.selectedTheme }?.second
                    ?: uiState.selectedTheme
                val totalTabs = VehicleTab.entries.size
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_units),
                    value = unitsLabel,
                    onEdit = { actions.onGoToStep(OnboardingStep.REGION) },
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_currency),
                    value = "${currency.code} (${currency.symbol})",
                    onEdit = { actions.onGoToStep(OnboardingStep.REGION) },
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_theme),
                    value = themeLabel,
                    onEdit = { actions.onGoToStep(OnboardingStep.APPEARANCE) },
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_tabs),
                    value = stringResource(
                        R.string.onboarding_tabs_visible_count,
                        totalTabs - uiState.defaultHiddenTabs.size,
                        totalTabs,
                    ),
                    onEdit = { actions.onGoToStep(OnboardingStep.SCREENS) },
                )
            }
        }
    }

    // Quick tips
    Section(title = stringResource(R.string.onboarding_tips_title)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    R.string.onboarding_tip_vin,
                    R.string.onboarding_tip_widgets,
                    R.string.onboarding_tip_backup,
                    R.string.onboarding_tip_rerun,
                ).forEach { tipRes ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(tipRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider()

    SupportSection(
        affiliateLinksEnabled = uiState.affiliateLinksEnabled,
        onToggleAffiliateLinks = actions.onToggleAffiliateLinks,
    )
}

// endregion

// region Building blocks

@Composable
private fun themeOptions(): List<Pair<String, String>> = listOf(
    "system" to stringResource(R.string.onboarding_theme_system),
    "light" to stringResource(R.string.onboarding_theme_light),
    "dark" to stringResource(R.string.onboarding_theme_dark),
)

@Composable
private fun HeroIcon(icon: ImageVector, size: Int) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeroIcon(icon = icon, size = 56)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelectableTile(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        content()
    }
}

@Composable
private fun SwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // The whole card is toggleable; the switch is purely visual.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = onEdit) {
            Text(stringResource(R.string.onboarding_summary_edit))
        }
    }
}

@Composable
private fun SupportSection(
    affiliateLinksEnabled: Boolean,
    onToggleAffiliateLinks: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Section(
        title = stringResource(R.string.startup_support_title),
        subtitle = stringResource(R.string.startup_support_subtitle),
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.startup_buy_coffee_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "buymeacoffee.com/XimW7nXI1j",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://buymeacoffee.com/XimW7nXI1j") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(imageVector = Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.startup_buy_coffee_button))
                }
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .toggleable(value = affiliateLinksEnabled, role = Role.Checkbox, onValueChange = onToggleAffiliateLinks),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.startup_affiliate_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.startup_affiliate_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(checked = affiliateLinksEnabled, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun UnitOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val cardBorder = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isGranted) {
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.startup_permission_granted)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                } else {
                    FilledTonalButton(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.startup_grant_permission))
                    }
                }
            }
        }
    }
}

// endregion

// region Previews

@Composable
private fun StepPreview(step: OnboardingStep, existingVehicleCount: Int = 0) {
    GarageTheme {
        StartupContent(
            uiState = StartupUiState(
                currentStep = step,
                notificationPermissionGranted = false,
                cameraPermissionGranted = true,
                defaultHiddenTabs = setOf(VehicleTab.MODS, VehicleTab.TRIPS),
            ),
            existingVehicleCount = existingVehicleCount,
            actions = StartupActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingWelcomePreview() = StepPreview(OnboardingStep.WELCOME)

@Preview(showBackground = true)
@Composable
private fun OnboardingRegionPreview() = StepPreview(OnboardingStep.REGION)

@Preview(showBackground = true)
@Composable
private fun OnboardingScreensPreview() = StepPreview(OnboardingStep.SCREENS)

@Preview(showBackground = true)
@Composable
private fun OnboardingFinishPreview() = StepPreview(OnboardingStep.FINISH)

// endregion
