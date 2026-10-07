package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

// Used when reading vehicles from LubeLogger GET /api/vehicles
data class LubeLoggerVehicleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("make") val make: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("licensePlate") val licensePlate: String? = null
)

// Used when creating a new vehicle in LubeLogger POST /api/vehicles/add
data class LubeLoggerVehicleImportDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("year") val year: String,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("licensePlate") val licensePlate: String = "N/A",
    @SerializedName("identifier") val identifier: String = "LicensePlate",
    @SerializedName("fuelType") val fuelType: String = "Gasoline"
)
