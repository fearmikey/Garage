package com.fearmikey.garage.ui.obd

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.obd.ObdAdapterError
import com.fearmikey.garage.obd.ObdPid

/*
 * Display names for OBD data. The obd package keeps stable English names (they're unit tested
 * and written into timeline summaries); these map them to translated strings for the UI.
 */

@get:StringRes
val ObdPid.labelRes: Int
    get() = when (this) {
        ObdPid.ENGINE_LOAD -> R.string.obd_pid_engine_load
        ObdPid.COOLANT_TEMP -> R.string.obd_pid_coolant_temp
        ObdPid.SHORT_FUEL_TRIM_1 -> R.string.obd_pid_short_fuel_trim
        ObdPid.LONG_FUEL_TRIM_1 -> R.string.obd_pid_long_fuel_trim
        ObdPid.INTAKE_PRESSURE -> R.string.obd_pid_intake_pressure
        ObdPid.RPM -> R.string.obd_pid_rpm
        ObdPid.SPEED -> R.string.obd_pid_speed
        ObdPid.INTAKE_AIR_TEMP -> R.string.obd_pid_intake_air_temp
        ObdPid.MAF -> R.string.obd_pid_maf
        ObdPid.THROTTLE -> R.string.obd_pid_throttle
        ObdPid.RUN_TIME -> R.string.obd_pid_run_time
        ObdPid.FUEL_RAIL_PRESSURE -> R.string.obd_pid_fuel_rail_pressure
        ObdPid.FUEL_LEVEL -> R.string.obd_pid_fuel_level
        ObdPid.BAROMETRIC_PRESSURE -> R.string.obd_pid_barometric_pressure
        ObdPid.CONTROL_MODULE_VOLTAGE -> R.string.obd_pid_module_voltage
        ObdPid.AMBIENT_AIR_TEMP -> R.string.obd_pid_ambient_air_temp
        ObdPid.HYBRID_BATTERY_SOC -> R.string.obd_pid_hybrid_battery
        ObdPid.OIL_TEMP -> R.string.obd_pid_oil_temp
    }

/** Translation for a readiness monitor name from [com.fearmikey.garage.obd.ObdResponseParser.parseMonitorStatus]. */
@StringRes
fun monitorNameRes(name: String): Int? = when (name) {
    "Misfire" -> R.string.obd_monitor_misfire
    "Fuel System" -> R.string.obd_monitor_fuel_system
    "Components" -> R.string.obd_monitor_components
    "NMHC Catalyst" -> R.string.obd_monitor_nmhc_catalyst
    "NOx/SCR Aftertreatment" -> R.string.obd_monitor_nox_scr
    "Boost Pressure" -> R.string.obd_monitor_boost_pressure
    "Exhaust Gas Sensor" -> R.string.obd_monitor_exhaust_gas_sensor
    "PM Filter" -> R.string.obd_monitor_pm_filter
    "EGR/VVT System" -> R.string.obd_monitor_egr_vvt
    "Catalyst" -> R.string.obd_monitor_catalyst
    "Heated Catalyst" -> R.string.obd_monitor_heated_catalyst
    "Evaporative System" -> R.string.obd_monitor_evap
    "Secondary Air" -> R.string.obd_monitor_secondary_air
    "A/C Refrigerant" -> R.string.obd_monitor_ac_refrigerant
    "Oxygen Sensor" -> R.string.obd_monitor_o2_sensor
    "Oxygen Sensor Heater" -> R.string.obd_monitor_o2_heater
    else -> null
}

@Composable
fun monitorDisplayName(name: String): String = monitorNameRes(name)?.let { stringResource(it) } ?: name

private val MODULE_NUMBER = Regex("^Module (.+)$")

/** Translation for a module name from [com.fearmikey.garage.obd.ObdResponseParser.moduleName]. */
@Composable
fun moduleDisplayName(name: String): String = when {
    name == "Engine (ECM)" -> stringResource(R.string.obd_module_engine)
    name == "Transmission (TCM)" -> stringResource(R.string.obd_module_transmission)
    else -> MODULE_NUMBER.matchEntire(name)?.let { stringResource(R.string.obd_module_numbered, it.groupValues[1]) } ?: name
}

@get:StringRes
val ObdAdapterError.messageRes: Int
    get() = when (this) {
        ObdAdapterError.UNABLE_TO_CONNECT -> R.string.obd_error_unable_to_connect
        ObdAdapterError.CAN_BUS -> R.string.obd_error_can_bus
        ObdAdapterError.VEHICLE_BUS -> R.string.obd_error_vehicle_bus
        ObdAdapterError.BUS_FEEDBACK -> R.string.obd_error_bus_feedback
        ObdAdapterError.LOW_VOLTAGE -> R.string.obd_error_low_voltage
        ObdAdapterError.BUS_INIT -> R.string.obd_error_bus_init
        ObdAdapterError.INTERNAL -> R.string.obd_error_internal
    }
