package com.fearmikey.garage.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response shape for NHTSA's `DecodeVinValues` endpoint. The API always
 * returns HTTP 200 with a single-element `Results` array; decode failures
 * are signalled via [VinDecodeResult.errorCode] (anything other than "0")
 * rather than an HTTP error status.
 */
data class VinDecodeResponse(
    @SerializedName("Results") val results: List<VinDecodeResult> = emptyList(),
)

data class VinDecodeResult(
    @SerializedName("ModelYear") val modelYear: String? = null,
    @SerializedName("Make") val make: String? = null,
    @SerializedName("Model") val model: String? = null,
    @SerializedName("Trim") val trim: String? = null,
    @SerializedName("DriveType") val driveType: String? = null,
    @SerializedName("ErrorCode") val errorCode: String? = null,
    @SerializedName("ErrorText") val errorText: String? = null,
    // Additional specs, surfaced to the user on the vehicle's "Specs" tab but
    // otherwise unused by the decode logic below.
    @SerializedName("EngineCylinders") val engineCylinders: String? = null,
    @SerializedName("DisplacementL") val displacementL: String? = null,
    @SerializedName("EngineHP") val engineHp: String? = null,
    @SerializedName("FuelTypePrimary") val fuelTypePrimary: String? = null,
    @SerializedName("TransmissionStyle") val transmissionStyle: String? = null,
    @SerializedName("TransmissionSpeeds") val transmissionSpeeds: String? = null,
    @SerializedName("BodyClass") val bodyClass: String? = null,
    @SerializedName("Doors") val doors: String? = null,
    @SerializedName("GVWR") val gvwr: String? = null,
    @SerializedName("Series") val series: String? = null,
    @SerializedName("VehicleType") val vehicleType: String? = null,
    @SerializedName("PlantCity") val plantCity: String? = null,
    @SerializedName("PlantState") val plantState: String? = null,
    @SerializedName("PlantCountry") val plantCountry: String? = null,
    @SerializedName("Manufacturer") val manufacturer: String? = null,
)
