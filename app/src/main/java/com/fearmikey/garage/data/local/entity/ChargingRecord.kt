package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChargerSpeed(val displayName: String) {
    LEVEL_1("Level 1 (120V)"),
    LEVEL_2("Level 2 (240V)"),
    DC_FAST("DC Fast Charging"),
}

enum class ChargerVendorType(val displayName: String) {
    HOME("Home"),
    PUBLIC("Public"),
}

/**
 * A single charging session for an Electric Vehicle (EV) or Plug-in Hybrid (PHEV).
 *
 * [date] is stored as epoch millis (UTC), same convention as [FuelRecord.date].
 */
@Entity(
    tableName = "charging_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId")],
)
data class ChargingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val mileage: Int,
    val kwhAdded: Double,
    val chargerSpeed: ChargerSpeed = ChargerSpeed.LEVEL_2,
    val batteryPercentStart: Int = 20,
    val batteryPercentEnd: Int = 80,
    val totalCost: Double = 0.0,
    val vendor: String = "",
    val vendorType: ChargerVendorType = ChargerVendorType.HOME,
    /**
     * Estimated battery range at 100% State of Charge (SoC) in canonical distance units (miles).
     * Used by Battery Health Tracker to plot range degradation over time.
     */
    val estimatedRangeAt100: Int? = null,
)
