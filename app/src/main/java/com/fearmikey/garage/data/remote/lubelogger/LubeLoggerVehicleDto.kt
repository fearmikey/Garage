package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerVehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("make") val make: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("licensePlate") val licensePlate: String?
)
