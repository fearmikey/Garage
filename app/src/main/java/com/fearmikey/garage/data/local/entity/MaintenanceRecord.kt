package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single completed maintenance/service event for a vehicle.
 *
 * [date] is stored as epoch millis (UTC) for easy sorting/formatting.
 * [cost] is stored in the device's local currency as a plain [Double]; this
 * is a hobbyist logging app, not an accounting tool, so we intentionally
 * avoid pulling in a currency/money library.
 */
@Entity(
    tableName = "maintenance_records",
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
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val mileage: Int,
    val description: String,
    val cost: Double,
    val category: MaintenanceCategory,
)
