package com.fearmikey.garage.data.repository

import android.net.Uri
import com.fearmikey.garage.data.local.dao.CustomDocumentDao
import com.fearmikey.garage.data.local.entity.CustomDocument
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** User-named vehicle documents (warranty, roadside assistance, title, loan...). */
@Singleton
class CustomDocumentRepository @Inject constructor(
    private val dao: CustomDocumentDao,
    private val imageStorageManager: ImageStorageManager,
) {
    fun getDocumentsForVehicle(vehicleId: Long): Flow<List<CustomDocument>> = dao.getForVehicle(vehicleId)

    /**
     * Saves [document], copying a newly picked attachment (image or PDF) into app storage first.
     * When [pickedAttachment] is set or [removeAttachment] is true, the previous attachment file
     * is deleted.
     */
    suspend fun saveDocument(
        document: CustomDocument,
        pickedAttachment: Uri? = null,
        removeAttachment: Boolean = false,
    ) {
        val previous = document.imageUri
        val newFilename = when {
            pickedAttachment != null -> imageStorageManager.copyPickedFileToInternalStorage(pickedAttachment)
            removeAttachment -> null
            else -> previous
        }
        dao.upsert(document.copy(imageUri = newFilename))
        if ((previous != null) && (previous != newFilename)) {
            imageStorageManager.deleteImage(previous)
        }
    }

    suspend fun deleteDocument(document: CustomDocument) {
        dao.delete(document)
        document.imageUri?.let(imageStorageManager::deleteImage)
    }

    fun attachmentFile(filename: String): File = imageStorageManager.imageFile(filename)
}
