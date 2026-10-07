package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface LubeLoggerApiService {

    @GET("api/vehicles")
    suspend fun getVehicles(): Response<List<LubeLoggerVehicleDto>>

    /**
     * Raw vehicle objects in the server's *native* culture, so that every field can be
     * round-tripped unchanged through [saveVehicle] (e.g. purchase dates keep their format).
     */
    @Headers("${LubeLoggerApiFactory.NATIVE_CULTURE_HEADER}: true")
    @GET("api/vehicles")
    suspend fun getVehiclesRaw(): Response<List<JsonObject>>

    @GET
    suspend fun downloadFile(@retrofit2.http.Url fileUrl: String): Response<okhttp3.ResponseBody>

    /**
     * Web-UI endpoint (not part of the public API) that stores an upload under /temp and
     * returns its path as a JSON string. Requires Basic auth; API keys are rejected.
     */
    @Multipart
    @POST("Files/HandleFileUpload")
    suspend fun uploadTempFile(@Part file: MultipartBody.Part): Response<String>

    /**
     * Web-UI endpoint used by LubeLogger's "Edit Vehicle" dialog. The public API has no way to
     * set a vehicle image, so this is the only route for pushing a photo. It overwrites every
     * vehicle field, so callers must send the full vehicle (see [flattenJsonToFormFields]).
     */
    @FormUrlEncoded
    @POST("Vehicle/SaveVehicle")
    suspend fun saveVehicle(@FieldMap fields: Map<String, String>): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicles/update")
    suspend fun updateVehicle(
        @Body vehicle: LubeLoggerVehicleUpdateDto,
    ): Response<LubeLoggerOperationResponse>

    @POST("api/vehicles/add")
    suspend fun addVehicle(
        @Body vehicle: LubeLoggerVehicleImportDto
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/gasrecords")
    suspend fun getGasRecords(
        @Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerGasRecordDto>>

    @POST("api/vehicle/gasrecords/add")
    suspend fun addGasRecord(
        @Query("vehicleId") vehicleId: Int,
        @Body gasRecord: LubeLoggerGasRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/gasrecords/update")
    suspend fun updateGasRecord(
        @Body gasRecord: LubeLoggerGasRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/gasrecords/delete")
    suspend fun deleteGasRecord(
        @Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/servicerecords")
    suspend fun getServiceRecords(
        @Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerServiceRecordDto>>

    @POST("api/vehicle/servicerecords/add")
    suspend fun addServiceRecord(
        @Query("vehicleId") vehicleId: Int,
        @Body serviceRecord: LubeLoggerServiceRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/servicerecords/update")
    suspend fun updateServiceRecord(
        @Body serviceRecord: LubeLoggerServiceRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/servicerecords/delete")
    suspend fun deleteServiceRecord(
        @Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/odometerrecords")
    suspend fun getOdometerRecords(
        @Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerOdometerRecordDto>>

    @POST("api/vehicle/odometerrecords/add")
    suspend fun addOdometerRecord(
        @Query("vehicleId") vehicleId: Int,
        @Body odometerRecord: LubeLoggerOdometerRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/odometerrecords/update")
    suspend fun updateOdometerRecord(
        @Body odometerRecord: LubeLoggerOdometerRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/odometerrecords/delete")
    suspend fun deleteOdometerRecord(
        @Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/repairrecords")
    suspend fun getRepairRecords(
        @Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerRepairRecordDto>>

    @POST("api/vehicle/repairrecords/add")
    suspend fun addRepairRecord(
        @Query("vehicleId") vehicleId: Int,
        @Body repairRecord: LubeLoggerRepairRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/repairrecords/update")
    suspend fun updateRepairRecord(
        @Body repairRecord: LubeLoggerRepairRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/repairrecords/delete")
    suspend fun deleteRepairRecord(
        @Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/upgraderecords")
    suspend fun getUpgradeRecords(
        @Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerUpgradeRecordDto>>

    @POST("api/vehicle/upgraderecords/add")
    suspend fun addUpgradeRecord(
        @Query("vehicleId") vehicleId: Int,
        @Body upgradeRecord: LubeLoggerUpgradeRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/upgraderecords/update")
    suspend fun updateUpgradeRecord(
        @Body upgradeRecord: LubeLoggerUpgradeRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/upgraderecords/delete")
    suspend fun deleteUpgradeRecord(
        @Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @Multipart
    @POST("api/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Query("entityId") entityId: Int,
        @Query("entityType") entityType: Int,
    ): Response<LubeLoggerOperationResponse>
}
