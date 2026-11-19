package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Garage pushes a modification's title as [description] and its description as [notes], with
 * its category in a "Garage Category" extra field. Records pushed by older Garage versions hold
 * "Title - Description" in [description] instead (see [toModificationRecord]).
 *
 * See [LubeLoggerServiceRecordDto] for why tags, extra fields and files are round-tripped.
 */
data class LubeLoggerUpgradeRecordDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("vehicleId") val vehicleId: Int? = null,
    @SerializedName("date") val date: String,
    @SerializedName("odometer") val odometer: String? = null,
    @SerializedName("mileage") val mileage: Int? = null,
    // Nullable: Gson leaves it null when the server omits it, whatever Kotlin's type says.
    @SerializedName("description") val description: String?,
    @SerializedName("cost") val cost: String? = null,
    @SerializedName("notes") val notes: String? = "",
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    @SerializedName("files") val files: List<JsonObject>? = null,
)
