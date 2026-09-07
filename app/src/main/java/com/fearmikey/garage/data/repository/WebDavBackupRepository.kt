package com.fearmikey.garage.data.repository

import android.content.Context
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
private const val BACKUP_FILE_NAME = "garage-backup.zip"

/**
 * Uploads the app's backup zip to a user-supplied WebDAV server (e.g.
 * Nextcloud, ownCloud) using raw HTTP requests -- WebDAV isn't a REST API so
 * this deliberately doesn't go through Retrofit.
 */
@Singleton
class WebDavBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val cloudBackupPreferencesManager: CloudBackupPreferencesManager,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun syncNow(): BackupResult = withContext(Dispatchers.IO) {
        val enabled = cloudBackupPreferencesManager.cloudSyncEnabled.first()
        val url = cloudBackupPreferencesManager.webdavUrl.first()
        val username = cloudBackupPreferencesManager.webdavUsername.first()
        val password = cloudBackupPreferencesManager.getWebdavPassword().orEmpty()

        if (!enabled || url.isBlank() || username.isBlank()) {
            return@withContext BackupResult.Failure("Cloud sync is not configured.")
        }

        val tempFile = File(context.cacheDir, "cloud_backup_upload.zip")
        try {
            when (val exportResult = backupRepository.exportBackupToFile(tempFile)) {
                is BackupResult.Failure -> {
                    cloudBackupPreferencesManager.setLastSyncError(exportResult.message)
                    return@withContext exportResult
                }
                BackupResult.Success -> Unit
            }

            val requestUrl = buildBackupUrl(url)
            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", Credentials.basic(username, password))
                .put(tempFile.asRequestBody(ZIP_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    cloudBackupPreferencesManager.setLastSyncTimestamp(System.currentTimeMillis())
                    cloudBackupPreferencesManager.setLastSyncError(null)
                    BackupResult.Success
                } else {
                    val message = "Upload failed: HTTP ${response.code} ${response.message}".trim()
                    cloudBackupPreferencesManager.setLastSyncError(message)
                    BackupResult.Failure(message)
                }
            }
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error while syncing to WebDAV."
            cloudBackupPreferencesManager.setLastSyncError(message)
            BackupResult.Failure(message)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Lightweight reachability + credential check used by the "Test
     * connection" button. Doesn't touch stored preferences so it can be used
     * to validate values before they're saved.
     */
    suspend fun testConnection(url: String, username: String, password: String): BackupResult =
        withContext(Dispatchers.IO) {
            if (url.isBlank() || username.isBlank()) {
                return@withContext BackupResult.Failure("Server URL and username are required.")
            }
            try {
                val request = Request.Builder()
                    .url(normalizeBaseUrl(url))
                    .header("Authorization", Credentials.basic(username, password))
                    .method("PROPFIND", null)
                    .header("Depth", "0")
                    .build()

                client.newCall(request).execute().use { response ->
                    // WebDAV servers reply 207 Multi-Status to PROPFIND; a
                    // plain 2xx is also accepted in case the endpoint doesn't
                    // implement PROPFIND but is otherwise reachable.
                    if (response.isSuccessful || response.code == 207) {
                        BackupResult.Success
                    } else if (response.code == 401 || response.code == 403) {
                        BackupResult.Failure("Authentication failed. Check your username and password.")
                    } else {
                        BackupResult.Failure("Connection failed: HTTP ${response.code} ${response.message}".trim())
                    }
                }
            } catch (e: Exception) {
                BackupResult.Failure(e.message ?: "Unknown error while testing connection.")
            }
        }

    private fun buildBackupUrl(url: String): String = normalizeBaseUrl(url) + BACKUP_FILE_NAME

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
