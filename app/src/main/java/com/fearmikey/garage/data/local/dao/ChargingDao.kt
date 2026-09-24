package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.ChargingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingDao {
    @Query("SELECT * FROM charging_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<ChargingRecord>>

    @Upsert
    suspend fun upsert(record: ChargingRecord): Long

    @Update
    suspend fun update(record: ChargingRecord)

    @Delete
    suspend fun delete(record: ChargingRecord)
}
