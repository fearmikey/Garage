package com.fearmikey.garage.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A knowledge-base note. Synced with LubeLogger's vehicle notes (title <-> description,
 * content <-> note text) by LubeLoggerSyncWorker.
 */
@Entity(
    tableName = "vehicle_notes",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class VehicleNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val content: String,
    val category: String,
    val lastUpdated: Long,
    @ColumnInfo(defaultValue = "NULL")
    val lubeLoggerId: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val lubeLoggerSyncHash: String? = null,
)
