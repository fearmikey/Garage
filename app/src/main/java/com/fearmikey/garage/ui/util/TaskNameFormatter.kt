package com.fearmikey.garage.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R

@Composable
fun formatTaskName(taskName: String?): String {
    if (taskName.isNullOrBlank()) return ""
    return when (taskName.trim()) {
        "Engine oil change" -> stringResource(R.string.task_engine_oil_change)
        "Power steering fluid" -> stringResource(R.string.task_power_steering_fluid)
        "Tire Replacement" -> stringResource(R.string.task_tire_replacement)
        "Rotation and Balance" -> stringResource(R.string.task_rotation_and_balance)
        "Brake fluid flush" -> stringResource(R.string.task_brake_fluid_flush)
        "Cabin air filter replacement" -> stringResource(R.string.task_cabin_air_filter)
        "Engine air filter replacement" -> stringResource(R.string.task_engine_air_filter)
        "Automatic transmission fluid service" -> stringResource(R.string.task_transmission_fluid)
        "Coolant flush" -> stringResource(R.string.task_coolant_flush)
        "Spark plug replacement" -> stringResource(R.string.task_spark_plug_replacement)
        "Battery load test / replacement" -> stringResource(R.string.task_battery_test)
        "Rust prevention" -> stringResource(R.string.task_rust_prevention)
        "Paint protection" -> stringResource(R.string.task_paint_protection)
        "Serpentine belt replacement" -> stringResource(R.string.task_serpentine_belt)
        "Wiper blade replacement" -> stringResource(R.string.task_wiper_blades)
        "Brake pad replacement" -> stringResource(R.string.task_brake_pads)
        "Transfer case fluid change" -> stringResource(R.string.task_transfer_case_fluid)
        "Front differential fluid change" -> stringResource(R.string.task_front_diff_fluid)
        "Rear differential fluid change" -> stringResource(R.string.task_rear_diff_fluid)
        else -> taskName
    }
}
