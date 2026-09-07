package com.fearmikey.garage.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudBackupDataStore by preferencesDataStore(name = "cloud_backup_preferences")

/**
 * Stores WebDAV cloud-backup settings. Everything except the password lives
 * in its own DataStore (kept separate from [PreferencesManager]'s so this
 * class doesn't need to reach into another file's private delegate); the
 * password is sensitive, so it's kept separately in
 * [EncryptedSharedPreferences].
 */
@Singleton
class CloudBackupPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        val CLOUD_SYNC_ENABLED_KEY = booleanPreferencesKey("cloud_sync_enabled")
        val WEBDAV_URL_KEY = stringPreferencesKey("webdav_url")
        val WEBDAV_USERNAME_KEY = stringPreferencesKey("webdav_username")
        val LAST_SYNC_TIMESTAMP_KEY = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_ERROR_KEY = stringPreferencesKey("last_sync_error")

        private const val ENCRYPTED_PREFS_NAME = "cloud_backup_secure_prefs"
        private const val WEBDAV_PASSWORD_KEY = "webdav_password"
    }

    val cloudSyncEnabled: Flow<Boolean> = context.cloudBackupDataStore.data
        .map { preferences -> preferences[CLOUD_SYNC_ENABLED_KEY] ?: false }

    val webdavUrl: Flow<String> = context.cloudBackupDataStore.data
        .map { preferences -> preferences[WEBDAV_URL_KEY] ?: "" }

    val webdavUsername: Flow<String> = context.cloudBackupDataStore.data
        .map { preferences -> preferences[WEBDAV_USERNAME_KEY] ?: "" }

    val lastSyncTimestamp: Flow<Long?> = context.cloudBackupDataStore.data
        .map { preferences -> preferences[LAST_SYNC_TIMESTAMP_KEY] }

    val lastSyncError: Flow<String?> = context.cloudBackupDataStore.data
        .map { preferences -> preferences[LAST_SYNC_ERROR_KEY] }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.cloudBackupDataStore.edit { preferences -> preferences[CLOUD_SYNC_ENABLED_KEY] = enabled }
    }

    suspend fun setWebdavUrl(url: String) {
        context.cloudBackupDataStore.edit { preferences -> preferences[WEBDAV_URL_KEY] = url }
    }

    suspend fun setWebdavUsername(username: String) {
        context.cloudBackupDataStore.edit { preferences -> preferences[WEBDAV_USERNAME_KEY] = username }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long?) {
        context.cloudBackupDataStore.edit { preferences ->
            if (timestamp == null) {
                preferences.remove(LAST_SYNC_TIMESTAMP_KEY)
            } else {
                preferences[LAST_SYNC_TIMESTAMP_KEY] = timestamp
            }
        }
    }

    suspend fun setLastSyncError(message: String?) {
        context.cloudBackupDataStore.edit { preferences ->
            if (message == null) {
                preferences.remove(LAST_SYNC_ERROR_KEY)
            } else {
                preferences[LAST_SYNC_ERROR_KEY] = message
            }
        }
    }

    /**
     * Writing to [EncryptedSharedPreferences] does synchronous disk I/O, so
     * this is dispatched off the calling thread even though the API itself
     * isn't `suspend`-aware internally.
     */
    suspend fun setWebdavPassword(password: String) = withContext(Dispatchers.IO) {
        encryptedPrefs().edit().putString(WEBDAV_PASSWORD_KEY, password).apply()
    }

    fun getWebdavPassword(): String? = encryptedPrefs().getString(WEBDAV_PASSWORD_KEY, null)

    private fun encryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
