package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerOdometerRecordDto(
    @SerializedName("id") val id: String = "0",
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("date") val date: String,
    @SerializedName("initialOdometer") val initialOdometer: String? = null,
    @SerializedName("odometer") val odometer: String,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("tags") val tags: String = "",
    @SerializedName("files") val files: List<String> = emptyList()
)
