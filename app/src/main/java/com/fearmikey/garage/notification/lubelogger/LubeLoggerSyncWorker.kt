package com.fearmikey.garage.notification.lubelogger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerApiFactory
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerDto
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class LubeLoggerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiFactory: LubeLoggerApiFactory,
    private val credentialsManager: LubeLoggerCredentialsManager,
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val maintenanceRepository: MaintenanceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!credentialsManager.isConfigured()) {
            return Result.success()
        }

        val api = apiFactory.createApiService() ?: return Result.failure()

        try {
            // 1. Fetch remote vehicles to build/update mappings
            val vehiclesResponse = api.getVehicles()
            if (!vehiclesResponse.isSuccessful) return Result.retry()
            
            val remoteVehicles = vehiclesResponse.body() ?: emptyList()
            val localVehicles = vehicleRepository.getAllVehicles().first()

            for (local in localVehicles) {
                var mappedId = credentialsManager.getVehicleMapping(local.id)
                if (mappedId == null) {
                    // Try to map by matching properties
                    val match = remoteVehicles.find {
                        it.year == local.year &&
                        it.make?.equals(local.make, ignoreCase = true) == true &&
                        it.model?.equals(local.model, ignoreCase = true) == true
                    }
                    if (match != null) {
                        mappedId = match.id
                        credentialsManager.saveVehicleMapping(local.id, mappedId)
                    }
                }

                // If mapped, we can push records
                if (mappedId != null) {
                    // Push fuel records
                    val fuelRecords = fuelRepository.getRecordsForVehicle(local.id).first()
                    // Push them all (MVP: LubeLogger allows pushing, we ignore duplicates for now or rely on its internal dedupe)
                    // Note: This MVP continuously pushes everything. 
                    // In a more robust system we would track exactly which IDs have been synced.
                    fuelRecords.forEach { fuelRecord ->
                        val gasDto = fuelRecord.toLubeLoggerDto(mappedId)
                        api.addGasRecord(gasDto)
                    }

                    // Push maintenance records
                    val maintenanceRecords = maintenanceRepository.getRecordsForVehicle(local.id).first()
                    maintenanceRecords.forEach { maintRecord ->
                        val maintDto = maintRecord.toLubeLoggerDto(mappedId)
                        api.addServiceRecord(maintDto)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
