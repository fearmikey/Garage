package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.ChargerSpeed
import com.fearmikey.garage.data.local.entity.ChargerVendorType
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.SupplyRecord
import com.fearmikey.garage.data.local.entity.VehicleNote
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

// ---------------------------------------------------------------------------------------------
// Garage-owned extra fields
//
// Garage stores data LubeLogger has no column for in record extra fields named "Garage ...".
// LubeLogger's update endpoints replace the whole extra field list, so pushes rebuild it from
// the server copy: the user's own fields are kept and only Garage's are replaced.
// ---------------------------------------------------------------------------------------------

private const val FIELD_CATEGORY = "Garage Category"
private const val FIELD_TASK = "Garage Task"
private const val FIELD_DEFERRED = "Garage Deferred"
/** Deferral interval, in the server's distance unit. */
private const val FIELD_DEFER_DISTANCE = "Garage Defer Distance"
private const val FIELD_DEFER_MONTHS = "Garage Defer Months"
private const val FIELD_FUEL_GRADE = "Garage Fuel Grade"
private const val FIELD_CHARGER = "Garage Charger"
private const val FIELD_CHARGING_LOCATION = "Garage Charging Location"
/** Estimated range at 100% charge, in the server's distance unit. */
private const val FIELD_RANGE_AT_FULL = "Garage Range at 100%"
private const val FIELD_PRODUCT_URL = "Garage Product URL"

private val MAINTENANCE_FIELDS = listOf(FIELD_CATEGORY, FIELD_TASK, FIELD_DEFERRED, FIELD_DEFER_DISTANCE, FIELD_DEFER_MONTHS)
private val FUEL_FIELDS = listOf(FIELD_FUEL_GRADE)
private val CHARGING_FIELDS = listOf(FIELD_CHARGER, FIELD_CHARGING_LOCATION, FIELD_RANGE_AT_FULL)
private val UPGRADE_FIELDS = listOf(FIELD_CATEGORY, FIELD_PRODUCT_URL)

private fun garageField(name: String, value: String) = LubeLoggerExtraFieldDto(name = name, value = value)

/** The server's extra fields minus those named in [managedNames], followed by [garageFields]. */
internal fun mergeExtraFields(
    server: List<LubeLoggerExtraFieldDto>?,
    garageFields: List<LubeLoggerExtraFieldDto>,
    managedNames: Collection<String>,
): List<LubeLoggerExtraFieldDto> {
    val managed = managedNames.map(LubeLoggerVehicleDto::normalizeFieldName).toSet()
    return server.orEmpty().filter { LubeLoggerVehicleDto.normalizeFieldName(it.name.orEmpty()) !in managed } + garageFields
}

/** Non-blank value of the extra field called [name] (case-insensitive), or null. */
private fun List<LubeLoggerExtraFieldDto>?.fieldValue(name: String): String? = orEmpty()
    .firstOrNull { it.name?.trim().equals(name, ignoreCase = true) }
    ?.value?.trim()?.takeIf { it.isNotEmpty() }

private fun List<LubeLoggerExtraFieldDto>?.hasField(name: String): Boolean =
    orEmpty().any { it.name?.trim().equals(name, ignoreCase = true) }

// ---------------------------------------------------------------------------------------------
// Fuel
// ---------------------------------------------------------------------------------------------

/** [remote] is the current server copy when updating; see [syncLubeLoggerRecords]'s `toDto`. */
fun FuelRecord.toLubeLoggerDto(
    lubeLoggerVehicleId: Int,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerGasRecordDto? = null,
): LubeLoggerGasRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(this.mileage, isMetric)
    val convertedGallons = if (isMetric) UnitConverter.gallonsToLiters(this.gallons) else this.gallons
    val garageFields = listOfNotNull(fuelGrade?.trim()?.takeIf { it.isNotEmpty() }?.let { garageField(FIELD_FUEL_GRADE, it) })

    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        fuelConsumed = "%.3f".format(Locale.US, convertedGallons),
        cost = "%.2f".format(Locale.US, this.totalCost),
        isFillToFull = lubeLoggerBool(this.isFullTank),
        // Garage doesn't track missed fuel-ups, notes or tags on fill-ups: keep the server's.
        missedFuelUp = lubeLoggerBool(parseLubeLoggerBoolean(remote?.missedFuelUp) == true),
        notes = remote?.notes.orEmpty(),
        tags = remote?.tags,
        extraFields = mergeExtraFields(remote?.extraFields, garageFields, FUEL_FIELDS),
        files = remote?.files,
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

    val parsedMileage = fromLubeLoggerOdometer(rawMileage, isMetric)
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
        // LubeLogger has no fuel grade column; Garage keeps it in an extra field. Records created
        // in LubeLogger keep whatever grade was entered locally.
        fuelGrade = extraFields.fieldValue(FIELD_FUEL_GRADE) ?: existing?.fuelGrade,
    )
}

