package com.fearmikey.garage.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fearmikey.garage.MainViewModel
import com.fearmikey.garage.PendingDeepLink
import com.fearmikey.garage.ui.components.UpdateOdometerDialog
import com.fearmikey.garage.ui.dashboard.DashboardScreen
import com.fearmikey.garage.ui.maintenance.export.MaintenanceExportScreen
import com.fearmikey.garage.ui.settings.SettingsScreen
import com.fearmikey.garage.ui.startup.StartupScreen
import com.fearmikey.garage.ui.vehicle.AddEditVehicleScreen
import com.fearmikey.garage.ui.vehicle.EditPartsScreen
import com.fearmikey.garage.ui.vehicle.VehicleDetailScreen
import com.fearmikey.garage.ui.obd.ObdScannerScreen
import com.fearmikey.garage.widget.WidgetRefresher

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GarageNavHost(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel(),
    pendingDeepLink: PendingDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {},
    isOnboardingCompleted: Boolean? = true,
) {
    if (isOnboardingCompleted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val allVehicles by mainViewModel.allVehicles.collectAsStateWithLifecycle()
    val unitSystem by mainViewModel.unitSystem.collectAsStateWithLifecycle()

    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let {
            if (!it.openOdometerDialog) {
                if (it.openDriversLicenseTab) {
                    navController.navigate(Destinations.dashboardRoute(tab = 1)) {
                        popUpTo(Destinations.DASHBOARD) { inclusive = true }
                    }
                } else {
                    navController.navigate(Destinations.vehicleDetailRoute(it.vehicleId, it.tab, it.openAdd))
                }
                onDeepLinkHandled()
            }
        }
    }

    if (pendingDeepLink?.openOdometerDialog == true) {
        UpdateOdometerDialog(
            vehicles = allVehicles,
            initialVehicleId = pendingDeepLink.vehicleId,
            getLatestMileageFlow = { vehicleId -> mainViewModel.getLatestMileageForVehicle(vehicleId) },
            unitSystem = unitSystem,
            onDismiss = onDeepLinkHandled,
            onSave = { vehicleId, canonicalMileage, date, notes ->
                mainViewModel.insertOdometerRecord(vehicleId, canonicalMileage, date, notes, context)
                onDeepLinkHandled()
            },
        )
    }

    val startDestination = if (isOnboardingCompleted) Destinations.DASHBOARD else Destinations.STARTUP

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)) },
            popExitTransition = { fadeOut(tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300)) },
        ) {
            composable(Destinations.STARTUP) {
                StartupScreen(
                    onStartupFinished = {
                        navController.navigate(Destinations.DASHBOARD) {
                            popUpTo(Destinations.STARTUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Destinations.DASHBOARD,
                arguments = Destinations.dashboardArgs,
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getInt(Destinations.DASHBOARD_TAB_ARG) ?: 0
                DashboardScreen(
                    initialTab = initialTab,
                    onAddVehicle = { navController.navigate(Destinations.addVehicleRoute()) },
                    onOpenVehicle = { vehicleId, tab -> navController.navigate(Destinations.vehicleDetailRoute(vehicleId, tab)) },
                    onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                    onUpdateMileage = { vehicleId -> mainViewModel.handleDeepLinkIntent(com.fearmikey.garage.MainActivity.ACTION_UPDATE_ODOMETER, vehicleId) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                )
            }
            composable(
                route = Destinations.ADD_EDIT_VEHICLE_ROUTE,
                arguments = Destinations.addEditVehicleArgs,
            ) {
                AddEditVehicleScreen(
                    onDone = { navController.popBackStack(Destinations.DASHBOARD, inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destinations.VEHICLE_DETAIL_ROUTE,
                arguments = Destinations.vehicleDetailArgs,
            ) {
                VehicleDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEditVehicle = { vehicleId -> navController.navigate(Destinations.editVehicleRoute(vehicleId)) },
                    onEditParts = { vehicleId -> navController.navigate(Destinations.editPartsRoute(vehicleId)) },
                    onExportMaintenance = { vehicleId -> navController.navigate(Destinations.exportMaintenanceRoute(vehicleId)) },
                    onOpenObd = { vehicleId -> navController.navigate("obd_scanner/$vehicleId") },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                )
            }
            composable(
                route = Destinations.EXPORT_MAINTENANCE_ROUTE,
                arguments = Destinations.exportMaintenanceArgs,
            ) {
                MaintenanceExportScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destinations.EDIT_PARTS_ROUTE,
                arguments = Destinations.editPartsArgs,
            ) {
                EditPartsScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "obd_scanner/{vehicleId}",
                arguments = Destinations.vehicleDetailArgs
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
                val unitSystem by mainViewModel.unitSystem.collectAsStateWithLifecycle()
                val context = LocalContext.current
                
                ObdScannerScreen(
                    vehicleId = vehicleId,
                    onNavigateBack = { navController.popBackStack() },
                    unitSystem = unitSystem,
                    onTimelineUpdated = { WidgetRefresher.refresh(context) },
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStartup = { navController.navigate(Destinations.STARTUP) },
                )
            }
        }
    }
}
