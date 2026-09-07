package com.fearmikey.garage.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.fearmikey.garage.data.local.GARAGE_DATABASE_NAME
import com.fearmikey.garage.data.local.GARAGE_DATABASE_VERSION
import com.fearmikey.garage.data.local.GarageDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupResult {
    data object Success : BackupResult
    data class Failure(val message: String) : BackupResult
}

/**
 * Handles zip export/import of the entire local dataset: the Room database
 * files and the internal vehicle-image library. This is the app's only form
 * of backup -- there is no cloud sync -- so the zip is meant to be a
 * complete, self-contained snapshot a user can move to a new device.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GarageDatabase,
    private val imageStorageManager: ImageStorageManager,
) {

    /** Exports the current database + images into a zip written to [destinationUri] (from SAF `CreateDocument`). */
    suspend fun exportBackup(destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Room defaults to WAL journal mode, meaning recent writes may
            // only exist in the "-wal" side file rather than the main ".db"
            // file. Running a FULL checkpoint flushes all committed WAL
            // content back into the main database file, so even if we didn't
            // include the (now largely empty) -wal/-shm files, the exported
            // .db file alone would still be a consistent, complete snapshot.
            // We don't need to close the connection to do this.
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL);").close()

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext BackupResult.Failure("Could not open destination for writing.")

            ZipOutputStream(outputStream.buffered()).use { zip ->
                for (dbFile in databaseFiles()) {
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, dbFile.name)
                    }
                }
                addDirectoryToZip(zip, imageStorageManager.imagesDir, IMAGES_ZIP_PREFIX)
            }
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Unknown error while exporting backup.")
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

            // Close the live connection before we start overwriting the files
            // backing it. The app process will be restarted by the caller
            // right after this, so we never attempt to reopen/reuse `database`.
            database.close()

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

            val stagedImagesDir = File(stagingDir, IMAGES_ZIP_PREFIX)
            val imagesDir = imageStorageManager.imagesDir
            imagesDir.deleteRecursively()
            imagesDir.mkdirs()
            if (stagedImagesDir.exists()) {
                stagedImagesDir.copyRecursively(imagesDir, overwrite = true)
            }

            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Unknown error while importing backup.")
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun databaseFiles(): List<File> {
        val main = context.getDatabasePath(GARAGE_DATABASE_NAME)
        return listOf(main, File(main.path + "-wal"), File(main.path + "-shm"))
    }

    /**
     * Reads the SQLite `user_version` pragma (i.e. the Room schema version)
     * out of a staged, not-yet-restored database file, without touching the
     * live [database] connection. `exportBackup` always checkpoints the WAL
     * before zipping, so the main `.db` file's header alone is a reliable
     * source for this -- we don't need the `-wal`/`-shm` side files here.
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
        return if (targetCanonicalPath.startsWith(stagingCanonicalPath + File.separator) ||
            targetCanonicalPath == stagingCanonicalPath
        ) {
            target
        } else {
            null
        }
    }

    private companion object {
        const val IMAGES_ZIP_PREFIX = "images/"
        const val STAGING_DIR_NAME = "restore_staging"
    }
}
