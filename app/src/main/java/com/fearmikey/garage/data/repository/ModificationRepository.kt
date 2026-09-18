package com.fearmikey.garage.data.repository

import android.net.Uri
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.entity.ModificationRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModificationRepository @Inject constructor(
    private val modificationDao: ModificationDao,
    private val imageStorageManager: ImageStorageManager,
) {
    fun getModsForVehicle(vehicleId: Long): Flow<List<ModificationRecord>> =
        modificationDao.getModsForVehicle(vehicleId)

    suspend fun getModById(id: Long): ModificationRecord? =
        modificationDao.getModById(id)

    /**
     * Saves or updates a modification record.
     *
     * If [newPickedUri] is provided, the image is copied into app storage and
     * the new filename is saved on the record (cleaning up any old photo).
     * If [deleteExistingImage] is true, any existing photo file is deleted and
     * [ModificationRecord.imageUri] is set to null.
     */
    suspend fun saveMod(
        mod: ModificationRecord,
        newPickedUri: Uri? = null,
        deleteExistingImage: Boolean = false,
    ): Long {
        val finalMod = when {
            newPickedUri != null -> {
                val newFilename = imageStorageManager.copyPickedImageToInternalStorage(newPickedUri)
                mod.imageUri?.let { oldFilename ->
                    if (oldFilename != newFilename) {
                        imageStorageManager.deleteImage(oldFilename)
                    }
                }
                mod.copy(imageUri = newFilename)
            }
            deleteExistingImage -> {
                mod.imageUri?.let { imageStorageManager.deleteImage(it) }
                mod.copy(imageUri = null)
            }
            else -> mod
        }
        return modificationDao.upsert(finalMod)
    }

    suspend fun deleteMod(mod: ModificationRecord) {
        mod.imageUri?.let { imageStorageManager.deleteImage(it) }
        modificationDao.delete(mod)
    }
}
