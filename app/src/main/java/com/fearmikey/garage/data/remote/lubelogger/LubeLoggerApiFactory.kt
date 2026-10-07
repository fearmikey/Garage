package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class LubeLoggerApiFactory @Inject constructor(
    private val credentialsManager: LubeLoggerCredentialsManager,
    private val gson: Gson,
) {
    fun createApiService(): LubeLoggerApiService? {
        val serverUrl = credentialsManager.getServerUrl()
        val username = credentialsManager.getUsername()
        val password = credentialsManager.getPassword()
        val apiKey = credentialsManager.getApiKey()

        if (serverUrl.isNullOrBlank()) {
            return null
        }

        // Ensure URL ends with a slash
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        // Add auth interceptor
        if (!apiKey.isNullOrBlank()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-api-key", apiKey)
                    .build()
                chain.proceed(request)
            }
        } else if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            clientBuilder.addInterceptor { chain ->
                val auth = Credentials.basic(username, password)
                val request = chain.request().newBuilder()
                    .header("Authorization", auth)
                    .build()
                chain.proceed(request)
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(LubeLoggerApiService::class.java)
    }
}
