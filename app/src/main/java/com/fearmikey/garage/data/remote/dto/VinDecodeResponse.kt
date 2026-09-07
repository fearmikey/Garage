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
    @SerializedName("ErrorCode") val errorCode: String? = null,
    @SerializedName("ErrorText") val errorText: String? = null,
)
