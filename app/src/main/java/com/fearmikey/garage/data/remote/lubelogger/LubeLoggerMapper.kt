package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.schedule.MaintenanceScheduleRules
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.parseToDoubleOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** taskName Garage uses for plain odometer readings (see MainViewModel.insertOdometerRecord). */
const val ODOMETER_CHECK_IN_TASK = "Odometer Check-in"
private const val ODOMETER_CHECK_IN_DESCRIPTION = "Odometer check-in"

/**
 * Odometer check-ins live in the maintenance table but sync to LubeLogger's *odometer* records,
 * whose IDs are a separate namespace from service record IDs.
 */
val MaintenanceRecord.isOdometerCheckIn: Boolean
    get() = category == MaintenanceCategory.INSPECTION && taskName == ODOMETER_CHECK_IN_TASK

internal fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

/** Parses LubeLogger integer fields that may arrive as "123", "123.0" or a JSON number. */
internal fun parseLubeLoggerInt(value: String?): Int? =
    value?.trim()?.replace(",", "")?.toDoubleOrNull()?.roundToInt()

/** Parses LubeLogger double fields (cost, gallons) from en-US invariant culture strings. */
internal fun parseLubeLoggerDouble(value: String?): Double? =
    value?.trim()?.replace(",", "")?.toDoubleOrNull()

/**
 * Booleans are sent as strings: LubeLogger's export models declare them as `string` and call
 * `bool.Parse`. Servers from Dec 2024 onwards also accept JSON `true`, older ones reject it.
 */
internal fun lubeLoggerBool(value: Boolean): String = if (value) "True" else "False"

/** Parses LubeLogger booleans, which may be "True"/"False" (default culture) or true/false. */
internal fun parseLubeLoggerBoolean(value: String?): Boolean? =
    value?.trim()?.lowercase(Locale.US)?.toBooleanStrictOrNull()

fun FuelRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerGasRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage
    val convertedGallons = if (isMetric) UnitConverter.gallonsToLiters(this.gallons) else this.gallons

    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        fuelConsumed = "%.3f".format(Locale.US, convertedGallons),
        cost = "%.2f".format(Locale.US, this.totalCost),
        isFillToFull = lubeLoggerBool(this.isFullTank),
        missedFuelUp = lubeLoggerBool(false), // We don't explicitly track missed fuel ups right now
    )
}

fun MaintenanceRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerServiceRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    val extraFields = mutableListOf<LubeLoggerExtraFieldDto>()
    extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Category", value = this.category.name))
    if (this.taskName != null) {
        extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Task", value = this.taskName))
    }
    if (this.isDeferred) {
        extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Deferred", value = "true"))
    }
    
    return LubeLoggerServiceRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.description,
        cost = "%.2f".format(Locale.US, this.cost),
        extraFields = extraFields
    )
}

fun MaintenanceRecord.toLubeLoggerRepairDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerRepairRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    val extraFields = mutableListOf<LubeLoggerExtraFieldDto>()
    extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Category", value = this.category.name))
    if (this.taskName != null) {
        extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Task", value = this.taskName))
    }
    if (this.isDeferred) {
        extraFields.add(LubeLoggerExtraFieldDto(name = "Garage Deferred", value = "true"))
    }

    return LubeLoggerRepairRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.description,
        cost = "%.2f".format(Locale.US, this.cost),
        extraFields = extraFields
    )
}

fun ModificationRecord.toLubeLoggerUpgradeDto(
    lubeLoggerVehicleId: Int,
    latestMileage: Int? = null,
    lubeLoggerUnitSystem: String = "imperial",
): LubeLoggerUpgradeRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val mileageMiles = latestMileage ?: 0
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(mileageMiles) else mileageMiles

    return LubeLoggerUpgradeRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.title + (if (this.description.isNotBlank()) " - ${this.description}" else ""),
        cost = "%.2f".format(Locale.US, this.cost),
    )
}

/**
 * Estimates the vehicle's odometer reading at [targetDate] based on known mileage readings
 * from fuel, maintenance, or charging logs. Falls back to [initialMileage] or the earliest/latest
 * known reading if no prior readings exist.
 */
fun estimateMileageAtDate(
    targetDate: Long,
    readings: List<Pair<Long, Int>>,
    initialMileage: Int? = null,
): Int {
    val beforeOrAt = readings.filter { it.first <= targetDate }
    if (beforeOrAt.isNotEmpty()) {
        return beforeOrAt.maxOf { it.second }
    }
    if (initialMileage != null && initialMileage > 0) {
        return initialMileage
    }
    if (readings.isNotEmpty()) {
        return readings.minOf { it.second }
    }
    return 0
}

