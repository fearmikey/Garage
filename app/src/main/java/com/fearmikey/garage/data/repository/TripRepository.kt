package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.TripDao
import com.fearmikey.garage.data.local.entity.TripRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val dao: TripDao
) {
    fun getTripsForVehicle(vehicleId: Long): Flow<List<TripRecord>> = dao.getTripsForVehicle(vehicleId)
    
    suspend fun insertTrip(trip: TripRecord) = dao.insertTrip(trip)
    
    suspend fun updateTrip(trip: TripRecord) = dao.updateTrip(trip)
    
    suspend fun deleteTrip(trip: TripRecord) = dao.deleteTrip(trip)
}
