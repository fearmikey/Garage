package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleSpecsDao {
    @Query("SELECT * FROM vehicle_specs WHERE vehicleId = :vehicleId")
    fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?>

    @Upsert
    suspend fun upsert(specs: VehicleSpecs)
}
