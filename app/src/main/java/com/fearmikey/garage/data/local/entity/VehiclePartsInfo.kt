package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * User-entered "cheat sheet" of common part numbers and specs for a vehicle,
 * meant to be glanced at while standing in an auto parts store aisle.
 *
 * Kept as a separate 1:1 table (same pattern as [VehicleSpecs]) since these
 * fields are optional and user-editable, unlike the VIN-decoded [VehicleSpecs].
 * All fields are free-form text since part numbers, gaps and PSI conventions
 * vary too much between manufacturers to model more strictly.
 */
@Entity(
    tableName = "vehicle_parts_info",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VehiclePartsInfo(
    @PrimaryKey val vehicleId: Long,
    // Oil
    val oilViscosity: String? = null,
    val oilCapacity: String? = null,
    val oilFilterPartNumber: String? = null,
    // Ignition
    val sparkPlugPartNumber: String? = null,
    val sparkPlugGap: String? = null,
    // Tires
    val tireSizeFront: String? = null,
    val tireSizeRear: String? = null,
    val tirePsiFront: String? = null,
    val tirePsiRear: String? = null,
    // Wipers
    val wiperBladeSizeDriver: String? = null,
    val wiperBladeSizePassenger: String? = null,
    val wiperBladeSizeRear: String? = null,
) {
    /** True when every field is null/blank, i.e. nothing worth showing on the Parts tab. */
    fun isEmpty(): Boolean = listOf(
        oilViscosity, oilCapacity, oilFilterPartNumber,
        sparkPlugPartNumber, sparkPlugGap,
        tireSizeFront, tireSizeRear, tirePsiFront, tirePsiRear,
        wiperBladeSizeDriver, wiperBladeSizePassenger, wiperBladeSizeRear,
    ).all { it.isNullOrBlank() }
}
