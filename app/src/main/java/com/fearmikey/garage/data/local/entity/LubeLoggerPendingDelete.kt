package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LubeLoggerRecordType {
    SERVICE,
    REPAIR,
    ODOMETER,
    FUEL,
    UPGRADE
}

@Entity(
    tableName = "lubelogger_pending_deletes",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["lubeLoggerVehicleId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lubeLoggerVehicleId")]
)
data class LubeLoggerPendingDelete(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: LubeLoggerRecordType,
    val lubeLoggerId: Int,
    val lubeLoggerVehicleId: Long
)
