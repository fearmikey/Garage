package com.fearmikey.garage.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.ModificationRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModificationRepository @Inject constructor(
    private val modificationDao: ModificationDao,
    private val imageStorageManager: ImageStorageManager,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao? = null,
    private val database: GarageDatabase? = null,
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
        if (database == null || pendingDeleteDao == null) {
            deleteModFromSync(mod)
            return
        }
        database.withTransaction {
            if (mod.lubeLoggerId != null) {
                pendingDeleteDao.insert(
                    LubeLoggerPendingDelete(
                        type = LubeLoggerRecordType.UPGRADE,
                        lubeLoggerId = mod.lubeLoggerId,
                        lubeLoggerVehicleId = mod.vehicleId
                    )
                )
            }
            modificationDao.delete(mod)
        }
        mod.imageUris.forEach { imageStorageManager.deleteImage(it) }
    }

    /** Deletes a modification that was already deleted in LubeLogger, without queuing a server delete. */
    suspend fun deleteModFromSync(mod: ModificationRecord) {
        modificationDao.delete(mod)
        mod.imageUris.forEach { imageStorageManager.deleteImage(it) }
    }
}
