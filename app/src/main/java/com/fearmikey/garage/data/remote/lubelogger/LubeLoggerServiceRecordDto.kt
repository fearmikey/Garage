package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerServiceRecordDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("date") val date: String,
    @SerializedName("mileage") val mileage: Int,
    @SerializedName("description") val description: String,
    @SerializedName("cost") val cost: Double,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("files") val files: List<String> = emptyList()
)
