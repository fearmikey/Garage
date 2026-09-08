package com.fearmikey.garage.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
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
 *
 * Modern phone cameras produce multi-megabyte, 12+ megapixel photos, but
 * every place we display a vehicle photo (the dashboard card, the vehicle
 * detail header, and the shared-element transition between them) only ever
 * renders it at a small fraction of that resolution. Decoding/uploading
 * those oversized bitmaps on every screen they appear on -- especially
 * mid-animation during the Dashboard <-> VehicleDetail shared-element
 * transition -- was a major source of scroll/navigation jank. So on ingest
 * we downsample to a size that comfortably covers any on-screen usage and
 * re-encode at a reasonable quality, rather than storing the original bytes
 * verbatim.
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
     *
     * The source image is downsampled (preserving aspect ratio and EXIF
     * orientation) so the stored file is cheap to decode and display.
     */
    suspend fun copyPickedImageToInternalStorage(pickedUri: Uri): String = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("picked_image_", ".tmp", context.cacheDir)
        try {
            context.contentResolver.openInputStream(pickedUri).use { input ->
                requireNotNull(input) { "Could not open input stream for $pickedUri" }
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            val filename = "${UUID.randomUUID()}.jpg"
            val destination = File(imagesDir, filename)
            downsampleAndSave(tempFile, destination)
            filename
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Decodes [source] at a reduced sample size (capped to [MAX_DIMENSION_PX]
     * on the longest side), applies any EXIF rotation/flip so the re-encoded
     * copy renders upright without relying on EXIF metadata, and writes the
     * result to [destination] as a JPEG.
     */
    private fun downsampleAndSave(source: File, destination: File) {
        val orientation = ExifInterface(source.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        }
        val decoded = requireNotNull(BitmapFactory.decodeFile(source.absolutePath, options)) {
            "Could not decode image at $source"
        }

        val upright = applyExifOrientation(decoded, orientation)
        try {
            destination.outputStream().use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        } finally {
            if (upright !== decoded) decoded.recycle()
            upright.recycle()
        }
    }

    /** Smallest power-of-two sample size that brings the longest side down to <= [maxDimension]. */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val longestSide = maxOf(width, height)
        while ((longestSide / sampleSize) > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Resolves a stored [filename] (as saved on [com.fearmikey.garage.data.local.entity.Vehicle.imageUri]) to its [File]. */
    fun imageFile(filename: String): File = File(imagesDir, filename)

    fun deleteImage(filename: String) {
        imageFile(filename).delete()
    }

    private companion object {
        const val IMAGES_DIR_NAME = "images"

        /** Longest-side cap (px) for stored vehicle photos; comfortably above any on-screen size. */
        const val MAX_DIMENSION_PX = 2048

        const val JPEG_QUALITY = 88
    }
}
