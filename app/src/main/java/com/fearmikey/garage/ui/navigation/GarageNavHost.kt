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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fearmikey.garage.PendingDeepLink
import com.fearmikey.garage.ui.dashboard.DashboardScreen
import com.fearmikey.garage.ui.maintenance.export.MaintenanceExportScreen
import com.fearmikey.garage.ui.settings.SettingsScreen
import com.fearmikey.garage.ui.startup.StartupScreen
import com.fearmikey.garage.ui.vehicle.AddEditVehicleScreen
import com.fearmikey.garage.ui.vehicle.EditPartsScreen
import com.fearmikey.garage.ui.vehicle.VehicleDetailScreen
import com.fearmikey.garage.ui.vehicle.scan.VinScannerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GarageNavHost(
    navController: NavHostController = rememberNavController(),
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

    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let {
            navController.navigate(Destinations.vehicleDetailRoute(it.vehicleId, it.tab, it.openAdd))
            onDeepLinkHandled()
        }
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
            composable(Destinations.DASHBOARD) {
                DashboardScreen(
                    onAddVehicle = { navController.navigate(Destinations.addVehicleRoute()) },
                    onOpenVehicle = { vehicleId -> navController.navigate(Destinations.vehicleDetailRoute(vehicleId)) },
                    onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                )
            }
            composable(
                route = Destinations.ADD_EDIT_VEHICLE_ROUTE,
                arguments = Destinations.addEditVehicleArgs,
            ) { backStackEntry ->
                val scannedVin = backStackEntry.savedStateHandle
                    .getStateFlow<String?>(Destinations.SCANNED_VIN_RESULT, null)
                    .collectAsStateWithLifecycle()
                AddEditVehicleScreen(
                    onDone = { navController.popBackStack(Destinations.DASHBOARD, inclusive = false) },
                    onBack = { navController.popBackStack() },
                    onScanVinClicked = { navController.navigate(Destinations.SCAN_VIN) },
                    scannedVin = scannedVin.value,
                    onScannedVinConsumed = {
                        backStackEntry.savedStateHandle[Destinations.SCANNED_VIN_RESULT] = null
                    },
                )
            }
            composable(Destinations.SCAN_VIN) {
                VinScannerScreen(
                    onVinScanned = { vin ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(Destinations.SCANNED_VIN_RESULT, vin)
                        navController.popBackStack()
                    },
                ) { navController.popBackStack() }
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
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStartup = { navController.navigate(Destinations.STARTUP) },
                )
            }
        }
    }
}
