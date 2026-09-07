package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleSpecsDao {
    @Query("SELECT * FROM vehicle_specs WHERE vehicleId = :vehicleId")
    fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(specs: VehicleSpecs)
}
