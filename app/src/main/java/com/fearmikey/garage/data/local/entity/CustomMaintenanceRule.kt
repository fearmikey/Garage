package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined recurring maintenance rule scoped to a single vehicle, for
 * things the built-in [com.fearmikey.garage.data.schedule.MaintenanceScheduleRules]
 * don't cover (custom intervals for modified vehicles, track days, non-standard
 * equipment, etc.).
 *
 * At least one of [intervalMiles] / [intervalMonths] should be set (enforced in
 * the UI layer). [MaintenanceScheduleEngine] treats these the same as built-in
 * rules once loaded, always preferring a custom rule over a built-in one that
 * shares the same [taskName].
 */
@Entity(
    tableName = "custom_maintenance_rules",
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
data class CustomMaintenanceRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val taskName: String,
    val category: MaintenanceCategory,
    val intervalMiles: Int? = null,
    val intervalMonths: Int? = null,
    val notes: String? = null,
)