// ---------------------------------------------------------------------------------------------
// EV charging (sent as LubeLogger fuel records on electric vehicles)
// ---------------------------------------------------------------------------------------------

fun ChargingRecord.toLubeLoggerDto(
    lubeLoggerVehicleId: Int,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerGasRecordDto? = null,
): LubeLoggerGasRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(this.mileage, isMetric)
    val garageFields = listOfNotNull(
        garageField(FIELD_CHARGER, chargerSpeed.name),
        garageField(FIELD_CHARGING_LOCATION, vendorType.name),
        estimatedRangeAt100?.let { garageField(FIELD_RANGE_AT_FULL, toLubeLoggerOdometer(it.toDouble(), isMetric).toString()) },
    )

    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        fuelConsumed = "%.3f".format(Locale.US, this.kwhAdded),
        cost = "%.2f".format(Locale.US, this.totalCost),
        isFillToFull = lubeLoggerBool(true),
        missedFuelUp = lubeLoggerBool(parseLubeLoggerBoolean(remote?.missedFuelUp) == true),
        startingSoc = this.batteryPercentStart.toString(),
        endingSoc = this.batteryPercentEnd.toString(),
        notes = this.vendor,
        tags = remote?.tags,
        extraFields = mergeExtraFields(remote?.extraFields, garageFields, CHARGING_FIELDS),
        files = remote?.files,
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

    val parsedMileage = fromLubeLoggerOdometer(rawMileage, isMetric)
    val speed = extraFields.fieldValue(FIELD_CHARGER)?.let { value -> ChargerSpeed.entries.firstOrNull { it.name == value } }
    val location = extraFields.fieldValue(FIELD_CHARGING_LOCATION)?.let { value -> ChargerVendorType.entries.firstOrNull { it.name == value } }
    val range = parseLubeLoggerInt(extraFields.fieldValue(FIELD_RANGE_AT_FULL))
        ?.takeIf { it > 0 }
        ?.let { fromLubeLoggerOdometer(it, isMetric).roundToInt() }

    return ChargingRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        kwhAdded = rawKwh,
        batteryPercentStart = startSoc,
        batteryPercentEnd = endSoc,
        totalCost = parsedCost,
        vendor = notes.orEmpty(),
        estimatedRangeAt100 = range ?: existing?.estimatedRangeAt100,
        chargerSpeed = speed ?: existing?.chargerSpeed ?: ChargerSpeed.LEVEL_2,
        vendorType = location ?: existing?.vendorType ?: ChargerVendorType.HOME,
        lubeLoggerId = recordId,
    )
}

// ---------------------------------------------------------------------------------------------
// Service / repair
// ---------------------------------------------------------------------------------------------

private fun MaintenanceRecord.garageExtraFields(isMetric: Boolean): List<LubeLoggerExtraFieldDto> = buildList {
    add(garageField(FIELD_CATEGORY, category.name))
    taskName?.let { add(garageField(FIELD_TASK, it)) }
    if (isDeferred) {
        add(garageField(FIELD_DEFERRED, "true"))
        deferredMiles?.let { add(garageField(FIELD_DEFER_DISTANCE, toLubeLoggerOdometer(it, isMetric).toString())) }
        deferredMonths?.let { add(garageField(FIELD_DEFER_MONTHS, it.toString())) }
    }
}

fun MaintenanceRecord.toLubeLoggerDto(
    lubeLoggerVehicleId: Int,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerServiceRecordDto? = null,
): LubeLoggerServiceRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(this.mileage, isMetric)

    return LubeLoggerServiceRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.description,
        cost = "%.2f".format(Locale.US, this.cost),
        notes = remote?.notes.orEmpty(),
        tags = remote?.tags,
        extraFields = mergeExtraFields(remote?.extraFields, garageExtraFields(isMetric), MAINTENANCE_FIELDS),
        files = remote?.files,
    )
}

