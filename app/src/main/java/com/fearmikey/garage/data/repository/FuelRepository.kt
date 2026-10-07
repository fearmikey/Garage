package com.fearmikey.garage.data.repository

import androidx.room.withTransaction
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(
    private val fuelDao: FuelDao,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao? = null,
    private val database: GarageDatabase? = null,
) {
    fun getRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> =
        fuelDao.getRecordsForVehicle(vehicleId)

    suspend fun saveRecord(record: FuelRecord): Long = fuelDao.upsert(record)

    suspend fun deleteRecord(record: FuelRecord) {
        if (database == null || pendingDeleteDao == null) {
            fuelDao.delete(record)
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
            fuelDao.delete(record)
        }
    }

    /** Deletes a record that was already deleted in LubeLogger, without queuing a server delete. */
    suspend fun deleteRecordFromSync(record: FuelRecord) = fuelDao.delete(record)
}
