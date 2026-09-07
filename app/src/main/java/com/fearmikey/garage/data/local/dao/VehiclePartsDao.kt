package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePartsDao {
    @Query("SELECT * FROM vehicle_parts_info WHERE vehicleId = :vehicleId")
    fun getByVehicleId(vehicleId: Long): Flow<VehiclePartsInfo?>

    @Upsert
    suspend fun upsert(info: VehiclePartsInfo)
}
