package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class MaintenanceSortOrder { DATE, MILEAGE }

@Singleton
class MaintenanceRepository @Inject constructor(
    private val maintenanceDao: MaintenanceDao,
) {
    fun getRecordsForVehicle(
        vehicleId: Long,
        sortOrder: MaintenanceSortOrder = MaintenanceSortOrder.DATE,
    ): Flow<List<MaintenanceRecord>> = when (sortOrder) {
        MaintenanceSortOrder.DATE -> maintenanceDao.getRecordsForVehicleByDate(vehicleId)
        MaintenanceSortOrder.MILEAGE -> maintenanceDao.getRecordsForVehicleByMileage(vehicleId)
    }

    fun getLatestMileageForVehicle(vehicleId: Long): Flow<Int?> =
        maintenanceDao.getLatestMileageForVehicle(vehicleId)

    suspend fun saveRecord(record: MaintenanceRecord): Long = maintenanceDao.upsert(record)

    suspend fun deleteRecord(record: MaintenanceRecord) = maintenanceDao.delete(record)
}
