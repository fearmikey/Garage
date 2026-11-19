package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.SupplyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyDao {
    @Query("SELECT * FROM supply_records WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun getSuppliesForVehicle(vehicleId: Long): Flow<List<SupplyRecord>>

    @Upsert
    suspend fun upsert(supply: SupplyRecord): Long

    @Delete
    suspend fun delete(supply: SupplyRecord)
}
