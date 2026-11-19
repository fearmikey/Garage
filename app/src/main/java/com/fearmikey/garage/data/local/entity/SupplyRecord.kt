package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A part or consumable bought for (or kept in stock for) a vehicle, e.g. a box of oil filters or
 * a jug of coolant. Shown in the Supplies list of the "Parts & Supplies" tab, under the
 * [VehiclePartsInfo] quick reference, and synced two-way with LubeLogger's Supplies.
 *
 * [cost] is the total paid for [quantity] units, matching LubeLogger's supply cost.
 */
@Entity(
    tableName = "supply_records",
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
data class SupplyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val description: String,
    val partNumber: String = "",
    val supplier: String = "",
    val quantity: Double = 1.0,
    val cost: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val lubeLoggerId: Int? = null,
    val lubeLoggerSyncHash: String? = null,
)
