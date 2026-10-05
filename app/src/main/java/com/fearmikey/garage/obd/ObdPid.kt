package com.fearmikey.garage.obd

import com.fearmikey.garage.ui.util.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

enum class ObdUnit { PERCENT, PERCENT_SIGNED, TEMPERATURE_C, RPM, SPEED_KMH, PRESSURE_KPA, VOLTAGE, GRAMS_PER_SEC, SECONDS }

/**
 * Standard SAE J1979 Mode 01 PIDs that Garage knows how to decode.
 *
 * @param bytes number of data bytes the reply must contain.
 */
enum class ObdPid(
    val pid: Int,
    val label: String,
    val bytes: Int,
    val unit: ObdUnit,
    val decode: (IntArray) -> Double,
) {
    ENGINE_LOAD(0x04, "Engine Load", 1, ObdUnit.PERCENT, { it[0] * 100.0 / 255 }),
    COOLANT_TEMP(0x05, "Coolant Temp", 1, ObdUnit.TEMPERATURE_C, { it[0] - 40.0 }),
    SHORT_FUEL_TRIM_1(0x06, "Short Term Fuel Trim (B1)", 1, ObdUnit.PERCENT_SIGNED, { (it[0] - 128) * 100.0 / 128 }),
    LONG_FUEL_TRIM_1(0x07, "Long Term Fuel Trim (B1)", 1, ObdUnit.PERCENT_SIGNED, { (it[0] - 128) * 100.0 / 128 }),
    INTAKE_PRESSURE(0x0B, "Intake Manifold Pressure", 1, ObdUnit.PRESSURE_KPA, { it[0].toDouble() }),
    RPM(0x0C, "Engine RPM", 2, ObdUnit.RPM, { (it[0] * 256 + it[1]) / 4.0 }),
    SPEED(0x0D, "Vehicle Speed", 1, ObdUnit.SPEED_KMH, { it[0].toDouble() }),
    INTAKE_AIR_TEMP(0x0F, "Intake Air Temp", 1, ObdUnit.TEMPERATURE_C, { it[0] - 40.0 }),
    MAF(0x10, "Mass Air Flow", 2, ObdUnit.GRAMS_PER_SEC, { (it[0] * 256 + it[1]) / 100.0 }),
    THROTTLE(0x11, "Throttle Position", 1, ObdUnit.PERCENT, { it[0] * 100.0 / 255 }),
    RUN_TIME(0x1F, "Run Time Since Start", 2, ObdUnit.SECONDS, { (it[0] * 256 + it[1]).toDouble() }),
    FUEL_RAIL_PRESSURE(0x23, "Fuel Rail Pressure", 2, ObdUnit.PRESSURE_KPA, { (it[0] * 256 + it[1]) * 10.0 }),
    FUEL_LEVEL(0x2F, "Fuel Level", 1, ObdUnit.PERCENT, { it[0] * 100.0 / 255 }),
    BAROMETRIC_PRESSURE(0x33, "Barometric Pressure", 1, ObdUnit.PRESSURE_KPA, { it[0].toDouble() }),
    CONTROL_MODULE_VOLTAGE(0x42, "Battery / Module Voltage", 2, ObdUnit.VOLTAGE, { (it[0] * 256 + it[1]) / 1000.0 }),
    AMBIENT_AIR_TEMP(0x46, "Ambient Air Temp", 1, ObdUnit.TEMPERATURE_C, { it[0] - 40.0 }),
    HYBRID_BATTERY_SOC(0x5B, "Hybrid Battery Charge", 1, ObdUnit.PERCENT, { it[0] * 100.0 / 255 }),
    OIL_TEMP(0x5C, "Engine Oil Temp", 1, ObdUnit.TEMPERATURE_C, { it[0] - 40.0 });

    companion object {
        /** Read once per scan when the vehicle reports support for them. */
        val SNAPSHOT: List<ObdPid> = listOf(
            COOLANT_TEMP, OIL_TEMP, RPM, FUEL_LEVEL, CONTROL_MODULE_VOLTAGE, ENGINE_LOAD,
            INTAKE_AIR_TEMP, AMBIENT_AIR_TEMP, BAROMETRIC_PRESSURE, FUEL_RAIL_PRESSURE,
            HYBRID_BATTERY_SOC, RUN_TIME,
        )

        /** Polled repeatedly while live data is on; kept short so refreshes stay quick. */
        val LIVE: List<ObdPid> = listOf(
            RPM, SPEED, COOLANT_TEMP, ENGINE_LOAD, THROTTLE, CONTROL_MODULE_VOLTAGE, MAF, INTAKE_PRESSURE,
        )

        /** Captured in freeze frame 0 when a code was set; the most useful values for diagnosis. */
        val FREEZE_FRAME: List<ObdPid> = listOf(
            ENGINE_LOAD, COOLANT_TEMP, SHORT_FUEL_TRIM_1, LONG_FUEL_TRIM_1, INTAKE_PRESSURE, RPM, SPEED,
            INTAKE_AIR_TEMP, THROTTLE,
        )

        /** Used when the vehicle didn't answer the "supported PIDs" query. */
        val FALLBACK: Set<ObdPid> = setOf(COOLANT_TEMP, RPM, SPEED, FUEL_LEVEL, ENGINE_LOAD, THROTTLE)
    }
}

data class ObdReading(val pid: ObdPid, val value: Double)

/** Formats OBD values (always reported in metric) for the user's preferred [UnitSystem]. */
object ObdFormatter {

    fun format(reading: ObdReading, unitSystem: UnitSystem): String =
        format(reading.pid.unit, reading.value, unitSystem)

    fun format(unit: ObdUnit, value: Double, unitSystem: UnitSystem): String = when (unit) {
        ObdUnit.PERCENT -> "${value.roundToInt()}%"
        ObdUnit.PERCENT_SIGNED -> String.format(Locale.getDefault(), "%+.1f%%", value)
        ObdUnit.RPM -> "${value.roundToInt()} rpm"
        ObdUnit.TEMPERATURE_C -> temperature(value, unitSystem)
        ObdUnit.SPEED_KMH -> if (unitSystem == UnitSystem.IMPERIAL) {
            "${(value / 1.609344).roundToInt()} mph"
        } else {
            "${value.roundToInt()} km/h"
        }
        ObdUnit.PRESSURE_KPA -> if (unitSystem == UnitSystem.IMPERIAL) {
            String.format(Locale.getDefault(), "%.1f psi", value * 0.1450377)
        } else {
            "${value.roundToInt()} kPa"
        }
        ObdUnit.VOLTAGE -> String.format(Locale.getDefault(), "%.1f V", value)
        ObdUnit.GRAMS_PER_SEC -> String.format(Locale.getDefault(), "%.1f g/s", value)
        ObdUnit.SECONDS -> {
            val total = value.roundToInt()
            "%d:%02d".format(total / 60, total % 60)
        }
    }

    fun temperature(celsius: Double, unitSystem: UnitSystem): String =
        if (unitSystem == UnitSystem.IMPERIAL) {
            "${(celsius * 9 / 5 + 32).roundToInt()}°F"
        } else {
            "${celsius.roundToInt()}°C"
        }
}
