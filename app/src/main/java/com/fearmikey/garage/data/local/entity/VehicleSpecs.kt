package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Extended vehicle specs decoded from NHTSA's vPIC VIN decode API. Kept as a
 * separate 1:1 table (rather than more columns on [Vehicle]) since these
 * fields are decode-only "reference" data the user never edits directly, and
 * not every vehicle will have one (e.g. added manually without a VIN decode).
 *
 * All fields are raw text as returned by NHTSA and displayed as-is; see
 * `VehicleRepository.cleanSpec` for how placeholder values like
 * "Not Applicable" are filtered out before being stored here.
 */
@Entity(
    tableName = "vehicle_specs",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VehicleSpecs(
    @PrimaryKey val vehicleId: Long,
    val engineCylinders: String? = null,
    val displacementL: String? = null,
    val engineHp: String? = null,
    val fuelType: String? = null,
    val transmissionStyle: String? = null,
    val transmissionSpeeds: String? = null,
    val bodyClass: String? = null,
    val doors: String? = null,
    val gvwr: String? = null,
    val series: String? = null,
    val vehicleType: String? = null,
    val plantCity: String? = null,
    val plantState: String? = null,
    val plantCountry: String? = null,
    val manufacturer: String? = null,
) {
    /** True when every field is null/blank, i.e. nothing worth showing on the Specs tab. */
    fun isEmpty(): Boolean = listOf(
        engineCylinders, displacementL, engineHp, fuelType, transmissionStyle, transmissionSpeeds,
        bodyClass, doors, gvwr, series, vehicleType, plantCity, plantState, plantCountry, manufacturer,
    ).all { it.isNullOrBlank() }
}
