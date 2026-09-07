package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMaintenanceRuleDao {
    @Query("SELECT * FROM custom_maintenance_rules WHERE vehicleId = :vehicleId ORDER BY taskName")
    fun getForVehicle(vehicleId: Long): Flow<List<CustomMaintenanceRule>>

    @Upsert
    suspend fun upsert(rule: CustomMaintenanceRule): Long

    @Delete
    suspend fun delete(rule: CustomMaintenanceRule)
}
