package com.fearmikey.garage.data.remote

import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit client for the free NHTSA vPIC VIN decoding API. */
interface VinDecoderApi {
    @GET("vehicles/decodevinvalues/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json",
    ): VinDecodeResponse
}
