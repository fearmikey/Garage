package com.fearmikey.garage.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/** Route constants + helpers for the app's single-activity Nav Host. */
object Destinations {
    const val STARTUP = "startup"
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

    const val VEHICLE_DETAIL_TAB_ARG = "tab"
    const val VEHICLE_DETAIL_OPEN_ADD_ARG = "openAdd"
    const val VEHICLE_DETAIL_ROUTE =
        "vehicle/{$VEHICLE_ID_ARG}?$VEHICLE_DETAIL_TAB_ARG={$VEHICLE_DETAIL_TAB_ARG}&$VEHICLE_DETAIL_OPEN_ADD_ARG={$VEHICLE_DETAIL_OPEN_ADD_ARG}"
    fun vehicleDetailRoute(vehicleId: Long, tab: Int = 0, openAdd: Boolean = false) =
        "vehicle/$vehicleId?$VEHICLE_DETAIL_TAB_ARG=$tab&$VEHICLE_DETAIL_OPEN_ADD_ARG=$openAdd"
    val vehicleDetailArgs = listOf(
        navArgument(VEHICLE_ID_ARG) { type = NavType.LongType },
        navArgument(VEHICLE_DETAIL_TAB_ARG) {
            type = NavType.IntType
            defaultValue = 0
        },
        navArgument(VEHICLE_DETAIL_OPEN_ADD_ARG) {
            type = NavType.BoolType
            defaultValue = false
        },
    )

    const val EDIT_PARTS_ROUTE = "vehicle/parts/edit?$VEHICLE_ID_ARG={$VEHICLE_ID_ARG}"
    fun editPartsRoute(vehicleId: Long) = "vehicle/parts/edit?$VEHICLE_ID_ARG=$vehicleId"
    val editPartsArgs = listOf(
        navArgument(VEHICLE_ID_ARG) { type = NavType.LongType },
    )

    const val EXPORT_MAINTENANCE_ROUTE = "vehicle/export?$VEHICLE_ID_ARG={$VEHICLE_ID_ARG}"
    fun exportMaintenanceRoute(vehicleId: Long) = "vehicle/export?$VEHICLE_ID_ARG=$vehicleId"
    val exportMaintenanceArgs = listOf(
        navArgument(VEHICLE_ID_ARG) { type = NavType.LongType },
    )
}
