package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fearmikey.garage.data.local.entity.TripRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_records WHERE vehicleId = :vehicleId ORDER BY startDate DESC")
    fun getTripsForVehicle(vehicleId: Long): Flow<List<TripRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripRecord): Long

    @Update
    suspend fun updateTrip(trip: TripRecord)

    @Delete
    suspend fun deleteTrip(trip: TripRecord)
}
