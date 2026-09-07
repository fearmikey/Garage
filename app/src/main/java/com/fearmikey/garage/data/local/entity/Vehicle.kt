package com.fearmikey.garage.data.local.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "UNKNOWN")
    val drivetrain: Drivetrain = Drivetrain.UNKNOWN,
)

/**
 * A vehicle's drivetrain layout. Used to match make/model-specific maintenance
 * rules that only apply to certain configurations (e.g. transfer case fluid
 * only applies to 4WD/AWD vehicles).
 */
enum class Drivetrain(val displayName: String) {
    UNKNOWN("Unknown"),
    FWD("FWD"),
    RWD("RWD"),
    AWD("AWD"),
    FOUR_WD("4WD"),
}
