package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.util.UnitConverter

/**
 * The vehicle details Garage keeps in sync with LubeLogger custom extra fields. Values are
 * normalized strings so they can be compared and stored as a sync snapshot:
 * - [vin]: uppercase VIN, or "" when unknown.
 * - [condition]: [CONDITION_NEW] / [CONDITION_USED].
 * - [purchaseMiles]: purchase odometer in *miles* (Garage's canonical unit), or "" when unknown.
 *
 * - [year] / [make] / [model]: the vehicle identity, synced to LubeLogger's own fields.
 * - [trim]: the `Trim` custom extra field.
 * - [plate]: the license plate (Garage keeps it on the Registration screen).
 *
 * For the server side, a null field means "not present on the server". For the newer fields
 * (year onwards), a null *Garage* value means "this field doesn't take part in the sync".
 */
data class VehicleSyncDetails(
    val vin: String?,
    val condition: String?,
    val purchaseMiles: String?,
    val year: String? = null,
    val make: String? = null,
    val model: String? = null,
    val trim: String? = null,
    val plate: String? = null,
) {
    companion object {
        const val CONDITION_NEW = "New"
        const val CONDITION_USED = "Used"
        const val TRIM_FIELD_NAME = "Trim"
    }
}

/** [plate] comes from the vehicle's registration record; null leaves the plate out of the sync. */
fun Vehicle.syncDetails(plate: String? = null) = VehicleSyncDetails(
    vin = vin.trim().uppercase(),
    condition = if (purchasedNew) VehicleSyncDetails.CONDITION_NEW else VehicleSyncDetails.CONDITION_USED,
    purchaseMiles = initialMileage?.toString().orEmpty(),
    year = year?.toString().orEmpty(),
    make = make.trim(),
    model = model.trim(),
    trim = trim.trim(),
    plate = plate?.trim(),
)

fun Vehicle.withSyncDetails(details: VehicleSyncDetails) = copy(
    vin = details.vin.orEmpty(),
    purchasedNew = details.condition == VehicleSyncDetails.CONDITION_NEW,
    initialMileage = details.purchaseMiles?.toIntOrNull(),
    // Never blank out Garage's identity fields: LubeLogger requires them, so a blank value only
    // means "unknown".
    year = details.year?.toIntOrNull() ?: year,
    make = details.make?.takeIf { it.isNotBlank() } ?: make,
    model = details.model?.takeIf { it.isNotBlank() } ?: model,
    trim = details.trim ?: trim,
)

fun LubeLoggerVehicleDto.syncDetails(lubeLoggerUnitSystem: String): VehicleSyncDetails {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    // A field that exists but is blank means "cleared in LubeLogger" (""), as opposed to a
    // field that doesn't exist at all (null = no information).
    val hasVinField = matchingExtraFields(LubeLoggerVehicleDto.VIN_FIELD_NAME).isNotEmpty()
    val hasMileageField = matchingExtraFields(*LubeLoggerVehicleDto.PURCHASE_MILEAGE_FIELD_NAMES).isNotEmpty()
    val vin = findVin()
    return VehicleSyncDetails(
        vin = vin ?: if (hasVinField) "" else null,
        condition = findPurchasedNew()?.let {
            if (it) VehicleSyncDetails.CONDITION_NEW else VehicleSyncDetails.CONDITION_USED
        },
        purchaseMiles = findPurchaseOdometer()
            ?.let { if (isMetric) UnitConverter.kmToMiles(it) else it }
            ?.toString()
            ?: if (hasMileageField) "" else null,
        year = year?.toString(),
        make = make?.trim(),
        model = model?.trim(),
        trim = matchingExtraFields(VehicleSyncDetails.TRIM_FIELD_NAME).firstOrNull()?.let { it.value?.trim().orEmpty() },
        plate = syncablePlate(vin),
    )
}

/**
 * The server's license plate, or "" when it only holds a placeholder: "N/A" (sent by Garage
 * when it had no VIN), or the VIN itself (older Garage versions stored the VIN there).
 */
internal fun LubeLoggerVehicleDto.syncablePlate(vin: String?): String? {
    val plate = licensePlate?.trim() ?: return null
    return when {
        plate.equals("N/A", ignoreCase = true) -> ""
        vin != null && plate.equals(vin, ignoreCase = true) -> ""
        com.fearmikey.garage.util.VinValidator.isValidVin(plate.uppercase()) -> ""
        else -> plate
    }
}

/**
 * Three-way merge of Garage ([local]) and LubeLogger ([remote]) using the values agreed at the
 * last successful sync ([base]) to tell which side changed:
 * - only Garage changed -> Garage's value wins (and is pushed);
 * - only LubeLogger changed -> LubeLogger's value wins (and is pulled);
 * - both changed -> Garage wins.
 *
 * On the very first sync (no [base]) LubeLogger only fills values Garage doesn't have yet
 * (blank VIN / mileage, or the default "Used" condition); otherwise Garage wins.
 */
