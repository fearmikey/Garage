package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleRegistrationDao {
    @Query("SELECT * FROM vehicle_registration_insurance WHERE vehicleId = :vehicleId")
    fun getByVehicleId(vehicleId: Long): Flow<VehicleRegistrationInsurance?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance)

    @Query("DELETE FROM vehicle_registration_insurance WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicleId(vehicleId: Long)
}