/*
 * Sync fingerprints: a stable string of the normalized fields both sides store. Dates are
 * compared by day and money/volume are rounded, so unit and precision quirks can't make an
 * unchanged record look edited.
 */
fun ChargingRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerGasRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        fuelConsumed = "%.3f".format(Locale.US, this.kwhAdded),
        cost = "%.2f".format(Locale.US, this.totalCost),
        isFillToFull = lubeLoggerBool(true),
        missedFuelUp = lubeLoggerBool(false),
        startingSoc = this.batteryPercentStart.toString(),
        endingSoc = this.batteryPercentEnd.toString(),
        notes = this.vendor.ifBlank { "" },
    )
}

fun LubeLoggerGasRecordDto.toChargingRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial", existing: ChargingRecord? = null): ChargingRecord? {
    val recordId = id ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val parsedDate = parseDateToEpochMillis(date)

    val rawMileage = mileage ?: parseLubeLoggerInt(odometer) ?: 0
    val rawKwh = gallons ?: fuelConsumed?.parseToDoubleOrNull() ?: 0.0
    val parsedCost = cost?.parseToDoubleOrNull() ?: 0.0
    val startSoc = parseLubeLoggerInt(startingSoc) ?: 20
    val endSoc = parseLubeLoggerInt(endingSoc) ?: 80

    val parsedMileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage

    return ChargingRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        kwhAdded = rawKwh,
        batteryPercentStart = startSoc,
        batteryPercentEnd = endSoc,
        totalCost = parsedCost,
        vendor = notes,
        estimatedRangeAt100 = existing?.estimatedRangeAt100,
        chargerSpeed = existing?.chargerSpeed ?: com.fearmikey.garage.data.local.entity.ChargerSpeed.LEVEL_2,
        vendorType = existing?.vendorType ?: com.fearmikey.garage.data.local.entity.ChargerVendorType.HOME,
        lubeLoggerId = recordId,
    )
}

fun ChargingRecord.syncFingerprint(): String =
    "$date|$mileage|${"%.3f".format(Locale.US, kwhAdded)}|${"%.2f".format(Locale.US, totalCost)}|$batteryPercentStart|$batteryPercentEnd|${vendor.trim()}"

fun FuelRecord.syncFingerprint(): String =
    "${formatDate(date)}|$mileage|${"%.2f".format(Locale.US, gallons)}|${"%.2f".format(Locale.US, totalCost)}|$isFullTank"

fun MaintenanceRecord.syncFingerprint(): String =
    "${formatDate(date)}|$mileage|${description.trim()}|${"%.2f".format(Locale.US, cost)}|${category.name}|${taskName.orEmpty()}|$isDeferred"

fun ModificationRecord.syncFingerprint(): String =
    "${formatDate(date)}|${title.trim()}|${description.trim()}|${"%.2f".format(Locale.US, cost)}"

/**
 * Older Garage versions sent maintenance records as "Description (Task name)". Splits such a
 * description back into its parts, but only when the suffix is a known maintenance task, so
 * user text like "Brake pads (front)" is left alone. Returns null when there's nothing to split.
 */
internal fun splitLegacyTaskSuffix(description: String): Pair<String, String>? {
    val match = Regex("""^(.*\S)\s+\(([^()]+)\)(\s+\[Deferred])?$""").find(description.trim()) ?: return null
    val task = match.groupValues[2].trim()
    val knownTask = MaintenanceScheduleRules.rules.firstOrNull { it.taskName.equals(task, ignoreCase = true) }
        ?: return null
    return match.groupValues[1] to knownTask.taskName
}

private fun knownCategoryFor(taskName: String): MaintenanceCategory? =
    MaintenanceScheduleRules.rules.firstOrNull { it.taskName.equals(taskName, ignoreCase = true) }?.category

/** One-time repair of a record pulled by an older Garage version (see [splitLegacyTaskSuffix]). */
fun MaintenanceRecord.cleanLegacyTaskSuffix(): MaintenanceRecord {
    val (cleanDescription, task) = splitLegacyTaskSuffix(description) ?: return this
    return copy(
        description = cleanDescription,
        taskName = taskName ?: task,
        category = if (category == MaintenanceCategory.OTHER) knownCategoryFor(task) ?: category else category,
        isDeferred = isDeferred || description.trimEnd().endsWith("[Deferred]"),
    )
}

