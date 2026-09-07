package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An upcoming maintenance task for a vehicle, due by date and/or mileage.
 *
 * At least one of [dueDate] / [dueMileage] should be set (enforced in the UI
 * layer, not the database, to keep the entity simple).
 */
@Entity(
    tableName = "reminders",
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
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val taskName: String,
    val dueDate: Long? = null,
    val dueMileage: Int? = null,
    val isCompleted: Boolean = false,
)
