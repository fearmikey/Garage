package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRecordsForVehicleByDate(vehicleId: Long): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY mileage DESC")
    fun getRecordsForVehicleByMileage(vehicleId: Long): Flow<List<MaintenanceRecord>>

    /** The highest recorded mileage for a vehicle, i.e. its last known odometer reading. */
    @Query("SELECT MAX(mileage) FROM maintenance_records WHERE vehicleId = :vehicleId")
    fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: MaintenanceRecord): Long

    @Update
    suspend fun update(record: MaintenanceRecord)

    @Delete
    suspend fun delete(record: MaintenanceRecord)
}
