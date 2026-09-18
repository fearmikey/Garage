package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.ModificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ModificationDao {
    @Query("SELECT * FROM modification_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getModsForVehicle(vehicleId: Long): Flow<List<ModificationRecord>>

    @Query("SELECT * FROM modification_records WHERE id = :id")
    suspend fun getModById(id: Long): ModificationRecord?

    @Upsert
    suspend fun upsert(mod: ModificationRecord): Long

    @Update
    suspend fun update(mod: ModificationRecord)

    @Delete
    suspend fun delete(mod: ModificationRecord)
}
