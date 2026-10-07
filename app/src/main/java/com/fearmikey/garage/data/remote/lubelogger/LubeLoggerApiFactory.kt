package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
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

        // Ask LubeLogger for culture-invariant JSON so dates/numbers don't depend on the
        // server's locale (e.g. dd/MM/yyyy vs MM/dd/yyyy). Requests tagged with
        // NATIVE_CULTURE_HEADER opt out (used when round-tripping a full vehicle object).
        clientBuilder.addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            if (original.header(NATIVE_CULTURE_HEADER) != null) {
                builder.removeHeader(NATIVE_CULTURE_HEADER)
            } else {
                builder.header("culture-invariant", "true")
            }
            chain.proceed(builder.build())
        }

        // Send JSON bodies as plain `application/json`. LubeLogger's
        // /api/vehicle/odometerrecords/add crashes the whole server process (nginx then
        // returns 502 and a blank "ghost" record is left behind) when the Content-Type carries
        // a `charset` parameter, which Retrofit's Gson converter always adds.
        clientBuilder.addInterceptor { chain ->
            val original = chain.request()
            val body = original.body
            val type = body?.contentType()
            if (body != null && type?.subtype == "json" && type.charset() != null) {
                val plainJson = body.withContentType("application/json".toMediaType())
                chain.proceed(original.newBuilder().method(original.method, plainJson).build())
            } else {
                chain.proceed(original)
            }
        }

        if (com.fearmikey.garage.BuildConfig.DEBUG) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request()
                val reqBody = request.body?.takeIf { it.contentType()?.subtype == "json" }?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    buffer.readUtf8().take(2048)
                }
                val response = chain.proceed(request)
                val peek = if (response.body?.contentType()?.subtype == "json") {
                    response.peekBody(2048).string()
                } else {
                    "<${response.body?.contentType()}>"
                }
                android.util.Log.d(
                    "LubeLoggerHttp",
                    "${request.method} ${request.url} auth=${describeAuth(request)} body=$reqBody -> ${response.code} $peek"
                )
                response
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(LubeLoggerApiService::class.java)
    }

    companion object {
        /** Marker header: skip the `culture-invariant` header for this request. */
        const val NATIVE_CULTURE_HEADER = "X-Garage-Native-Culture"
    }
}

/** Debug-log description of the credentials on a request, without revealing them. */
private fun describeAuth(request: okhttp3.Request): String {
    request.header("x-api-key")?.let { return "apiKey(len=${it.length})" }
    val basic = request.header("Authorization")?.removePrefix("Basic ") ?: return "none"
    val decoded = runCatching { String(android.util.Base64.decode(basic, android.util.Base64.NO_WRAP)) }.getOrNull()
    val user = decoded?.substringBefore(':')
    val passLen = decoded?.substringAfter(':', "")?.length
    return "basic(user=$user, passLen=$passLen)"
}

private fun RequestBody.withContentType(type: MediaType): RequestBody {
    val delegate = this
    return object : RequestBody() {
        override fun contentType(): MediaType = type
        override fun contentLength(): Long = delegate.contentLength()
        override fun writeTo(sink: BufferedSink) = delegate.writeTo(sink)
    }
}
