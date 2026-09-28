package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fearmikey.garage.data.local.entity.IgnoredMaintenanceRule
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredMaintenanceRuleDao {
    @Query("SELECT * FROM ignored_maintenance_rules WHERE vehicleId = :vehicleId")
    fun getIgnoredRulesForVehicle(vehicleId: Long): Flow<List<IgnoredMaintenanceRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ignoreRule(rule: IgnoredMaintenanceRule)

    @Query("DELETE FROM ignored_maintenance_rules WHERE vehicleId = :vehicleId AND LOWER(taskName) = LOWER(:taskName)")
    suspend fun unignoreRule(vehicleId: Long, taskName: String)
}
