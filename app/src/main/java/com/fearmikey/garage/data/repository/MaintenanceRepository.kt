package com.fearmikey.garage.data.repository

import android.net.Uri
import com.fearmikey.garage.data.local.dao.IgnoredMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.entity.IgnoredMaintenanceRule
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class MaintenanceSortOrder { DATE, MILEAGE }

@Singleton
class MaintenanceRepository @Inject constructor(
    private val maintenanceDao: MaintenanceDao,
    private val ignoredMaintenanceRuleDao: IgnoredMaintenanceRuleDao? = null,
    private val imageStorageManager: ImageStorageManager? = null,
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

    fun getIgnoredRulesForVehicle(vehicleId: Long): Flow<List<IgnoredMaintenanceRule>> =
        ignoredMaintenanceRuleDao?.getIgnoredRulesForVehicle(vehicleId) ?: flowOf(emptyList())

    suspend fun ignoreRule(vehicleId: Long, taskName: String) {
        ignoredMaintenanceRuleDao?.ignoreRule(IgnoredMaintenanceRule(vehicleId = vehicleId, taskName = taskName))
    }

    suspend fun unignoreRule(vehicleId: Long, taskName: String) {
        ignoredMaintenanceRuleDao?.unignoreRule(vehicleId, taskName)
    }

    fun imageFileFor(filename: String): File? = imageStorageManager?.imageFile(filename)

    suspend fun saveRecord(
        record: MaintenanceRecord,
        newPickedReceiptUri: Uri? = null,
        deleteExistingReceipt: Boolean = false,
    ): Long {
        val finalRecord = when {
            (newPickedReceiptUri != null) && (imageStorageManager != null) -> {
                val newFilename = imageStorageManager.copyPickedFileToInternalStorage(newPickedReceiptUri)
                record.receiptUri?.let { oldFilename ->
                    if (oldFilename != newFilename) {
                        imageStorageManager.deleteImage(oldFilename)
                    }
                }
                record.copy(receiptUri = newFilename)
            }
            deleteExistingReceipt -> {
                record.receiptUri?.let { imageStorageManager?.deleteImage(it) }
                record.copy(receiptUri = null)
            }
            else -> record
        }
        return maintenanceDao.upsert(finalRecord)
    }

    suspend fun deleteRecord(record: MaintenanceRecord) {
        record.receiptUri?.let { imageStorageManager?.deleteImage(it) }
        maintenanceDao.delete(record)
    }
}
