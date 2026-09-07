package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomMaintenanceRuleRepository @Inject constructor(
    private val dao: CustomMaintenanceRuleDao,
) {
    fun getRulesForVehicle(vehicleId: Long): Flow<List<CustomMaintenanceRule>> =
        dao.getForVehicle(vehicleId)

    suspend fun saveRule(rule: CustomMaintenanceRule): Long = dao.upsert(rule)

    suspend fun deleteRule(rule: CustomMaintenanceRule) = dao.delete(rule)
}
