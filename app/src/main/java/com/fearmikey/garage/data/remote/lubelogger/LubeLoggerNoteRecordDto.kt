package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * LubeLogger's NoteRecordExportModel (api/vehicle/notes). Garage's knowledge-base notes map
 * title -> [description] and content -> [noteText]. Updates overwrite every field, so [pinned],
 * [tags], [extraFields] and [files] are sent back unchanged.
 */
data class LubeLoggerNoteRecordDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("vehicleId") val vehicleId: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("noteText") val noteText: String? = null,
    @SerializedName("pinned") val pinned: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    @SerializedName("files") val files: List<JsonObject>? = null,
)
