package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.CustomDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomDocumentDao {
    @Query("SELECT * FROM custom_documents WHERE vehicleId = :vehicleId ORDER BY title COLLATE NOCASE")
    fun getForVehicle(vehicleId: Long): Flow<List<CustomDocument>>

    @Upsert
    suspend fun upsert(document: CustomDocument): Long

    @Delete
    suspend fun delete(document: CustomDocument)
}
