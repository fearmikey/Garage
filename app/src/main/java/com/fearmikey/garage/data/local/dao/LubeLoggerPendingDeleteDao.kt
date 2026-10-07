package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import kotlinx.coroutines.flow.Flow

@Dao
interface LubeLoggerPendingDeleteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDelete: LubeLoggerPendingDelete)

    @Query("SELECT * FROM lubelogger_pending_deletes WHERE lubeLoggerVehicleId = :vehicleId")
    fun getPendingDeletesForVehicle(vehicleId: Long): Flow<List<LubeLoggerPendingDelete>>

    @Query("SELECT * FROM lubelogger_pending_deletes WHERE lubeLoggerVehicleId = :vehicleId")
    suspend fun getPendingDeletesForVehicleSync(vehicleId: Long): List<LubeLoggerPendingDelete>

    @Query("DELETE FROM lubelogger_pending_deletes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM lubelogger_pending_deletes WHERE lubeLoggerVehicleId = :vehicleId AND type = :type AND lubeLoggerId = :lubeLoggerId")
    suspend fun deleteByRecord(vehicleId: Long, type: LubeLoggerRecordType, lubeLoggerId: Int)
}