fun MaintenanceRecord.toLubeLoggerOdometerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerOdometerRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    return LubeLoggerOdometerRecordDto(
        id = this.lubeLoggerId?.toString(),
        vehicleId = lubeLoggerVehicleId.toString(),
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        notes = this.description.takeIf { it != ODOMETER_CHECK_IN_DESCRIPTION } ?: "",
    )
}

fun createLubeLoggerOdometerDto(
    lubeLoggerVehicleId: Int,
    dateMillis: Long,
    mileageMiles: Int,
    notes: String,
    lubeLoggerUnitSystem: String = "imperial",
): LubeLoggerOdometerRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(mileageMiles) else mileageMiles

    return LubeLoggerOdometerRecordDto(
        vehicleId = lubeLoggerVehicleId.toString(),
        date = formatDate(dateMillis),
        odometer = convertedOdometer.toString(),
        notes = notes,
    )
}

/**
 * Converts a LubeLogger odometer record into a Garage odometer check-in, or null if the record
 * is unusable (missing ID, zero odometer, or a blank `01/01/0001` row that some LubeLogger
 * databases contain).
 */
fun LubeLoggerOdometerRecordDto.toOdometerCheckIn(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial", existing: MaintenanceRecord? = null): MaintenanceRecord? {
    val recordId = parseLubeLoggerInt(id)?.takeIf { it > 0 } ?: return null
    val rawMileage = parseLubeLoggerInt(odometer)?.takeIf { it > 0 } ?: return null
    val parsedDate = parseDateToEpochMillisOrNull(date) ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val parsedMileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage

    return MaintenanceRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        description = notes?.takeIf { it.isNotBlank() } ?: ODOMETER_CHECK_IN_DESCRIPTION,
        cost = 0.0,
        category = MaintenanceCategory.INSPECTION,
        taskName = ODOMETER_CHECK_IN_TASK,
        lubeLoggerId = recordId,
        lubeLoggerRecordType = LubeLoggerRecordType.ODOMETER
    )
}

fun LubeLoggerGasRecordDto.toFuelRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial", existing: FuelRecord? = null): FuelRecord? {
    val recordId = id ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val parsedDate = parseDateToEpochMillis(date)
    
    val rawMileage = mileage ?: parseLubeLoggerInt(odometer) ?: 0
    val rawGallons = gallons ?: parseLubeLoggerDouble(fuelConsumed) ?: 0.0
    val parsedCost = parseLubeLoggerDouble(cost) ?: 0.0
    val parsedIsFillToFull = parseLubeLoggerBoolean(isFillToFull) ?: true

    val parsedMileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage
    val parsedGallons = if (isMetric) UnitConverter.litersToGallons(rawGallons) else rawGallons
    val ppg = if (parsedGallons > 0.0) parsedCost / parsedGallons else 0.0

    return FuelRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        gallons = parsedGallons,
        totalCost = parsedCost,
        pricePerGallon = ppg,
        isFullTank = parsedIsFillToFull,
        lubeLoggerId = recordId,
    )
}

fun LubeLoggerServiceRecordDto.toMaintenanceRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial", existing: MaintenanceRecord? = null): MaintenanceRecord? =
    parseMaintenanceRecord(
        recordId = id, dateStr = date, mileage = mileage, odometer = odometer, description = description,
        cost = cost, extraFields = extraFields, recordType = LubeLoggerRecordType.SERVICE,
        localVehicleId = localVehicleId, lubeLoggerUnitSystem = lubeLoggerUnitSystem, existing = existing,
    )

fun LubeLoggerRepairRecordDto.toMaintenanceRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial", existing: MaintenanceRecord? = null): MaintenanceRecord? =
    parseMaintenanceRecord(
        recordId = id, dateStr = date, mileage = mileage, odometer = odometer, description = description,
        cost = cost, extraFields = extraFields, recordType = LubeLoggerRecordType.REPAIR,
        localVehicleId = localVehicleId, lubeLoggerUnitSystem = lubeLoggerUnitSystem, existing = existing,
    )

/**
 * Shared parser for LubeLogger service and repair records. Category, task and deferral come from
 * Garage's custom fields; records created in LubeLogger without them default to Other (service)
 * or Repair (repair).
 */
