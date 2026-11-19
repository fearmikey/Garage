package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** See [LubeLoggerServiceRecordDto] for why notes, tags, extra fields and files are round-tripped. */
data class LubeLoggerRepairRecordDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("vehicleId") val vehicleId: Int? = null,
    @SerializedName("date") val date: String,
    @SerializedName("odometer") val odometer: String? = null,
    @SerializedName("mileage") val mileage: Int? = null,
    @SerializedName("description") val description: String,
    @SerializedName("cost") val cost: String? = null,
    @SerializedName("notes") val notes: String? = "",
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    @SerializedName("files") val files: List<JsonObject>? = null,
)
