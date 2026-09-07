package com.fearmikey.garage.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the vehicle photo library under `context.filesDir/images/`.
 *
 * Images picked via the Android Photo Picker are only granted a short-lived,
 * one-time read grant on their `content://` URI -- there is no persistable
 * permission for Photo Picker selections. So instead of storing that URI, we
 * eagerly stream the bytes into our own app-private file the moment the user
 * picks a photo. This also means the image ships for free as part of the zip
 * export/import (see BackupRepository), with no separate media backup logic.
 */
@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Directory holding all vehicle images; created on first access. */
    val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR_NAME).apply { mkdirs() }

    /**
     * Copies the bytes behind [pickedUri] (typically from
     * `ActivityResultContracts.PickVisualMedia`) into a new file under
     * [imagesDir] and returns the generated filename (not a full path/URI).
     */
    suspend fun copyPickedImageToInternalStorage(pickedUri: Uri): String = withContext(Dispatchers.IO) {
        val filename = "${UUID.randomUUID()}.jpg"
        val destination = File(imagesDir, filename)
        context.contentResolver.openInputStream(pickedUri).use { input ->
            requireNotNull(input) { "Could not open input stream for $pickedUri" }
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        filename
    }

    /** Resolves a stored [filename] (as saved on [com.fearmikey.garage.data.local.entity.Vehicle.imageUri]) to its [File]. */
    fun imageFile(filename: String): File = File(imagesDir, filename)

    fun deleteImage(filename: String) {
        imageFile(filename).delete()
    }

    private companion object {
        const val IMAGES_DIR_NAME = "images"
    }
}
