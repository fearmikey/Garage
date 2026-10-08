package com.fearmikey.garage.data.remote.lubelogger

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores LubeLogger connection settings in EncryptedSharedPreferences.
 *
 * `open` with lazily-created preferences so JVM unit tests can subclass it without touching
 * the Android Keystore (mirrors CloudBackupPreferencesManager).
 */
@Suppress("DEPRECATION")
@Singleton
open class LubeLoggerCredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "lubelogger_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * The config dialog never pre-fills the password, so a null [password] means "unchanged"
     * when the username is the same; it is only cleared if the username changes.
     */
    open fun saveCredentials(serverUrl: String, username: String, password: String?, apiKey: String?, unitSystem: String = "imperial") {
        // Stray whitespace (e.g. a Tab from moving between fields) silently breaks Basic auth.
        val serverUrl = normalizeServerUrl(serverUrl)
        val username = username.trim()
        val apiKey = apiKey?.trim()?.takeIf { it.isNotEmpty() }
        val usernameChanged = username != getUsername()
        val connectionChanged = serverUrl != getServerUrl() || usernameChanged ||
            password != null || apiKey != getApiKey()
        sharedPreferences.edit {
            if (connectionChanged) {
                // The last result was for different settings; don't keep showing it.
                remove(KEY_LAST_SUCCESS)
                remove(KEY_LAST_FAILURE)
                remove(KEY_LAST_ERROR)
            }
            putString("server_url", serverUrl)
            putString("username", username)
            putString("unit_system", unitSystem)
            if (password != null) {
                putString("password", password)
            } else if (usernameChanged || username.isBlank()) {
                remove("password")
            }
            if (apiKey != null) {
                putString("api_key", apiKey)
            } else {
                remove("api_key")
            }
        }
        if (connectionChanged) syncStatusState?.value = LubeLoggerSyncStatus.NeverSynced
    }

    // ---------------------------------------------------------------------
    // Sync status (written by the sync worker, shown in Settings)
    // ---------------------------------------------------------------------

    private var syncStatusState: MutableStateFlow<LubeLoggerSyncStatus>? = null

    /** Live sync status; the worker runs in-process, so Settings updates as soon as it changes. */
    open fun syncStatus(): StateFlow<LubeLoggerSyncStatus> = synchronized(this) {
        syncStatusState ?: MutableStateFlow(readStoredSyncStatus()).also { syncStatusState = it }
    }.asStateFlow()

    open fun markSyncStarted() {
        updateSyncStatus(LubeLoggerSyncStatus.Syncing(lastSuccessAt()))
    }

    open fun markSyncSucceeded(at: Long = System.currentTimeMillis()) {
        sharedPreferences.edit {
            putLong(KEY_LAST_SUCCESS, at)
            remove(KEY_LAST_FAILURE)
            remove(KEY_LAST_ERROR)
        }
        updateSyncStatus(LubeLoggerSyncStatus.Success(at))
    }

    open fun markSyncFailed(message: String, at: Long = System.currentTimeMillis()) {
        sharedPreferences.edit {
            putLong(KEY_LAST_FAILURE, at)
            putString(KEY_LAST_ERROR, message)
        }
        updateSyncStatus(LubeLoggerSyncStatus.Failed(at, message, lastSuccessAt()))
    }

    /** A sync was cancelled (e.g. replaced by a newer one): fall back to the last real result. */
    open fun markSyncCancelled() {
        updateSyncStatus(readStoredSyncStatus())
    }

    private fun updateSyncStatus(status: LubeLoggerSyncStatus) {
        syncStatus() // make sure the flow exists before publishing to it
        syncStatusState?.value = status
    }

    private fun lastSuccessAt(): Long? =
        sharedPreferences.getLong(KEY_LAST_SUCCESS, -1L).takeIf { it > 0 }

    private fun readStoredSyncStatus(): LubeLoggerSyncStatus {
        val lastSuccess = lastSuccessAt()
        val lastFailure = sharedPreferences.getLong(KEY_LAST_FAILURE, -1L).takeIf { it > 0 }
        return when {
            lastFailure != null && (lastSuccess == null || lastFailure > lastSuccess) -> LubeLoggerSyncStatus.Failed(
                at = lastFailure,
                message = sharedPreferences.getString(KEY_LAST_ERROR, null) ?: "Sync failed",
                lastSuccessAt = lastSuccess,
            )
            lastSuccess != null -> LubeLoggerSyncStatus.Success(lastSuccess)
            else -> LubeLoggerSyncStatus.NeverSynced
        }
    }

    open fun getServerUrl(): String? {
        val raw = sharedPreferences.getString("server_url", null)?.trim() ?: return null
        if (raw.isBlank()) return null
        return normalizeServerUrl(raw)
    }

    open fun getUsername(): String? = sharedPreferences.getString("username", null)?.trim()
    open fun getPassword(): String? = sharedPreferences.getString("password", null)
    open fun getApiKey(): String? = sharedPreferences.getString("api_key", null)?.trim()
    open fun getUnitSystem(): String = sharedPreferences.getString("unit_system", "imperial") ?: "imperial"

    open fun clearCredentials() {
        sharedPreferences.edit { clear() }
        syncStatusState?.value = LubeLoggerSyncStatus.NeverSynced
    }

    private companion object {
        const val KEY_LAST_SUCCESS = "ll_last_sync_success_at"
        const val KEY_LAST_FAILURE = "ll_last_sync_failure_at"
        const val KEY_LAST_ERROR = "ll_last_sync_error"

        fun normalizeServerUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return ""
            return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                "http://$trimmed"
            } else {
                trimmed
            }
        }
    }

    open fun isConfigured(): Boolean {
        return !getServerUrl().isNullOrBlank()
    }

    /**
     * True when requests are authenticated with Basic auth (username/password, no API key).
     * LubeLogger only accepts API keys on /api routes, so web-UI endpoints such as vehicle
     * image upload need Basic auth.
     */
    fun usesBasicAuth(): Boolean =
        getApiKey().isNullOrBlank() && !getUsername().isNullOrBlank() && !getPassword().isNullOrBlank()

    open fun getVehicleMapping(localVehicleId: Long): Int? {
        val mappedId = sharedPreferences.getInt("ll_vehicle_map_$localVehicleId", -1)
        return if (mappedId == -1) null else mappedId
    }

    open fun saveVehicleMapping(localVehicleId: Long, lubeLoggerVehicleId: Int) {
        sharedPreferences.edit { putInt("ll_vehicle_map_$localVehicleId", lubeLoggerVehicleId) }
    }

    /** Vehicle details agreed with LubeLogger at the last successful sync (three-way merge base). */
    open fun getVehicleDetailsSnapshot(localVehicleId: Long): VehicleSyncDetails? {
        val prefix = "ll_vehicle_details_$localVehicleId"
        if (!sharedPreferences.contains("${prefix}_condition")) return null
        // Fields added later are null in older snapshots, which the merge treats as a first sync.
        return VehicleSyncDetails(
            vin = sharedPreferences.getString("${prefix}_vin", ""),
            condition = sharedPreferences.getString("${prefix}_condition", null),
            purchaseMiles = sharedPreferences.getString("${prefix}_purchase_miles", ""),
            year = sharedPreferences.getString("${prefix}_year", null),
            make = sharedPreferences.getString("${prefix}_make", null),
            model = sharedPreferences.getString("${prefix}_model", null),
            trim = sharedPreferences.getString("${prefix}_trim", null),
            plate = sharedPreferences.getString("${prefix}_plate", null),
        )
    }

    open fun saveVehicleDetailsSnapshot(localVehicleId: Long, details: VehicleSyncDetails) {
        val prefix = "ll_vehicle_details_$localVehicleId"
        sharedPreferences.edit {
            putString("${prefix}_vin", details.vin.orEmpty())
            putString("${prefix}_condition", details.condition ?: VehicleSyncDetails.CONDITION_USED)
            putString("${prefix}_purchase_miles", details.purchaseMiles.orEmpty())
            listOf(
                "year" to details.year,
                "make" to details.make,
                "model" to details.model,
                "trim" to details.trim,
                "plate" to details.plate,
            ).forEach { (key, value) ->
                if (value == null) remove("${prefix}_$key") else putString("${prefix}_$key", value)
            }
        }
    }

    /** Primary photo filename and server image path agreed at the last sync, or null. */
    open fun getVehiclePhotoSnapshot(localVehicleId: Long): Pair<String?, String?>? {
        val prefix = "ll_vehicle_photo_$localVehicleId"
        if (!sharedPreferences.contains("${prefix}_saved")) return null
        return sharedPreferences.getString("${prefix}_local", null) to sharedPreferences.getString("${prefix}_remote", null)
    }

    open fun saveVehiclePhotoSnapshot(localVehicleId: Long, localImage: String?, remoteImage: String?) {
        val prefix = "ll_vehicle_photo_$localVehicleId"
        sharedPreferences.edit {
            putBoolean("${prefix}_saved", true)
            if (localImage == null) remove("${prefix}_local") else putString("${prefix}_local", localImage)
            if (remoteImage == null) remove("${prefix}_remote") else putString("${prefix}_remote", remoteImage)
        }
    }
}