fun MaintenanceRecord.toLubeLoggerRepairDto(
    lubeLoggerVehicleId: Int,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerRepairRecordDto? = null,
): LubeLoggerRepairRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(this.mileage, isMetric)

    return LubeLoggerRepairRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.description,
        cost = "%.2f".format(Locale.US, this.cost),
        notes = remote?.notes.orEmpty(),
        tags = remote?.tags,
        extraFields = mergeExtraFields(remote?.extraFields, garageExtraFields(isMetric), MAINTENANCE_FIELDS),
        files = remote?.files,
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
    dateStr: String?,
    mileage: Int?,
    odometer: String?,
    description: String?,
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
    val rawDescription = description.orEmpty()

    val categoryField = extraFields.fieldValue(FIELD_CATEGORY)
    var category = MaintenanceCategory.entries.firstOrNull { it.name == categoryField } ?: defaultCategory
    var task = extraFields.fieldValue(FIELD_TASK)
    var isDeferred = parseLubeLoggerBoolean(extraFields.fieldValue(FIELD_DEFERRED)) == true
    var cleanDescription = rawDescription

    // Records pushed by older Garage versions carry the task as a " (task)" suffix instead.
    if (task == null) {
        splitLegacyTaskSuffix(rawDescription)?.let { (desc, legacyTask) ->
            cleanDescription = desc
            task = legacyTask
            if (categoryField == null) category = knownCategoryFor(legacyTask) ?: category
            isDeferred = isDeferred || rawDescription.trimEnd().endsWith("[Deferred]")
        }
    }

    // Older Garage versions (or other devices not yet updated) still tag built-in tasks that
    // have since moved out of OTHER (air filters, spark plugs...) as OTHER; re-file them.
    if (category == MaintenanceCategory.OTHER) {
        task?.let(::knownCategoryFor)?.let { category = it }
    }

    // Deferral intervals were local-only before they synced, so fall back to Garage's values.
    val deferDistance = parseLubeLoggerInt(extraFields.fieldValue(FIELD_DEFER_DISTANCE))
        ?.takeIf { it > 0 }?.let { fromLubeLoggerOdometer(it, isMetric) }
    val deferMonths = parseLubeLoggerInt(extraFields.fieldValue(FIELD_DEFER_MONTHS))?.takeIf { it > 0 }

    return MaintenanceRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        date = parseDateToEpochMillis(dateStr),
        mileage = fromLubeLoggerOdometer(rawMileage, isMetric),
        description = cleanDescription,
        cost = parseLubeLoggerDouble(cost) ?: 0.0,
        category = category,
        taskName = task,
        receiptUri = existing?.receiptUri,
        isDeferred = isDeferred,
        deferredMiles = deferDistance ?: existing?.deferredMiles,
        deferredMonths = deferMonths ?: existing?.deferredMonths,
        lubeLoggerId = id,
        lubeLoggerRecordType = recordType,
    )
}

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
    MaintenanceScheduleRules.categoryForTask(taskName)

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

/**
 * True when two maintenance records at the same reading describe the same job. Used before
 * linking an unlinked Garage record to a server record: an oil change and a tire rotation logged
 * at the same visit share date and mileage, and linking them crosswise would overwrite one with
 * the other.
 */
fun MaintenanceRecord.describesSameWorkAs(other: MaintenanceRecord): Boolean =
    description.trim().equals(other.description.trim(), ignoreCase = true) ||
        (taskName != null && taskName.equals(other.taskName, ignoreCase = true))

// ---------------------------------------------------------------------------------------------
// Upgrades (Garage modifications)
// ---------------------------------------------------------------------------------------------

/**
 * The modification's title goes in LubeLogger's description and its description in the notes.
 * Modifications have no odometer in Garage: an update keeps the server's reading, and new
 * records use [latestMileage] (estimated from other logs).
 */
