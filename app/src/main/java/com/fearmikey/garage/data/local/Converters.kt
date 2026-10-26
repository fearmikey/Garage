package com.fearmikey.garage.data.local

import androidx.room.TypeConverter
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.RecallState
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.data.local.entity.VehicleType

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
    fun fromVehicleType(type: VehicleType): String = type.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType =
        VehicleType.entries.firstOrNull { it.name == value } ?: VehicleType.CAR

    @TypeConverter
    fun fromModificationCategory(category: ModificationCategory): String = category.name

    @TypeConverter
    fun toModificationCategory(value: String): ModificationCategory =
        when (value) {
            "AUDIO_ELECTRONICS", "AUDIO_ELECTRICAL" -> ModificationCategory.AUDIO_ELECTRICAL
            else -> ModificationCategory.entries.firstOrNull { it.name == value } ?: ModificationCategory.OTHER
        }

    @TypeConverter
    fun fromRecallState(state: RecallState): String = state.name

    @TypeConverter
    fun toRecallState(value: String): RecallState =
        RecallState.entries.firstOrNull { it.name == value } ?: RecallState.OPEN

    @TypeConverter
    fun fromLubeLoggerRecordType(type: LubeLoggerRecordType): String = type.name

    @TypeConverter
    fun toLubeLoggerRecordType(value: String): LubeLoggerRecordType =
        LubeLoggerRecordType.entries.firstOrNull { it.name == value } ?: LubeLoggerRecordType.SERVICE

    @TypeConverter
    fun fromTripCategory(category: TripCategory): String = category.name

    @TypeConverter
    fun toTripCategory(value: String): TripCategory =
        TripCategory.entries.firstOrNull { it.name == value } ?: TripCategory.OTHER
}
