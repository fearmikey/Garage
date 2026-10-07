package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

// Used when reading vehicles from LubeLogger GET /api/vehicles
data class LubeLoggerVehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("make") val make: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("licensePlate") val licensePlate: String? = null,
    @SerializedName("imageLocation") val imageLocation: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    // Round-tripped unchanged when Garage updates the vehicle (PUT /api/vehicles/update
    // replaces all of these).
    @SerializedName("vehicleIdentifier") val vehicleIdentifier: String? = null,
    @SerializedName("isElectric") val isElectric: Boolean? = null,
    @SerializedName("isDiesel") val isDiesel: Boolean? = null,
    @SerializedName("useHours") val useHours: Boolean? = null,
    @SerializedName("odometerOptional") val odometerOptional: Boolean? = null,
    @SerializedName("tags") val tags: List<String>? = null,
) {
    /**
     * LubeLogger has no dedicated VIN field. Garage stores it in a custom "VIN" extra field;
     * older Garage versions (and some users) put it in License Plate instead. If a VIN extra
     * field exists it is authoritative (even when blank); otherwise License Plate is used if it
     * holds a valid 17-character VIN.
     */
    fun findVin(): String? {
        val vinFields = matchingExtraFields(VIN_FIELD_NAME)
        val candidates = if (vinFields.isNotEmpty()) vinFields.mapNotNull { it.value } else listOfNotNull(licensePlate)
        return candidates
            .map { it.trim().uppercase() }
            .firstOrNull { com.fearmikey.garage.util.VinValidator.isValidVin(it) }
    }

    /**
     * LubeLogger has no purchase-condition field, so this reads a custom extra field such as
     * "Purchase Condition" = "New"/"Used". Returns true for New, false for Used, null if absent.
     */
    fun findPurchasedNew(): Boolean? =
        extraFieldValues(*PURCHASE_CONDITION_FIELD_NAMES).firstNotNullOfOrNull { value ->
            when (value.trim().lowercase()) {
                "new", "true", "yes" -> true
                "used", "pre-owned", "preowned", "false", "no" -> false
                else -> null
            }
        }

    /**
     * LubeLogger has no purchase-mileage field, so this reads a custom extra field such as
     * "Purchase Mileage". The value is in the server's distance unit (accepts "45,000" or "45000 mi").
     */
    fun findPurchaseOdometer(): Int? =
        extraFieldValues(*PURCHASE_MILEAGE_FIELD_NAMES).firstNotNullOfOrNull { value ->
            value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.toInt()?.takeIf { it >= 0 }
        }

    internal fun matchingExtraFields(vararg names: String): List<LubeLoggerExtraFieldDto> {
        val wanted = names.map(::normalizeFieldName).toSet()
        return extraFields.orEmpty().filter { normalizeFieldName(it.name.orEmpty()) in wanted }
    }

    /** Non-blank values of extra fields whose name matches, ignoring case, spaces and punctuation. */
    private fun extraFieldValues(vararg names: String): List<String> =
        matchingExtraFields(*names).mapNotNull { it.value?.takeIf(String::isNotBlank) }

    companion object {
        const val VIN_FIELD_NAME = "VIN"
        val PURCHASE_CONDITION_FIELD_NAMES = arrayOf("Purchase Condition", "Purchased Condition", "Condition")
        val PURCHASE_MILEAGE_FIELD_NAMES = arrayOf(
            "Purchase Mileage", "Purchase Odometer", "Mileage at Purchase", "Initial Mileage", "Initial Odometer",
        )

        internal fun normalizeFieldName(name: String) = name.lowercase().filter(Char::isLetterOrDigit)
    }
}

data class LubeLoggerExtraFieldDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("isRequired") val isRequired: Boolean? = null,
    @SerializedName("fieldType") val fieldType: Int? = null,
)

// Used when creating a new vehicle in LubeLogger POST /api/vehicles/add
data class LubeLoggerVehicleImportDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("year") val year: String,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("licensePlate") val licensePlate: String = "N/A",
    @SerializedName("identifier") val identifier: String = "LicensePlate",
    @SerializedName("fuelType") val fuelType: String = "Gasoline",
)

/**
 * Body for PUT /api/vehicles/update (LubeLogger's VehicleImportModel). Every field is replaced,
 * so it must be built from the current server copy (see [toUpdateDto]).
 */
data class LubeLoggerVehicleUpdateDto(
    @SerializedName("id") val id: String,
    @SerializedName("year") val year: String,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("licensePlate") val licensePlate: String,
    @SerializedName("identifier") val identifier: String,
    @SerializedName("fuelType") val fuelType: String,
    @SerializedName("useEngineHours") val useEngineHours: String,
    @SerializedName("odometerOptional") val odometerOptional: String,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>,
    @SerializedName("tags") val tags: String,
)