fun ModificationRecord.toLubeLoggerUpgradeDto(
    lubeLoggerVehicleId: Int,
    latestMileage: Double? = null,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerUpgradeRecordDto? = null,
): LubeLoggerUpgradeRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val serverOdometer = (remote?.mileage ?: parseLubeLoggerInt(remote?.odometer))?.takeIf { it > 0 }
    val convertedOdometer = serverOdometer ?: toLubeLoggerOdometer(latestMileage ?: 0.0, isMetric)
    // The category field is always sent: it also marks the record as using title/notes.
    val garageFields = listOfNotNull(
        garageField(FIELD_CATEGORY, category.name),
        productUrl.trim().takeIf { it.isNotEmpty() }?.let { garageField(FIELD_PRODUCT_URL, it) },
    )

    return LubeLoggerUpgradeRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = formatDate(this.date),
        odometer = convertedOdometer.toString(),
        mileage = convertedOdometer,
        description = this.title.trim().ifEmpty { "Upgrade" },
        cost = "%.2f".format(Locale.US, this.cost),
        notes = this.description.trim(),
        tags = remote?.tags,
        extraFields = mergeExtraFields(remote?.extraFields, garageFields, UPGRADE_FIELDS),
        files = remote?.files,
    )
}

fun LubeLoggerUpgradeRecordDto.toModificationRecord(localVehicleId: Long, existing: ModificationRecord? = null): ModificationRecord? {
    val recordId = id ?: return null
    val parsedDate = parseDateToEpochMillis(date)
    val parsedCost = parseLubeLoggerDouble(cost) ?: 0.0
    val rawDescription = description.orEmpty().trim()
    val noteText = notes.orEmpty().trim()

    // Current Garage versions send title/notes plus a category field. Older ones sent
    // "Title - Description" with no notes; anything else is a record made in LubeLogger.
    val isLegacyGarageFormat = !extraFields.hasField(FIELD_CATEGORY) && noteText.isEmpty() && rawDescription.contains(" - ")
    val (title, desc) = if (isLegacyGarageFormat) {
        val parts = rawDescription.split(" - ", limit = 2)
        parts[0].trim() to parts.getOrElse(1) { "" }.trim()
    } else {
        rawDescription to noteText
    }
    val categoryField = extraFields.fieldValue(FIELD_CATEGORY)

    return ModificationRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        title = title.ifEmpty { "Upgrade" },
        description = desc,
        category = ModificationCategory.entries.firstOrNull { it.name == categoryField }
            ?: existing?.category ?: ModificationCategory.OTHER,
        date = parsedDate,
        cost = parsedCost,
        lubeLoggerId = recordId,
        imageUri = existing?.imageUri,
        imageUri2 = existing?.imageUri2,
        imageUri3 = existing?.imageUri3,
        imageUri4 = existing?.imageUri4,
        imageUri5 = existing?.imageUri5,
        imageUri6 = existing?.imageUri6,
        productUrl = extraFields.fieldValue(FIELD_PRODUCT_URL) ?: existing?.productUrl ?: "",
    )
}

/**
 * Estimates the vehicle's odometer reading at [targetDate] based on known mileage readings
 * from fuel, maintenance, or charging logs. Falls back to [initialMileage] or the earliest/latest
 * known reading if no prior readings exist.
 */
fun estimateMileageAtDate(
    targetDate: Long,
    readings: List<Pair<Long, Double>>,
    initialMileage: Double? = null,
): Double {
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
    return 0.0
}

// ---------------------------------------------------------------------------------------------
// Supplies
// ---------------------------------------------------------------------------------------------

fun SupplyRecord.toLubeLoggerSupplyDto(lubeLoggerVehicleId: Int, remote: LubeLoggerSupplyRecordDto? = null): LubeLoggerSupplyRecordDto =
    LubeLoggerSupplyRecordDto(
        id = lubeLoggerId?.toString(),
        vehicleId = lubeLoggerVehicleId.toString(),
        date = formatDate(date),
        partNumber = partNumber.trim(),
        partSupplier = supplier.trim(),
        partQuantity = "%.2f".format(Locale.US, quantity),
        description = description.trim(),
        cost = "%.2f".format(Locale.US, cost),
        notes = notes.trim(),
        tags = remote?.tags,
        extraFields = remote?.extraFields,
        files = remote?.files,
    )

