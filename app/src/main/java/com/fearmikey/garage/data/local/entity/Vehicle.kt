package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single vehicle tracked by the user.
 *
 * [imageUri] intentionally stores only a *filename* (e.g. "3f1c...jpg"), not a
 * content:// or file:// URI. The actual bytes live under
 * `context.filesDir/images/<imageUri>` (see ImageStorageManager) so the app is
 * never dependent on a persistable URI permission from the Photo Picker, and
 * the image ships for free as part of the zip export/import.
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vin: String = "",
    val year: Int? = null,
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val imageUri: String? = null,
)
