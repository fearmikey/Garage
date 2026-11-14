package com.fearmikey.garage.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.fearmikey.garage.data.local.CloudBackupPreferencesManager
import com.fearmikey.garage.data.local.GARAGE_DATABASE_NAME
import com.fearmikey.garage.data.local.GARAGE_DATABASE_VERSION
import com.fearmikey.garage.data.local.GarageDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupResult {
    data object Success : BackupResult
    /**
     * @param restartRequired true when the live database was already closed (a restore failed
     *   midway and was rolled back), so the app must restart before it can touch the database.
     * @param retryable false when retrying can't help until the user changes something (sync not
     *   configured, credentials rejected), so background workers shouldn't keep retrying.
     */
    data class Failure(
        val message: String,
        val restartRequired: Boolean = false,
        val retryable: Boolean = true,
    ) : BackupResult
}

/**
 * Handles zip export/import of the entire local dataset: the Room database
 * files and the internal vehicle-image library. The same zip is used for
 * manual export, automatic local-folder backups, and WebDAV uploads, and is
 * meant to be a complete, self-contained snapshot a user can move to a new
 * device.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GarageDatabase,
    private val imageStorageManager: ImageStorageManager,
    private val cloudBackupPreferencesManager: CloudBackupPreferencesManager,
) {

    /** Exports the current database + images into a zip written to [destinationUri] (from SAF `CreateDocument`). */
    suspend fun exportBackup(destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext BackupResult.Failure("Could not open destination for writing.")

            writeBackupZip(outputStream)
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Unknown error while exporting backup.")
        }
    }

    /** Exports the current database + images into a zip written to the local [destination] file (e.g. for cloud upload). */
    suspend fun exportBackupToFile(destination: File): BackupResult = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(destination).use { writeBackupZip(it) }
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Unknown error while exporting backup.")
        }
    }

    /** Exports the current database + images into a zip file inside the selected local folder SAF tree URI. */
    suspend fun exportBackupToLocalFolder(folderUriString: String): BackupResult = withContext(Dispatchers.IO) {
        if (folderUriString.isBlank()) {
            val failure = BackupResult.Failure("No local backup folder configured.")
            cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
            return@withContext failure
        }
        try {
            val folderUri = folderUriString.toUri()
            val directory = DocumentFile.fromTreeUri(context, folderUri)
                ?: run {
                    val failure = BackupResult.Failure("Could not access the selected backup folder.")
                    cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
                    return@withContext failure
                }

            if (!directory.exists() || !directory.canWrite()) {
                val failure = BackupResult.Failure("Backup folder does not exist or is not writable.")
                cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
                return@withContext failure
            }

            val backupFileName = "garage-backup.zip"
            var targetFile = directory.findFile(backupFileName)
            if (targetFile == null) {
                targetFile = directory.createFile("application/zip", backupFileName)
            }

            if (targetFile == null) {
                val failure = BackupResult.Failure("Could not create $backupFileName in selected folder.")
                cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
                return@withContext failure
            }

            val outputStream = try {
                context.contentResolver.openOutputStream(targetFile.uri, "wt")
            } catch (_: Exception) {
                context.contentResolver.openOutputStream(targetFile.uri, "w")
            } ?: run {
                val failure = BackupResult.Failure("Could not open output stream for backup file.")
                cloudBackupPreferencesManager.setLastLocalBackupError(failure.message)
                return@withContext failure
            }

            outputStream.use { writeBackupZip(it) }

            cloudBackupPreferencesManager.setLastLocalBackupTimestamp(System.currentTimeMillis())
            cloudBackupPreferencesManager.setLastLocalBackupError(null)

            BackupResult.Success
        } catch (_: SecurityException) {
            val msg = "Permission denied to access folder. Please re-select the backup folder."
            cloudBackupPreferencesManager.setLastLocalBackupError(msg)
            BackupResult.Failure(msg)
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error writing backup to local folder."
            cloudBackupPreferencesManager.setLastLocalBackupError(msg)
            BackupResult.Failure(msg)
        }
    }

    /**
     * Snapshots the database and zips it + images into [outputStream]. Shared by
     * [exportBackup], [exportBackupToFile] and [exportBackupToLocalFolder] so the zip layout
     * never drifts between entry points.
     */
    private fun writeBackupZip(outputStream: OutputStream) {
        // Copying the live .db/-wal files while the app (auto backup, LubeLogger sync, the UI)
        // keeps writing can capture them at different moments and produce a corrupt backup.
        // VACUUM INTO writes a single, self-contained, transactionally consistent copy instead.
        val snapshotDir = File(context.cacheDir, SNAPSHOT_DIR_NAME).apply { mkdirs() }
        val snapshot = File.createTempFile("garage-snapshot", ".db", snapshotDir)
        try {
            snapshot.delete() // VACUUM INTO requires that the target doesn't exist.
            database.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf<Any?>(snapshot.path))

            ZipOutputStream(outputStream.buffered()).use { zip ->
                addFileToZip(zip, snapshot, GARAGE_DATABASE_NAME)
                addDirectoryToZip(zip, imageStorageManager.imagesDir, IMAGES_ZIP_PREFIX)
            }
        } finally {
            snapshot.delete()
        }
    }

    /**
     * Imports a previously exported zip from [sourceUri] (from SAF
     * `OpenDocument`), overwriting the current database and image library.
     *
     * The caller (Settings UI) is responsible for restarting the app process
     * after a [BackupResult.Success], since Room and any in-memory ViewModel
     * state referencing the old database files must not be reused.
     */
    suspend fun importBackup(sourceUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, STAGING_DIR_NAME)
        val imagesDir = imageStorageManager.imagesDir
        val newImagesDir = File(imagesDir.parentFile, imagesDir.name + ".restore")
        var databaseClosed = false
        try {
            stagingDir.deleteRecursively()
            stagingDir.mkdirs()

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext BackupResult.Failure("Could not open the selected backup file.")

            ZipInputStream(inputStream.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val outFile = resolveZipEntryFile(stagingDir, entry.name)
                        ?: return@withContext BackupResult.Failure(
                            "Backup file contains an invalid entry: ${entry.name}"
                        )
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { output -> zip.copyTo(output) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val stagedDb = File(stagingDir, GARAGE_DATABASE_NAME)
            if (!stagedDb.exists()) {
                return@withContext BackupResult.Failure("This file doesn't look like a Garage backup (missing database).")
            }

            // Guard against restoring a backup that was taken with a newer
            // app build (higher schema version) than what's currently
            // installed. Room only ever ships forward migrations, so opening
            // a newer-schema database file with an older app build crashes
            // immediately on every launch with no way to recover other than
            // clearing app data again -- we'd rather fail loudly here, before
            // any live files are touched, than leave the app unusable.
            val backupVersion = try {
                readSchemaVersion(stagedDb)
            } catch (_: SQLiteException) {
                return@withContext BackupResult.Failure(
                    "This file doesn't look like a valid Garage backup (unreadable database)."
                )
            }
            if (backupVersion > GARAGE_DATABASE_VERSION) {
                return@withContext BackupResult.Failure(
                    "This backup was created by a newer version of Garage and can't be restored " +
                        "here. Update the app, then try restoring this backup again."
                )
            }

            // Copy the images first: if that fails, the live database hasn't been touched yet.
            val stagedImagesDir = File(stagingDir, IMAGES_ZIP_PREFIX)
            newImagesDir.deleteRecursively()
            newImagesDir.mkdirs()
            if (stagedImagesDir.exists()) {
                stagedImagesDir.copyRecursively(newImagesDir, overwrite = true)
            }

            // Close the live connection before overwriting the files backing it. From here on
            // the app must restart (on success or failure) since `database` can't be reused.
            database.close()
            databaseClosed = true

            val rollbackDir = File(context.cacheDir, ROLLBACK_DIR_NAME)
            try {
                // Keep the current database so a failed copy can be undone instead of leaving
                // a mix of old and new files behind.
                rollbackDir.deleteRecursively()
                rollbackDir.mkdirs()
                for (dbFile in databaseFiles()) {
                    if (dbFile.exists()) dbFile.copyTo(File(rollbackDir, dbFile.name), overwrite = true)
                }

                for (dbFile in databaseFiles()) {
                    val stagedFile = File(stagingDir, dbFile.name)
                    if (stagedFile.exists()) {
                        stagedFile.copyTo(dbFile, overwrite = true)
                    } else {
                        // No -wal/-shm in the backup (already checkpointed) --
                        // remove any stale side files from the current install.
                        dbFile.delete()
                    }
                }
            } catch (e: Exception) {
                for (dbFile in databaseFiles()) {
                    val saved = File(rollbackDir, dbFile.name)
                    if (saved.exists()) runCatching { saved.copyTo(dbFile, overwrite = true) } else dbFile.delete()
                }
                return@withContext BackupResult.Failure(
                    e.message ?: "Unknown error while importing backup.",
                    restartRequired = true,
                )
            } finally {
                rollbackDir.deleteRecursively()
            }

            // Database restored; swap the image library in.
            imagesDir.deleteRecursively()
            if (!newImagesDir.renameTo(imagesDir)) {
                imagesDir.mkdirs()
                newImagesDir.copyRecursively(imagesDir, overwrite = true)
                newImagesDir.deleteRecursively()
            }

            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(
                e.message ?: "Unknown error while importing backup.",
                restartRequired = databaseClosed,
            )
        } finally {
            stagingDir.deleteRecursively()
            // Leftovers from a failed restore; after a successful swap this no longer exists.
            newImagesDir.deleteRecursively()
        }
    }

    private fun databaseFiles(): List<File> {
        val main = context.getDatabasePath(GARAGE_DATABASE_NAME)
        return listOf(main, File(main.path + "-wal"), File(main.path + "-shm"))
    }

    /**
     * Reads the SQLite `user_version` pragma (i.e. the Room schema version)
     * out of a staged, not-yet-restored database file, without touching the
     * live [database] connection. Exports are a single `VACUUM INTO` snapshot
     * (older ones were checkpointed before zipping), so the main `.db` file's
     * header alone is a reliable source for this.
     */
    private fun readSchemaVersion(dbFile: File): Int =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /** Wipes all database records and stored images. */
    suspend fun clearAllData(): BackupResult = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            imageStorageManager.imagesDir.listFiles()?.forEach { file ->
                file.delete()
            }
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.localizedMessage ?: "Failed to clear data.")
        }
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, directory: File, entryPrefix: String) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                addDirectoryToZip(zip, file, "$entryPrefix${file.name}/")
            } else {
                addFileToZip(zip, file, "$entryPrefix${file.name}")
            }
        }
    }

    /**
     * Resolves a zip entry's path against [stagingDir], rejecting "Zip Slip"
     * path-traversal attempts (e.g. an entry named "../../etc/passwd") by
     * verifying the resolved, canonicalized file still lives inside the
     * staging directory.
     */
    private fun resolveZipEntryFile(stagingDir: File, entryName: String): File? {
        val target = File(stagingDir, entryName)
        val stagingCanonicalPath = stagingDir.canonicalPath
        val targetCanonicalPath = target.canonicalPath
        return if ((targetCanonicalPath.startsWith(stagingCanonicalPath + File.separator)) ||
            (targetCanonicalPath == stagingCanonicalPath)
        ) {
            target
        } else {
            null
        }
    }

    private companion object {
        const val IMAGES_ZIP_PREFIX = "images/"
        const val STAGING_DIR_NAME = "restore_staging"
        const val ROLLBACK_DIR_NAME = "restore_rollback"
        const val SNAPSHOT_DIR_NAME = "backup_snapshot"
    }
}