/** Converts a LubeLogger supply into a Garage supply, or null if it has no usable ID. */
fun LubeLoggerSupplyRecordDto.toSupplyRecord(localVehicleId: Long, existing: SupplyRecord? = null): SupplyRecord? {
    val recordId = parseLubeLoggerInt(id)?.takeIf { it > 0 } ?: return null
    return SupplyRecord(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        description = description?.trim().orEmpty(),
        partNumber = partNumber?.trim().orEmpty(),
        supplier = partSupplier?.trim().orEmpty(),
        quantity = parseLubeLoggerDouble(partQuantity) ?: 0.0,
        cost = parseLubeLoggerDouble(cost) ?: 0.0,
        date = parseDateToEpochMillis(date.orEmpty()),
        notes = notes?.trim().orEmpty(),
        lubeLoggerId = recordId,
        lubeLoggerSyncHash = existing?.lubeLoggerSyncHash,
    )
}

// ---------------------------------------------------------------------------------------------
// Notes (Garage knowledge-base notes <-> LubeLogger vehicle notes)
// ---------------------------------------------------------------------------------------------

/** LubeLogger requires both a description and note text, so blank Garage notes stay local. */
val LubeLoggerNoteRecordDto.isPushable: Boolean
    get() = !description.isNullOrBlank() && !noteText.isNullOrBlank()

fun VehicleNote.toLubeLoggerNoteDto(lubeLoggerVehicleId: Int, remote: LubeLoggerNoteRecordDto? = null): LubeLoggerNoteRecordDto =
    LubeLoggerNoteRecordDto(
        id = lubeLoggerId?.toString(),
        vehicleId = lubeLoggerVehicleId.toString(),
        description = title.trim(),
        noteText = content,
        pinned = lubeLoggerBool(parseLubeLoggerBoolean(remote?.pinned) == true),
        tags = remote?.tags,
        extraFields = remote?.extraFields,
        files = remote?.files,
    )

/** Converts a LubeLogger note into a Garage note, or null if it has no usable ID. */
fun LubeLoggerNoteRecordDto.toVehicleNote(localVehicleId: Long, existing: VehicleNote? = null): VehicleNote? {
    val recordId = parseLubeLoggerInt(id)?.takeIf { it > 0 } ?: return null
    return VehicleNote(
        id = existing?.id ?: 0,
        vehicleId = localVehicleId,
        title = description?.trim().orEmpty(),
        content = noteText.orEmpty(),
        category = existing?.category ?: "General",
        lastUpdated = System.currentTimeMillis(),
        lubeLoggerId = recordId,
        lubeLoggerSyncHash = existing?.lubeLoggerSyncHash,
    )
}

// ---------------------------------------------------------------------------------------------
// Odometer
// ---------------------------------------------------------------------------------------------

/**
 * LubeLogger stores odometers as whole numbers in its own unit, while Garage stores fractional
 * canonical miles. Converting with full precision both ways means a km value entered on either
 * side survives the round trip unchanged.
 */
internal fun toLubeLoggerOdometer(miles: Double, isMetric: Boolean): Int =
    (if (isMetric) UnitConverter.milesToKm(miles) else miles).roundToInt()

internal fun fromLubeLoggerOdometer(odometer: Int, isMetric: Boolean): Double =
    if (isMetric) UnitConverter.kmToMiles(odometer.toDouble()) else odometer.toDouble()

private fun isMetricUnits(lubeLoggerUnitSystem: String) = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)

/**
 * [remote] is the server copy when updating: its initial odometer, tags, extra fields, files and
 * equipment links are kept (LubeLogger clears whatever an update leaves out).
 */
fun MaintenanceRecord.toLubeLoggerOdometerDto(
    lubeLoggerVehicleId: Int,
    lubeLoggerUnitSystem: String = "imperial",
    remote: LubeLoggerOdometerRecordDto? = null,
): LubeLoggerOdometerRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(this.mileage, isMetric)

    return LubeLoggerOdometerRecordDto(
        id = this.lubeLoggerId?.toString(),
        vehicleId = lubeLoggerVehicleId.toString(),
        date = formatDate(this.date),
        initialOdometer = remote?.initialOdometer
            ?.takeIf { (parseLubeLoggerInt(it) ?: -1) in 0..convertedOdometer },
        odometer = convertedOdometer.toString(),
        notes = this.description.takeIf { it != ODOMETER_CHECK_IN_DESCRIPTION } ?: "",
        tags = remote?.tags,
        extraFields = remote?.extraFields,
        files = remote?.files,
        equipmentRecordId = remote?.equipmentRecordId,
    )
}

