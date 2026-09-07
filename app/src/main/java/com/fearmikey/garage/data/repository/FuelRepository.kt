package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.entity.FuelRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(
    private val fuelDao: FuelDao,
) {
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> =
        fuelDao.getRecordsForVehicle(vehicleId)

    suspend fun saveRecord(record: FuelRecord): Long = fuelDao.upsert(record)

    suspend fun deleteRecord(record: FuelRecord) = fuelDao.delete(record)
}
