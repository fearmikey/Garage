package com.fearmikey.garage.data.remote

import com.fearmikey.garage.data.remote.dto.RecallResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit client for NHTSA's free recalls-by-vehicle API. */
interface RecallApi {
    @GET("recalls/recallsByVehicle")
    suspend fun getRecalls(
        @Query("make") make: String,
        @Query("model") model: String,
        @Query("modelYear") modelYear: Int,
    ): RecallResponse
}
