package com.fearmikey.garage.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/** Route constants + helpers for the app's single-activity Nav Host. */
object Destinations {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SCAN_VIN = "vehicle/scan-vin"

    /** Key used to return a scanned VIN from [SCAN_VIN] to the previous back stack entry. */
    const val SCANNED_VIN_RESULT = "scanned_vin"

    const val VEHICLE_ID_ARG = "vehicleId"
    const val NO_VEHICLE_ID = -1L

    const val ADD_EDIT_VEHICLE_ROUTE = "vehicle/edit?$VEHICLE_ID_ARG={$VEHICLE_ID_ARG}"
    fun addVehicleRoute() = "vehicle/edit?$VEHICLE_ID_ARG=$NO_VEHICLE_ID"
    fun editVehicleRoute(vehicleId: Long) = "vehicle/edit?$VEHICLE_ID_ARG=$vehicleId"
    val addEditVehicleArgs = listOf(
        navArgument(VEHICLE_ID_ARG) {
            type = NavType.LongType
            defaultValue = NO_VEHICLE_ID
        },
    )

    const val VEHICLE_DETAIL_ROUTE = "vehicle/{$VEHICLE_ID_ARG}"
    fun vehicleDetailRoute(vehicleId: Long) = "vehicle/$vehicleId"
    val vehicleDetailArgs = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.LongType })
}
