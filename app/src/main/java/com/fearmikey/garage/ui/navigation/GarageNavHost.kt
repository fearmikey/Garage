package com.fearmikey.garage.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fearmikey.garage.ui.dashboard.DashboardScreen
import com.fearmikey.garage.ui.settings.SettingsScreen
import com.fearmikey.garage.ui.vehicle.AddEditVehicleScreen
import com.fearmikey.garage.ui.vehicle.VehicleDetailScreen
import com.fearmikey.garage.ui.vehicle.scan.VinScannerScreen

@Composable
fun GarageNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.DASHBOARD) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                onAddVehicle = { navController.navigate(Destinations.addVehicleRoute()) },
                onOpenVehicle = { vehicleId -> navController.navigate(Destinations.vehicleDetailRoute(vehicleId)) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
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
                onDone = { navController.popBackStack() },
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
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
