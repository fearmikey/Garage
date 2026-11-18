package com.fearmikey.garage.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R

/**
 * Translation for a built-in maintenance task name. Task names are stored in English (they key
 * schedules, ignored rules and LubeLogger sync), so they are only translated for display.
 * Returns null for custom/unknown names, which are shown as typed.
 */
@StringRes
fun taskNameRes(taskName: String?): Int? = when (taskName?.trim()) {
    "Engine oil change" -> R.string.task_engine_oil_change
    "Power steering fluid" -> R.string.task_power_steering_fluid
    "Tire Replacement" -> R.string.task_tire_replacement
    "Rotation and Balance" -> R.string.task_rotation_and_balance
    "Brake fluid flush" -> R.string.task_brake_fluid_flush
    "Cabin air filter replacement" -> R.string.task_cabin_air_filter
    "Engine air filter replacement" -> R.string.task_engine_air_filter
    "Automatic transmission fluid service" -> R.string.task_transmission_fluid
    "Coolant flush" -> R.string.task_coolant_flush
    "Spark plug replacement" -> R.string.task_spark_plug_replacement
    "Battery load test / replacement" -> R.string.task_battery_test
    "Rust prevention" -> R.string.task_rust_prevention
    "Paint protection" -> R.string.task_paint_protection
    "Serpentine belt replacement" -> R.string.task_serpentine_belt
    "Wiper blade replacement" -> R.string.task_wiper_blades
    "Brake pad replacement" -> R.string.task_brake_pads
    "Transfer case fluid change" -> R.string.task_transfer_case_fluid
    "Front differential fluid change" -> R.string.task_front_diff_fluid
    "Rear differential fluid change" -> R.string.task_rear_diff_fluid
    "Drive chain clean & lube" -> R.string.task_chain_lube
    "Chain & sprocket replacement" -> R.string.task_chain_sprocket
    "Valve clearance check" -> R.string.task_valve_clearance
    "Fork oil change" -> R.string.task_fork_oil
    "Clutch & throttle cable adjustment" -> R.string.task_clutch_throttle_cable
    // Preset schedule templates (MaintenanceScheduleTemplates)
    "Motorcycle Oil & Filter" -> R.string.tpl_task_motorcycle_oil
    "Chain Clean, Lube & Slack Check" -> R.string.tpl_task_chain_clean
    "Tire Pressure & Tread Inspection" -> R.string.tpl_task_tire_pressure_tread
    "Brake Fluid Flush (Front & Rear)" -> R.string.tpl_task_brake_fluid_front_rear
    "Fork Oil & Seal Service" -> R.string.tpl_task_fork_seal
    "Battery Tender & Terminal Check" -> R.string.tpl_task_battery_tender
    "Severe Duty Engine Oil & Filter" -> R.string.tpl_task_severe_oil
    "Transfer Case Fluid (Severe)" -> R.string.tpl_task_transfer_case_severe
    "Front & Rear Differential Fluid" -> R.string.tpl_task_diff_fluid
    "Chassis Greasing & Ball Joints" -> R.string.tpl_task_chassis_grease
    "Heavy Duty Brake & Suspension Inspection" -> R.string.tpl_task_hd_brake_suspension
    "Full Synthetic Oil & Filter" -> R.string.tpl_task_synthetic_oil
    "Tire Rotation & Pressure Check" -> R.string.tpl_task_rotation_pressure
    "Cabin & Engine Air Filters" -> R.string.tpl_task_air_filters
    "Wiper Blade Replacement" -> R.string.task_wiper_blades
    "Serpentine Belt Replacement" -> R.string.task_serpentine_belt
    "Multi-Point Inspection" -> R.string.tpl_task_multipoint
    "Cabin Air Filter & HV Battery Duct Check" -> R.string.tpl_task_hv_duct
    "Brake Slide Lubrication & Fluid Check" -> R.string.tpl_task_brake_slides
    "Tire Rotation & Alignment Inspection" -> R.string.tpl_task_rotation_alignment
    "Inverter & Battery Coolant Flush" -> R.string.tpl_task_inverter_coolant
    "High-Zinc / High-Mileage Oil Change" -> R.string.tpl_task_high_zinc_oil
    "Cooling System & Hose Inspection" -> R.string.tpl_task_cooling_hoses
    "Ignition, Spark Plugs & Distributer" -> R.string.tpl_task_ignition
    "V-Belts & Tensioner Check" -> R.string.tpl_task_v_belts
    "Chassis & Steering Greasing" -> R.string.tpl_task_steering_grease
    else -> null
}

@Composable
fun formatTaskName(taskName: String?): String {
    if (taskName.isNullOrBlank()) return ""
    return taskNameRes(taskName)?.let { stringResource(it) } ?: taskName
}

/** Non-composable variant of [formatTaskName] for notifications, widgets and workers. */
fun Context.localizedTaskName(taskName: String?): String {
    if (taskName.isNullOrBlank()) return ""
    return taskNameRes(taskName)?.let(::getString) ?: taskName
}