fun createLubeLoggerOdometerDto(
    lubeLoggerVehicleId: Int,
    dateMillis: Long,
    mileageMiles: Double,
    notes: String,
    lubeLoggerUnitSystem: String = "imperial",
): LubeLoggerOdometerRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = toLubeLoggerOdometer(mileageMiles, isMetric)

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
    val parsedMileage = fromLubeLoggerOdometer(rawMileage, isMetric)

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

// ---------------------------------------------------------------------------------------------
// Sync fingerprints
//
// A stable string of the normalized fields both sides store. Dates are compared by day and
// money/volume are rounded, so unit and precision quirks can't make an unchanged record look
// edited. Mileage is fingerprinted as the whole number LubeLogger itself stores (in its unit), so
// a mileage edit only counts as a change when it changes the server value.
//
// Fields that started syncing later are appended only when they hold a non-default value, so
// fingerprints stored by older versions stay valid for records that don't use them.
// ---------------------------------------------------------------------------------------------

fun ChargingRecord.syncFingerprint(lubeLoggerUnitSystem: String = "imperial"): String {
    val isMetric = isMetricUnits(lubeLoggerUnitSystem)
    val base = "${formatDate(date)}|${toLubeLoggerOdometer(mileage, isMetric)}|${"%.3f".format(Locale.US, kwhAdded)}|" +
        "${"%.2f".format(Locale.US, totalCost)}|$batteryPercentStart|$batteryPercentEnd|${vendor.trim()}"
    val hasDetails = chargerSpeed != ChargerSpeed.LEVEL_2 || vendorType != ChargerVendorType.HOME || estimatedRangeAt100 != null
    if (!hasDetails) return base
    val range = estimatedRangeAt100?.let { toLubeLoggerOdometer(it.toDouble(), isMetric) }
    return "$base|${chargerSpeed.name}|${vendorType.name}|${range ?: ""}"
}

fun FuelRecord.syncFingerprint(lubeLoggerUnitSystem: String = "imperial"): String {
    val base = "${formatDate(date)}|${toLubeLoggerOdometer(mileage, isMetricUnits(lubeLoggerUnitSystem))}|" +
        "${"%.2f".format(Locale.US, gallons)}|${"%.2f".format(Locale.US, totalCost)}|$isFullTank"
    val grade = fuelGrade?.trim().orEmpty()
    return if (grade.isEmpty()) base else "$base|$grade"
}

fun MaintenanceRecord.syncFingerprint(lubeLoggerUnitSystem: String = "imperial"): String {
    val isMetric = isMetricUnits(lubeLoggerUnitSystem)
    val base = "${formatDate(date)}|${toLubeLoggerOdometer(mileage, isMetric)}|${description.trim()}|" +
        "${"%.2f".format(Locale.US, cost)}|${category.name}|${taskName.orEmpty()}|$isDeferred"
    if (!isDeferred || (deferredMiles == null && deferredMonths == null)) return base
    return "$base|${deferredMiles?.let { toLubeLoggerOdometer(it, isMetric) } ?: ""}|${deferredMonths ?: ""}"
}

fun ModificationRecord.syncFingerprint(): String {
    val base = "${formatDate(date)}|${title.trim()}|${description.trim()}|${"%.2f".format(Locale.US, cost)}"
    if (category == ModificationCategory.OTHER && productUrl.isBlank()) return base
    return "$base|${category.name}|${productUrl.trim()}"
}

fun SupplyRecord.syncFingerprint(): String =
    "${formatDate(date)}|${description.trim()}|${partNumber.trim()}|${supplier.trim()}|" +
        "${"%.2f".format(Locale.US, quantity)}|${"%.2f".format(Locale.US, cost)}|${notes.trim()}"

fun VehicleNote.syncFingerprint(): String = "${title.trim()}|${content.trim()}"

// ---------------------------------------------------------------------------------------------
// Dates
// ---------------------------------------------------------------------------------------------

private fun parseDateToEpochMillis(dateStr: String?): Long =
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
