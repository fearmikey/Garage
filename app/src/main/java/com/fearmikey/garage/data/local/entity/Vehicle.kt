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
    @ColumnInfo(defaultValue = "0.0")
    val imageOffsetY: Float = 0f,
    val imageUri2: String? = null,
    @ColumnInfo(defaultValue = "0.0")
    val imageOffsetY2: Float = 0f,
    val imageUri3: String? = null,
    @ColumnInfo(defaultValue = "0.0")
    val imageOffsetY3: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val purchasedNew: Boolean = false,
    val initialMileage: Int? = null,
) {
    /** Helper list of all attached photos (up to 3) with their respective vertical offsets. */
    val photos: List<VehiclePhoto>
        get() = buildList {
            imageUri?.let { add(VehiclePhoto(it, imageOffsetY)) }
            imageUri2?.let { add(VehiclePhoto(it, imageOffsetY2)) }
            imageUri3?.let { add(VehiclePhoto(it, imageOffsetY3)) }
        }

    /** 
     * Evaluates whether this vehicle is a pure Battery Electric Vehicle (BEV) without an ICE engine.
     * Checks both the provided [specs] and a fallback keyword check on make/model/trim. 
     */
    fun isPureEv(specs: VehicleSpecs? = null): Boolean {
        if (specs?.isPureEv() == true) return true
        val text = "$make $model $trim".lowercase()
        val hasGas = text.contains("phev") || text.contains("hybrid") || text.contains("plug-in")
        return (text.contains("tesla") || text.contains("rivian") || text.contains("polestar") ||
            text.contains("lucid") || text.contains("ioniq") || text.contains("ev6") || text.contains("id.4") ||
            text.contains("leaf") || text.contains("bolt")) && !hasGas
    }
}

/** Represents a single photo URI (filename) and its vertical offset. */
data class VehiclePhoto(
    val uri: String,
    val offsetY: Float = 0f,
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
