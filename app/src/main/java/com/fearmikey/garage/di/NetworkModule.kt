package com.fearmikey.garage.di

import com.fearmikey.garage.data.remote.RecallApi
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val NHTSA_VPIC_BASE_URL = "https://vpic.nhtsa.dot.gov/api/"

// NHTSA's Safety Recalls API lives at a completely different base URL/host
// than the vPIC VIN-decode API above, so it gets its own Retrofit instance
// below rather than reusing [provideRetrofit].
private const val NHTSA_RECALLS_BASE_URL = "https://api.nhtsa.gov/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideRetrofit(gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(NHTSA_VPIC_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideVinDecoderApi(retrofit: Retrofit): VinDecoderApi =
        retrofit.create(VinDecoderApi::class.java)

    @Provides
    @Singleton
    fun provideRecallApi(gson: Gson): RecallApi =
        Retrofit.Builder()
            .baseUrl(NHTSA_RECALLS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RecallApi::class.java)
}