private fun parseMaintenanceRecord(
    recordId: Int?,
    dateStr: String,
    mileage: Int?,
    odometer: String?,
    description: String,
    cost: String?,
    extraFields: List<LubeLoggerExtraFieldDto>?,
    recordType: LubeLoggerRecordType,
    localVehicleId: Long,
    lubeLoggerUnitSystem: String,
    existing: MaintenanceRecord?,
): MaintenanceRecord? {
    val id = recordId?.takeIf { it > 0 } ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val rawMileage = mileage ?: parseLubeLoggerInt(odometer) ?: 0
    val defaultCategory = if (recordType == LubeLoggerRecordType.REPAIR) MaintenanceCategory.REPAIR else MaintenanceCategory.OTHER

    fun field(name: String): String? = extraFields.orEmpty()
        .firstOrNull { it.name?.trim().equals(name, ignoreCase = true) }
        ?.value?.takeIf { it.isNotBlank() }

    val categoryField = field(FIELD_CATEGORY)
    var category = MaintenanceCategory.entries.firstOrNull { it.name == categoryField } ?: defaultCategory
    var task = field(FIELD_TASK)
    var isDeferred = parseLubeLoggerBoolean(field(FIELD_DEFERRED)) == true
    var cleanDescription = description

    // Records pushed by older Garage versions carry the task as a " (task)" suffix instead.
    if (task == null) {
        splitLegacyTaskSuffix(description)?.let { (desc, legacyTask) ->
            cleanDescription = desc
            task = legacyTask
            if (categoryField == null) category = knownCategoryFor(legacyTask) ?: category
            isDeferred = isDeferred || description.trimEnd().endsWith("[Deferred]")
        }
    }

    return MaintenanceRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parseDateToEpochMillis(dateStr),
        mileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage,
        description = cleanDescription,
        cost = parseLubeLoggerDouble(cost) ?: 0.0,
        category = category,
        taskName = task,
        receiptUri = existing?.receiptUri,
        isDeferred = isDeferred,
        deferredMiles = existing?.deferredMiles,
        deferredMonths = existing?.deferredMonths,
        lubeLoggerId = id,
        lubeLoggerRecordType = recordType,
    )
}

private const val FIELD_CATEGORY = "Garage Category"
private const val FIELD_TASK = "Garage Task"
private const val FIELD_DEFERRED = "Garage Deferred"

fun LubeLoggerUpgradeRecordDto.toModificationRecord(localVehicleId: Long, existing: ModificationRecord? = null): ModificationRecord? {
    val recordId = id ?: return null
    val parsedDate = parseDateToEpochMillis(date)
    val parsedCost = parseLubeLoggerDouble(cost) ?: 0.0
    
    // We split title and description by " - " if it exists, otherwise it's just title
    val parts = description.split(" - ", limit = 2)
    val title = parts.getOrNull(0) ?: "Upgrade"
    val desc = parts.getOrNull(1) ?: ""

    return ModificationRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        title = title,
        description = desc,
        category = existing?.category ?: com.fearmikey.garage.data.local.entity.ModificationCategory.OTHER,
        date = parsedDate,
        cost = parsedCost,
        lubeLoggerId = recordId,
        imageUri = existing?.imageUri,
        imageUri2 = existing?.imageUri2,
        imageUri3 = existing?.imageUri3,
        imageUri4 = existing?.imageUri4,
        imageUri5 = existing?.imageUri5,
        imageUri6 = existing?.imageUri6,
        productUrl = existing?.productUrl ?: ""
    )
}

private fun parseDateToEpochMillis(dateStr: String): Long =
    parseDateToEpochMillisOrNull(dateStr) ?: System.currentTimeMillis()

/**
 * Parses LubeLogger dates: ISO `yyyy-MM-dd` (culture-invariant mode, optionally followed by a
 * time) or `MM/dd/yyyy` (en-US default). Returns null for unparseable or placeholder dates
 * such as `0001-01-01`.
 */
internal fun parseDateToEpochMillisOrNull(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    val value = dateStr.trim().substringBefore("T").substringBefore(" ")
    for (pattern in listOf("yyyy-MM-dd", "MM/dd/yyyy")) {
        val parsed = try {
            SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: Exception) {
            null
        }
        if (parsed != null) {
            val year = Calendar.getInstance().apply { time = parsed }.get(Calendar.YEAR)
            return if (year < 1900) null else parsed.time
        }
    }
    return null
}
