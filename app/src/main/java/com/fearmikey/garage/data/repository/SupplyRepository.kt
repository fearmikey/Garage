package com.fearmikey.garage.data.repository

import androidx.room.withTransaction
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.dao.SupplyDao
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.SupplyRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies shown on the "Parts & Supplies" tab; synced with LubeLogger by LubeLoggerSyncWorker. */
@Singleton
class SupplyRepository @Inject constructor(
    private val supplyDao: SupplyDao,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao,
    private val database: GarageDatabase,
) {
    fun getSuppliesForVehicle(vehicleId: Long): Flow<List<SupplyRecord>> =
        supplyDao.getSuppliesForVehicle(vehicleId)

    suspend fun saveSupply(supply: SupplyRecord): Long = supplyDao.upsert(supply)

    /** Deletes a supply and, if it was synced, queues the matching LubeLogger delete. */
    suspend fun deleteSupply(supply: SupplyRecord) {
        database.withTransaction {
            supply.lubeLoggerId?.let { remoteId ->
                pendingDeleteDao.insert(
                    LubeLoggerPendingDelete(
                        type = LubeLoggerRecordType.SUPPLY,
                        lubeLoggerId = remoteId,
                        lubeLoggerVehicleId = supply.vehicleId,
                    ),
                )
            }
            supplyDao.delete(supply)
        }
    }

    /** Deletes a supply that was already deleted in LubeLogger, without queuing a server delete. */
    suspend fun deleteSupplyFromSync(supply: SupplyRecord) = supplyDao.delete(supply)
}
