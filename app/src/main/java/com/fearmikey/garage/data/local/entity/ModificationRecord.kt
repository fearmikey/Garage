package com.fearmikey.garage.data.local.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "")
    val productUrl: String = "",
    val imageUri2: String? = null,
    val imageUri3: String? = null,
    val imageUri4: String? = null,
    val imageUri5: String? = null,
    val imageUri6: String? = null,
) {
    /** Helper list of all attached photo filenames (up to 6). */
    val imageUris: List<String>
        get() = buildList {
            imageUri?.let { add(it) }
            imageUri2?.let { add(it) }
            imageUri3?.let { add(it) }
            imageUri4?.let { add(it) }
            imageUri5?.let { add(it) }
            imageUri6?.let { add(it) }
        }
}
