package com.fearmikey.garage.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.ChargerSpeed
import com.fearmikey.garage.data.local.entity.ChargerVendorType
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.FuelGrade
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.TripCategory
import com.fearmikey.garage.data.local.entity.VehicleType
import com.fearmikey.garage.ui.cost.TimeFilter
import com.fearmikey.garage.ui.maintenance.TimelineDateRange
import com.fearmikey.garage.ui.theme.AppThemePalette

/*
 * Localized labels for enums whose `displayName` is a fixed English string. `displayName` stays as the
 * stable, language-independent name (used by search, CSV export and tests); anything shown to the user
 * should go through these instead.
 */

@get:StringRes
val VehicleType.labelRes: Int
    get() = when (this) {
        VehicleType.CAR -> R.string.vehicle_type_car
        VehicleType.MOTORCYCLE -> R.string.vehicle_type_motorcycle
    }

@Composable
fun VehicleType.label(): String = stringResource(labelRes)

@get:StringRes
val FuelGrade.labelRes: Int
    get() = when (this) {
        FuelGrade.REGULAR -> R.string.fuel_grade_regular
        FuelGrade.MIDGRADE -> R.string.fuel_grade_midgrade
        FuelGrade.PREMIUM -> R.string.fuel_grade_premium
        FuelGrade.DIESEL -> R.string.fuel_grade_diesel
        FuelGrade.E85 -> R.string.fuel_grade_e85
    }

@Composable
fun FuelGrade.label(): String = stringResource(labelRes)

/** Display text for a stored [com.fearmikey.garage.data.local.entity.FuelRecord.fuelGrade] (preset label or the custom text). */
@Composable
fun fuelGradeLabel(stored: String?): String? {
    if (stored.isNullOrBlank()) return null
    return FuelGrade.fromStored(stored)?.label() ?: stored
}

/** FWD/RWD/AWD/4WD are the same abbreviations in every supported language; only "Unknown" is translated. */
@Composable
fun Drivetrain.label(): String =
    if (this == Drivetrain.UNKNOWN) stringResource(R.string.drivetrain_unknown) else displayName

@get:StringRes
val ModificationCategory.labelRes: Int
    get() = when (this) {
        ModificationCategory.PERFORMANCE -> R.string.mod_category_performance
        ModificationCategory.SUSPENSION -> R.string.mod_category_suspension
        ModificationCategory.EXTERIOR -> R.string.mod_category_exterior
        ModificationCategory.INTERIOR -> R.string.mod_category_interior
        ModificationCategory.LIGHTING -> R.string.mod_category_lighting
        ModificationCategory.WHEELS_TIRES -> R.string.mod_category_wheels_tires
        ModificationCategory.AUDIO_ELECTRICAL -> R.string.mod_category_audio_electrical
        ModificationCategory.EXHAUST -> R.string.mod_category_exhaust
        ModificationCategory.OTHER -> R.string.mod_category_other
    }

@Composable
fun ModificationCategory.label(): String = stringResource(labelRes)

@get:StringRes
val MaintenanceCategory.labelRes: Int
    get() = when (this) {
        MaintenanceCategory.FLUIDS -> R.string.maint_category_fluids
        MaintenanceCategory.FILTERS -> R.string.maint_category_filters
        MaintenanceCategory.TIRES -> R.string.maint_category_tires
        MaintenanceCategory.BRAKES -> R.string.maint_category_brakes
        MaintenanceCategory.BATTERY -> R.string.maint_category_battery
        MaintenanceCategory.ENGINE -> R.string.maint_category_engine
        MaintenanceCategory.DRIVETRAIN -> R.string.maint_category_drivetrain
        MaintenanceCategory.SUSPENSION -> R.string.maint_category_suspension
        MaintenanceCategory.ELECTRICAL -> R.string.maint_category_electrical
        MaintenanceCategory.HVAC -> R.string.maint_category_hvac
        MaintenanceCategory.BODY -> R.string.maint_category_body
        MaintenanceCategory.INSPECTION -> R.string.maint_category_inspection
        MaintenanceCategory.REPAIR -> R.string.maint_category_repair
        MaintenanceCategory.OTHER -> R.string.maint_category_other
    }

@Composable
fun MaintenanceCategory.label(): String = stringResource(labelRes)

@get:StringRes
val ChargerSpeed.labelRes: Int
    get() = when (this) {
        ChargerSpeed.LEVEL_1 -> R.string.charger_speed_level_1
        ChargerSpeed.LEVEL_2 -> R.string.charger_speed_level_2
        ChargerSpeed.DC_FAST -> R.string.charger_speed_dc_fast
    }

@Composable
fun ChargerSpeed.label(): String = stringResource(labelRes)

@get:StringRes
val ChargerVendorType.labelRes: Int
    get() = when (this) {
        ChargerVendorType.HOME -> R.string.charger_vendor_home
        ChargerVendorType.PUBLIC -> R.string.charger_vendor_public
    }

@Composable
fun ChargerVendorType.label(): String = stringResource(labelRes)

@get:StringRes
val TimelineDateRange.labelRes: Int
    get() = when (this) {
        TimelineDateRange.ALL -> R.string.date_range_all
        TimelineDateRange.LAST_30_DAYS -> R.string.date_range_last_30_days
        TimelineDateRange.LAST_12_MONTHS -> R.string.date_range_last_12_months
        TimelineDateRange.THIS_YEAR -> R.string.date_range_this_year
        TimelineDateRange.LAST_YEAR -> R.string.date_range_last_year
    }

@Composable
fun TimelineDateRange.label(): String = stringResource(labelRes)

@get:StringRes
val TimeFilter.labelRes: Int
    get() = when (this) {
        TimeFilter.ALL_TIME -> R.string.time_filter_all_time
        TimeFilter.THIS_YEAR -> R.string.time_filter_this_year
        TimeFilter.LAST_YEAR -> R.string.time_filter_last_year
        TimeFilter.PAST_12_MONTHS -> R.string.time_filter_12_months
        TimeFilter.PAST_6_MONTHS -> R.string.time_filter_6_months
    }

@Composable
fun TimeFilter.label(): String = stringResource(labelRes)

@get:StringRes
val AppThemePalette.labelRes: Int
    get() = when (this) {
        AppThemePalette.GARAGE_BLUE -> R.string.palette_garage_blue
        AppThemePalette.RACING_RED -> R.string.palette_racing_red
        AppThemePalette.RACING_GREEN -> R.string.palette_racing_green
        AppThemePalette.TUNER_AMBER -> R.string.palette_tuner_amber
        AppThemePalette.MONOCHROME -> R.string.palette_stealth_slate
        AppThemePalette.MIDNIGHT_PURPLE -> R.string.palette_midnight_purple
    }

@Composable
fun AppThemePalette.label(): String = stringResource(labelRes)

@get:StringRes
val TripCategory.labelRes: Int
    get() = when (this) {
        TripCategory.BUSINESS -> R.string.trip_cat_business
        TripCategory.PERSONAL -> R.string.trip_cat_personal
        TripCategory.COMMUTE -> R.string.trip_cat_commute
        TripCategory.MEDICAL -> R.string.trip_cat_medical
        TripCategory.CHARITY -> R.string.trip_cat_charity
        TripCategory.OTHER -> R.string.trip_cat_other
    }

@Composable
fun TripCategory.label(): String = stringResource(labelRes)
