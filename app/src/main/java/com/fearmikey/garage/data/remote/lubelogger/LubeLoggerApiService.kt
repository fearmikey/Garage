package com.fearmikey.garage.data.remote.lubelogger

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface LubeLoggerApiService {

    @GET("api/vehicles")
    suspend fun getVehicles(): Response<List<LubeLoggerVehicleDto>>

    @POST("api/vehicle/gasrecords/add")
    suspend fun addGasRecord(
        @Body gasRecord: LubeLoggerGasRecordDto
    ): Response<Unit>

    @POST("api/vehicle/servicerecords/add")
    suspend fun addServiceRecord(
        @Body serviceRecord: LubeLoggerServiceRecordDto
    ): Response<Unit>
}
