package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A vehicle modification entry (e.g. lift kit, exhaust system, tint, aftermarket wheels).
 *
 * Stored with a title, category, description, installation date, optional cost,
 * and an optional photo filename saved in [com.fearmikey.garage.data.repository.ImageStorageManager].
 */
@Entity(
    tableName = "modification_records",
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
data class ModificationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val category: ModificationCategory,
    val description: String,
    val imageUri: String? = null,
    val date: Long = System.currentTimeMillis(),
    val cost: Double = 0.0,
)
