package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * LubeLogger's update endpoints overwrite *every* field: notes, tags, extra fields and attached
 * files that aren't sent are cleared. Updates are therefore built on top of the server copy
 * (see [toLubeLoggerDto]'s `remote` parameter) so [notes], [tags], [files] and the user's own
 * extra fields survive a Garage edit.
 */
data class LubeLoggerServiceRecordDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("vehicleId") val vehicleId: Int? = null,
    @SerializedName("date") val date: String,
    @SerializedName("odometer") val odometer: String? = null,
    @SerializedName("mileage") val mileage: Int? = null,
    @SerializedName("description") val description: String,
    @SerializedName("cost") val cost: String? = null,
    @SerializedName("notes") val notes: String? = "",
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("extraFields") val extraFields: List<LubeLoggerExtraFieldDto>? = null,
    /** Attachments (LubeLogger `UploadedFiles`), round-tripped as-is. */
    @SerializedName("files") val files: List<JsonObject>? = null,
)
