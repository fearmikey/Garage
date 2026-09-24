package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.ChargingDao
import com.fearmikey.garage.data.local.entity.ChargingRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingRepository @Inject constructor(
    private val chargingDao: ChargingDao,
) {
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<ChargingRecord>> =
        chargingDao.getRecordsForVehicle(vehicleId)

    suspend fun saveRecord(record: ChargingRecord): Long = chargingDao.upsert(record)

    suspend fun deleteRecord(record: ChargingRecord) = chargingDao.delete(record)
}
