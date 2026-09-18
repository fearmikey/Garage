package com.fearmikey.garage.data.local

import androidx.room.TypeConverter
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.ModificationCategory

/** Room type converters for enum columns not natively supported. */
class Converters {
    @TypeConverter
    fun fromMaintenanceCategory(category: MaintenanceCategory): String = category.name

    @TypeConverter
    fun toMaintenanceCategory(value: String): MaintenanceCategory =
        when (value) {
            "TIRE_ROTATION" -> MaintenanceCategory.TIRES
            else -> MaintenanceCategory.entries.firstOrNull { it.name == value } ?: MaintenanceCategory.OTHER
        }

    @TypeConverter
    fun fromDrivetrain(drivetrain: Drivetrain): String = drivetrain.name

    @TypeConverter
    fun toDrivetrain(value: String): Drivetrain =
        Drivetrain.entries.firstOrNull { it.name == value } ?: Drivetrain.UNKNOWN

    @TypeConverter
    fun fromModificationCategory(category: ModificationCategory): String = category.name

    @TypeConverter
    fun toModificationCategory(value: String): ModificationCategory =
        when (value) {
            "AUDIO_ELECTRONICS", "AUDIO_ELECTRICAL" -> ModificationCategory.AUDIO_ELECTRICAL
            else -> ModificationCategory.entries.firstOrNull { it.name == value } ?: ModificationCategory.OTHER
        }
}
