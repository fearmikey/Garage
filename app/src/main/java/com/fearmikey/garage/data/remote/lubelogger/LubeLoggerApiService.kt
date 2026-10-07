package com.fearmikey.garage.data.remote.lubelogger

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.Response

interface LubeLoggerApiService {

    @GET("api/vehicles")
    suspend fun getVehicles(): Response<List<LubeLoggerVehicleDto>>

    @GET
    suspend fun downloadFile(@retrofit2.http.Url fileUrl: String): Response<okhttp3.ResponseBody>

    @POST("api/vehicles/add")
    suspend fun addVehicle(
        @Body vehicle: LubeLoggerVehicleImportDto
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/gasrecords")
    suspend fun getGasRecords(
        @retrofit2.http.Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerGasRecordDto>>

    @POST("api/vehicle/gasrecords/add")
    suspend fun addGasRecord(
        @retrofit2.http.Query("vehicleId") vehicleId: Int,
        @Body gasRecord: LubeLoggerGasRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/gasrecords/update")
    suspend fun updateGasRecord(
        @Body gasRecord: LubeLoggerGasRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/gasrecords/delete")
    suspend fun deleteGasRecord(
        @retrofit2.http.Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>

    @GET("api/vehicle/servicerecords")
    suspend fun getServiceRecords(
        @retrofit2.http.Query("vehicleId") vehicleId: Int
    ): Response<List<LubeLoggerServiceRecordDto>>

    @POST("api/vehicle/servicerecords/add")
    suspend fun addServiceRecord(
        @retrofit2.http.Query("vehicleId") vehicleId: Int,
        @Body serviceRecord: LubeLoggerServiceRecordDto
    ): Response<LubeLoggerOperationResponse>

    @PUT("api/vehicle/servicerecords/update")
    suspend fun updateServiceRecord(
        @Body serviceRecord: LubeLoggerServiceRecordDto
    ): Response<LubeLoggerOperationResponse>

    @DELETE("api/vehicle/servicerecords/delete")
    suspend fun deleteServiceRecord(
        @retrofit2.http.Query("id") id: Int
    ): Response<LubeLoggerOperationResponse>
}