fun mergeVehicleSyncDetails(
    local: VehicleSyncDetails,
    remote: VehicleSyncDetails,
    base: VehicleSyncDetails?,
): VehicleSyncDetails = VehicleSyncDetails(
    vin = mergeField(local.vin.orEmpty(), remote.vin, base?.vin, localIsUnset = local.vin.isNullOrBlank()),
    condition = mergeField(
        local.condition ?: VehicleSyncDetails.CONDITION_USED, remote.condition, base?.condition,
        localIsUnset = local.condition != VehicleSyncDetails.CONDITION_NEW,
    ),
    purchaseMiles = mergeField(
        local.purchaseMiles.orEmpty(), remote.purchaseMiles, base?.purchaseMiles,
        localIsUnset = local.purchaseMiles.isNullOrBlank(),
    ),
    year = mergeOptional(local.year, remote.year, base?.year),
    make = mergeOptional(local.make, remote.make, base?.make),
    model = mergeOptional(local.model, remote.model, base?.model),
    trim = mergeOptional(local.trim, remote.trim, base?.trim),
    plate = mergeOptional(local.plate, remote.plate, base?.plate),
)

/** Like [mergeField], but a null Garage value keeps the field out of the sync. */
private fun mergeOptional(local: String?, remote: String?, base: String?): String? =
    local?.let { mergeField(it, remote, base, localIsUnset = it.isBlank()) }

private fun mergeField(local: String, remote: String?, base: String?, localIsUnset: Boolean): String = when {
    base == null -> if (localIsUnset && !remote.isNullOrBlank()) remote else local
    local != base -> local
    remote != null && remote != base -> remote
    else -> local
}

/**
 * Builds the PUT /api/vehicles/update body that writes [details] into this vehicle's custom
 * extra fields, keeping every other server value as-is. Returns null when the server already
 * matches, or when LubeLogger would reject the update (missing year/make/model or plate).
 */
fun LubeLoggerVehicleDto.toUpdateDto(details: VehicleSyncDetails, lubeLoggerUnitSystem: String): LubeLoggerVehicleUpdateDto? {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val serverMileage = details.purchaseMiles?.toIntOrNull()
        ?.let { if (isMetric) UnitConverter.milesToKm(it) else it }
        ?.toString().orEmpty()

    var fields = extraFields.orEmpty()
    fields = fields.upsert(arrayOf(LubeLoggerVehicleDto.VIN_FIELD_NAME), details.vin.orEmpty())
    fields = fields.upsert(LubeLoggerVehicleDto.PURCHASE_CONDITION_FIELD_NAMES, details.condition.orEmpty())
    fields = fields.upsert(LubeLoggerVehicleDto.PURCHASE_MILEAGE_FIELD_NAMES, serverMileage)
    details.trim?.let { fields = fields.upsert(arrayOf(VehicleSyncDetails.TRIM_FIELD_NAME), it) }

    // Blank Garage values never clear LubeLogger's required fields.
    val newYear = details.year?.toIntOrNull() ?: year
    val newMake = details.make?.takeIf { it.isNotBlank() } ?: make
    val newModel = details.model?.takeIf { it.isNotBlank() } ?: model
    val newPlate = details.plate?.takeIf { it.isNotBlank() } ?: licensePlate
    val unchanged = fields == extraFields.orEmpty() && newYear == year && newMake == make &&
        newModel == model && newPlate == licensePlate
    if (unchanged) return null

    val identifier = vehicleIdentifier?.takeIf { it.isNotBlank() } ?: "LicensePlate"
    if (newYear == null || newMake.isNullOrBlank() || newModel.isNullOrBlank()) return null
    if (identifier == "LicensePlate" && newPlate.isNullOrBlank()) return null

    return LubeLoggerVehicleUpdateDto(
        id = id.toString(),
        year = newYear.toString(),
        make = newMake,
        model = newModel,
        licensePlate = newPlate.orEmpty(),
        identifier = identifier,
        fuelType = when {
            isElectric == true -> "Electric"
            isDiesel == true -> "Diesel"
            else -> "Gasoline"
        },
        useEngineHours = (useHours == true).toString(),
        odometerOptional = (odometerOptional == true).toString(),
        extraFields = fields,
        tags = tags.orEmpty().joinToString(" "),
    )
}

/**
 * Sets [value] on the first extra field matching any of [names] (keeping the user's own field
 * name), or appends a new field named `names[0]`. Blank values are never added as new fields,
 * but do clear an existing one. Values are compared case-insensitively to avoid churn.
 */
private fun List<LubeLoggerExtraFieldDto>.upsert(names: Array<String>, value: String): List<LubeLoggerExtraFieldDto> {
    val wanted = names.map(LubeLoggerVehicleDto::normalizeFieldName).toSet()
    val index = indexOfFirst { LubeLoggerVehicleDto.normalizeFieldName(it.name.orEmpty()) in wanted }
    if (index == -1) {
        return if (value.isBlank()) this else this + LubeLoggerExtraFieldDto(names.first(), value, false, 0)
    }
    val existing = this[index]
    if (existing.value.orEmpty().trim().equals(value, ignoreCase = true)) return this
    return toMutableList().apply { this[index] = existing.copy(value = value) }
}
