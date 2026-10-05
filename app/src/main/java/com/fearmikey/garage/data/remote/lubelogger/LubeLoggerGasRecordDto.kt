package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerGasRecordDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("date") val date: String, // format: "MM/dd/yyyy" or standard string date
    @SerializedName("mileage") val mileage: Int,
    @SerializedName("gallons") val gallons: Double,
    @SerializedName("cost") val cost: Double,
    @SerializedName("isFillToFull") val isFillToFull: Boolean,
    @SerializedName("missedFuelUp") val missedFuelUp: Boolean,
    @SerializedName("startingSoc") val startingSoc: Int = 20,
    @SerializedName("endingSoc") val endingSoc: Int = 80,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("files") val files: List<String> = emptyList()
)
