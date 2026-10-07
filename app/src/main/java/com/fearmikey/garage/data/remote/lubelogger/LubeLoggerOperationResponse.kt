package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

data class LubeLoggerOperationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("additionalData") val additionalData: AdditionalData?
) {
    data class AdditionalData(
        @SerializedName("recordId") val recordId: Int?
    )
}
