package com.fearmikey.garage.di

import com.fearmikey.garage.data.remote.RecallApi
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
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

    /**
     * The app-wide HTTP client. Everything that talks to the network (NHTSA, LubeLogger,
     * WebDAV) derives from this one via [OkHttpClient.newBuilder], so they all share one
     * connection pool and dispatcher thread pool; callers only override what differs
     * (timeouts, auth interceptors).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(NHTSA_VPIC_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideVinDecoderApi(retrofit: Retrofit): VinDecoderApi =
        retrofit.create(VinDecoderApi::class.java)

    @Provides
    @Singleton
    fun provideRecallApi(client: OkHttpClient, gson: Gson): RecallApi =
        Retrofit.Builder()
            .baseUrl(NHTSA_RECALLS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RecallApi::class.java)
}
