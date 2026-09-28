package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a maintenance rule/task that the user has explicitly chosen
 * to ignore for a specific vehicle, suppressing suggestions and reminders.
 */
@Entity(
    tableName = "ignored_maintenance_rules",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["vehicleId", "taskName"], unique = true)],
)
data class IgnoredMaintenanceRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val taskName: String,
)
