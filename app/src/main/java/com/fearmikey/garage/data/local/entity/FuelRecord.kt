package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single fuel fill-up for a vehicle, used to track fuel economy (MPG) and
 * fuel spending over time.
 *
 * [date] is stored as epoch millis (UTC), same convention as [MaintenanceRecord.date].
 *
 * [isFullTank] matters for fuel economy math: MPG is only meaningful between
 * two *full* tanks (see [com.fearmikey.garage.data.fuel.FuelEconomyCalculator]) --
 * a partial fill-up alone can't tell you how much fuel was already in the
 * tank when it happened. Users should mark a fill-up as partial (false) if
 * they didn't top off the tank, so the economy calculation can correctly
 * bundle its gallons into the next full-tank segment instead of treating it
 * as a standalone (and wildly inaccurate) MPG data point.
 */
@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("vehicleId")],
)
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val mileage: Double,
    val gallons: Double,
    val totalCost: Double,
    /** Derived from [totalCost] / [gallons] at save time, kept alongside so the UI never has to recompute it. */
    val pricePerGallon: Double,
    val isFullTank: Boolean = true,
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val lubeLoggerId: Int? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val lubeLoggerSyncHash: String? = null,
    /**
     * Fuel grade/type pumped: a [FuelGrade] name (e.g. "PREMIUM") for the preset grades, or the
     * user's own text (e.g. "98 RON", "E10") for anything else. Null when not recorded.
     */
    @androidx.room.ColumnInfo(defaultValue = "NULL")
    val fuelGrade: String? = null,
)

/**
 * Preset fuel grades offered when logging a fill-up. Stored in [FuelRecord.fuelGrade] by [name],
 * which must stay stable; the UI shows a localized label (see `ui/util/EnumLabels.kt`).
 */
enum class FuelGrade(val displayName: String) {
    REGULAR("Regular"),
    MIDGRADE("Mid-grade"),
    PREMIUM("Premium"),
    DIESEL("Diesel"),
    E85("E85"),
    ;

    companion object {
        /** The preset for a stored [FuelRecord.fuelGrade] value, or null for custom text / no grade. */
        fun fromStored(value: String?): FuelGrade? = entries.firstOrNull { it.name == value }
    }
}
