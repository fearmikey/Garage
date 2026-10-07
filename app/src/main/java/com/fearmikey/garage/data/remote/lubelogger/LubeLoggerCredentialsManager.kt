package com.fearmikey.garage.data.remote.lubelogger

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LubeLoggerCredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "lubelogger_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(serverUrl: String, username: String, password: String?, apiKey: String?) {
        sharedPreferences.edit()
            .putString("server_url", serverUrl)
            .putString("username", username)
            .apply {
                if (password != null) {
                    putString("password", password)
                } else {
                    remove("password")
                }
                if (apiKey != null) {
                    putString("api_key", apiKey)
                } else {
                    remove("api_key")
                }
            }
            .apply()
    }

    fun getServerUrl(): String? = sharedPreferences.getString("server_url", null)
    fun getUsername(): String? = sharedPreferences.getString("username", null)
    fun getPassword(): String? = sharedPreferences.getString("password", null)
    fun getApiKey(): String? = sharedPreferences.getString("api_key", null)

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }

    fun isConfigured(): Boolean {
        return !getServerUrl().isNullOrBlank()
    }

    fun getVehicleMapping(localVehicleId: Long): Int? {
        val mappedId = sharedPreferences.getInt("ll_vehicle_map_$localVehicleId", -1)
        return if (mappedId == -1) null else mappedId
    }

    fun saveVehicleMapping(localVehicleId: Long, lubeLoggerVehicleId: Int) {
        sharedPreferences.edit().putInt("ll_vehicle_map_$localVehicleId", lubeLoggerVehicleId).apply()
    }
}
