package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerGasRecordDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("vehicleId") val vehicleId: Int? = null,
    @SerializedName("date") val date: String,
    @SerializedName("odometer") val odometer: String? = null, // Used for PUT/POST (string in LubeLogger Export models)
    @SerializedName("mileage") val mileage: Int? = null, // Used for GET (int in LubeLogger GasRecord models)
    @SerializedName("fuelConsumed") val fuelConsumed: String? = null, // Used for PUT/POST
    @SerializedName("gallons") val gallons: Double? = null, // Used for GET
    @SerializedName("cost") val cost: String? = null, // Used for PUT/POST
    @SerializedName("costDouble") val costDouble: Double? = null, // Some GETs might return double, we can rely on String for export/import models usually
    @SerializedName("isFillToFull") val isFillToFull: Boolean? = null, // Used for PUT/POST
    @SerializedName("missedFuelUp") val missedFuelUp: Boolean? = null,
    @SerializedName("startingSoc") val startingSoc: String? = "20",
    @SerializedName("endingSoc") val endingSoc: String? = "80",
    @SerializedName("notes") val notes: String = "",
    @SerializedName("tags") val tags: String? = null
)
