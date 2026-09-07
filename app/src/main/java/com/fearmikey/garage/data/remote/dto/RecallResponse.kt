package com.fearmikey.garage.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response shape for NHTSA's `recallsByVehicle` endpoint (base URL
 * `https://api.nhtsa.gov/`). NHTSA doesn't offer a VIN-keyed recalls lookup,
 * so [com.fearmikey.garage.data.repository.RecallRepository] looks recalls up
 * using the vehicle's year/make/model instead -- results are therefore for
 * the general year/make/model, not verified against the specific VIN/trim.
 */
data class RecallResponse(
    @SerializedName("Count") val count: Int = 0,
    @SerializedName("Message") val message: String? = null,
    @SerializedName("results") val results: List<RecallDto> = emptyList(),
)

data class RecallDto(
    @SerializedName("Manufacturer") val manufacturer: String? = null,
    @SerializedName("NHTSACampaignNumber") val campaignNumber: String? = null,
    @SerializedName("parkIt") val parkIt: Boolean = false,
    @SerializedName("parkOutSide") val parkOutside: Boolean = false,
    @SerializedName("ReportReceivedDate") val reportReceivedDate: String? = null,
    @SerializedName("Component") val component: String? = null,
    @SerializedName("Summary") val summary: String? = null,
    @SerializedName("Consequence") val consequence: String? = null,
    @SerializedName("Remedy") val remedy: String? = null,
    @SerializedName("Notes") val notes: String? = null,
)
