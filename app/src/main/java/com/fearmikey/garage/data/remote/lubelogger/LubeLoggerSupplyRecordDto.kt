package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * LubeLogger's SupplyRecordExportModel. Numbers travel as strings (the server's
 * FromIntOptional/FromDecimalOptional converters accept both), so they're parsed leniently.
 * [cost] is the total paid for [partQuantity] units. Text fields are nullable because Gson
 * ignores Kotlin null-safety when the server omits them.
 *
 * Updates overwrite every field, so [tags], [extraFields] and [files] are sent back unchanged.
 */
data class LubeLoggerSupplyRecordDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("vehicleId") val vehicleId: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("partNumber") val partNumber: String? = null,
    @SerializedName("partSupplier") val partSupplier: String? = null,
    @SerializedName("partQuantity") val partQuantity: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("cost") val cost: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    @SerializedName("files") val files: List<JsonObject>? = null,
)
