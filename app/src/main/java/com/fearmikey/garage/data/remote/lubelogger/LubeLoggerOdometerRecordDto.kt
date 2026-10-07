package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.annotations.SerializedName

/**
 * Mirrors LubeLogger's `OdometerRecordExportModel`, used both for
 * GET /api/vehicle/odometerrecords and POST /api/vehicle/odometerrecords/add.
 *
 * Every field is a nullable String: LubeLogger returns them as strings in its default
 * culture mode and as numbers in culture-invariant mode, and Gson's String adapter
 * accepts both. Null fields are omitted when serializing.
 */
data class LubeLoggerOdometerRecordDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("vehicleId") val vehicleId: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("initialOdometer") val initialOdometer: String? = null,
    @SerializedName("odometer") val odometer: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: String? = null,
)
