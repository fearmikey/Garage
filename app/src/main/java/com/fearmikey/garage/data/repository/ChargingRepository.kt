package com.fearmikey.garage.data.repository

import androidx.room.withTransaction
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.ChargingDao
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingRepository @Inject constructor(
    private val chargingDao: ChargingDao,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao? = null,
    private val database: GarageDatabase? = null,
) {
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<ChargingRecord>> =
        chargingDao.getRecordsForVehicle(vehicleId)

    suspend fun saveRecord(record: ChargingRecord): Long = chargingDao.upsert(record)

    suspend fun deleteRecord(record: ChargingRecord) {
        if (database == null || pendingDeleteDao == null) {
            chargingDao.delete(record)
            return
        }
        database.withTransaction {
            if (record.lubeLoggerId != null) {
                pendingDeleteDao.insert(
                    LubeLoggerPendingDelete(
                        type = LubeLoggerRecordType.FUEL,
                        lubeLoggerId = record.lubeLoggerId,
                        lubeLoggerVehicleId = record.vehicleId
                    )
                )
            }
            chargingDao.delete(record)
        }
    }

    /** Deletes a charging record that was already deleted in LubeLogger, without queuing a server delete. */
    suspend fun deleteRecordFromSync(record: ChargingRecord) = chargingDao.delete(record)
}
