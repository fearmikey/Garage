package com.fearmikey.garage.notification.lubelogger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerApiFactory
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerVehicleImportDto
import com.fearmikey.garage.data.remote.lubelogger.toFuelRecord
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerDto
import com.fearmikey.garage.data.remote.lubelogger.toMaintenanceRecord
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
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
    private val maintenanceRepository: MaintenanceRepository,
    private val imageStorageManager: ImageStorageManager,
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
            
            var remoteVehicles = vehiclesResponse.body() ?: emptyList()
            var localVehicles = vehicleRepository.getAllVehicles().first()

            // A) PUSH LOCAL VEHICLES TO LUBELOGGER IF NOT MAPPED / NOT MATCHED
            for (local in localVehicles) {
                var mappedId = credentialsManager.getVehicleMapping(local.id)
                if (mappedId == null) {
                    val match = remoteVehicles.find {
                        it.year == local.year &&
                        it.make?.equals(local.make, ignoreCase = true) == true &&
                        it.model?.equals(local.model, ignoreCase = true) == true
                    }
                    if (match != null) {
                        mappedId = match.id
                        credentialsManager.saveVehicleMapping(local.id, mappedId)
                    } else if (local.year != null && local.make.isNotBlank() && local.model.isNotBlank()) {
                        // Push new vehicle to LubeLogger
                        val importDto = LubeLoggerVehicleImportDto(
                            year = local.year.toString(),
                            make = local.make,
                            model = local.model,
                            licensePlate = local.vin.ifBlank { "N/A" },
                        )
                        val createResp = api.addVehicle(importDto)
                        if (createResp.isSuccessful) {
                            val newRemoteId = createResp.body()?.additionalData?.recordId
                            if (newRemoteId != null) {
                                mappedId = newRemoteId
                                credentialsManager.saveVehicleMapping(local.id, newRemoteId)
                            }
                        }
                    }
                }
            }

            // Refetch remote and local vehicles after auto-creation
            val updatedRemoteVehiclesResponse = api.getVehicles()
            if (updatedRemoteVehiclesResponse.isSuccessful) {
                remoteVehicles = updatedRemoteVehiclesResponse.body() ?: emptyList()
            }
            localVehicles = vehicleRepository.getAllVehicles().first()

            // B) PULL REMOTE VEHICLES FROM LUBELOGGER IF NOT IN GARAGE
            for (remote in remoteVehicles) {
                val matchesLocal = localVehicles.any { local ->
                    val mappedId = credentialsManager.getVehicleMapping(local.id)
                    mappedId == remote.id || (
                        remote.year == local.year &&
                        remote.make?.equals(local.make, ignoreCase = true) == true &&
                        remote.model?.equals(local.model, ignoreCase = true) == true
                    )
                }

                if (!matchesLocal && remote.year != null && !remote.make.isNullOrBlank() && !remote.model.isNullOrBlank()) {
                    // Create local vehicle in Garage
                    val newVehicle = com.fearmikey.garage.data.local.entity.Vehicle(
                        year = remote.year,
                        make = remote.make,
                        model = remote.model,
                        trim = ""
                    )
                    val newLocalId = vehicleRepository.saveVehicle(newVehicle)
                    credentialsManager.saveVehicleMapping(newLocalId, remote.id)
                }
            }

            // Refetch local vehicles to ensure all mapped vehicles participate in record sync
            localVehicles = vehicleRepository.getAllVehicles().first()

            val unitSystem = credentialsManager.getUnitSystem()

            for (local in localVehicles) {
                val mappedId = credentialsManager.getVehicleMapping(local.id)
                if (mappedId != null) {
                    val remoteVehicle = remoteVehicles.find { it.id == mappedId }

                    // Sync Vehicle Image if local imageUri is empty and remote has an imageLocation
                    if (local.imageUri == null && remoteVehicle?.imageLocation != null && !remoteVehicle.imageLocation.contains("noimage.png")) {
                        try {
                            val imgResp = api.downloadFile(remoteVehicle.imageLocation)
                            if (imgResp.isSuccessful) {
                                val bytes = imgResp.body()?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    val savedFilename = imageStorageManager.saveImageBytesToInternalStorage(bytes)
                                    vehicleRepository.saveVehicle(local.copy(imageUri = savedFilename))
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // ---------------------------------------------------------
                    // PULL REMOTE RECORDS & UPDATE LOCAL DATABASE
                    // ---------------------------------------------------------
                    val remoteGasRecordsResponse = api.getGasRecords(mappedId)
                    if (remoteGasRecordsResponse.isSuccessful) {
                        val remoteGasRecords = remoteGasRecordsResponse.body() ?: emptyList()
                        val localFuelRecords = fuelRepository.getRecordsForVehicle(local.id).first()

                        for (remoteGas in remoteGasRecords) {
                            if (remoteGas.id != null) {
                                val linkedLocally = localFuelRecords.find { it.lubeLoggerId == remoteGas.id }
                                if (linkedLocally == null) {
                                    val candidate = remoteGas.toFuelRecord(local.id, unitSystem)
                                    if (candidate != null) {
                                        // Match unlinked local records by mileage (within 1 mi/km) and date (within 24 hours)
                                        val unlinkedMatch = localFuelRecords.find { localRec ->
                                            localRec.lubeLoggerId == null &&
                                            kotlin.math.abs(localRec.mileage - candidate.mileage) <= 1 &&
                                            kotlin.math.abs(localRec.date - candidate.date) < 24 * 3600 * 1000L
                                        }

                                        if (unlinkedMatch != null) {
                                            fuelRepository.saveRecord(unlinkedMatch.copy(lubeLoggerId = remoteGas.id))
                                        } else {
                                            fuelRepository.saveRecord(candidate)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val remoteServiceRecordsResponse = api.getServiceRecords(mappedId)
                    if (remoteServiceRecordsResponse.isSuccessful) {
                        val remoteServiceRecords = remoteServiceRecordsResponse.body() ?: emptyList()
                        val localMaintRecords = maintenanceRepository.getRecordsForVehicle(local.id).first()

                        for (remoteService in remoteServiceRecords) {
                            if (remoteService.id != null) {
                                val linkedLocally = localMaintRecords.find { it.lubeLoggerId == remoteService.id }
                                if (linkedLocally == null) {
                                    val candidate = remoteService.toMaintenanceRecord(local.id, unitSystem)
                                    if (candidate != null) {
                                        // Match unlinked local records by mileage (within 1 mi/km) and date (within 24 hours)
                                        val unlinkedMatch = localMaintRecords.find { localRec ->
                                            localRec.lubeLoggerId == null &&
                                            kotlin.math.abs(localRec.mileage - candidate.mileage) <= 1 &&
                                            kotlin.math.abs(localRec.date - candidate.date) < 24 * 3600 * 1000L
                                        }

                                        if (unlinkedMatch != null) {
                                            maintenanceRepository.saveRecord(unlinkedMatch.copy(lubeLoggerId = remoteService.id))
                                        } else {
                                            maintenanceRepository.saveRecord(candidate)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // PUSH NEW LOCAL RECORDS
                    // ---------------------------------------------------------
                    val fuelRecords = fuelRepository.getRecordsForVehicle(local.id).first()
                    // Push only un-synced records
                    val newFuelRecords = fuelRecords.filter { it.lubeLoggerId == null }
                    newFuelRecords.forEach { fuelRecord ->
                        val gasDto = fuelRecord.toLubeLoggerDto(mappedId, unitSystem)
                        val response = api.addGasRecord(mappedId, gasDto)
                        if (response.isSuccessful) {
                            val returnedId = response.body()?.additionalData?.recordId
                            if (returnedId != null) {
                                fuelRepository.saveRecord(fuelRecord.copy(lubeLoggerId = returnedId))
                            }
                        }
                    }

                    val maintenanceRecords = maintenanceRepository.getRecordsForVehicle(local.id).first()
                    // Push only un-synced records
                    val newMaintRecords = maintenanceRecords.filter { it.lubeLoggerId == null }
                    newMaintRecords.forEach { maintRecord ->
                        val maintDto = maintRecord.toLubeLoggerDto(mappedId, unitSystem)
                        val response = api.addServiceRecord(mappedId, maintDto)
                        if (response.isSuccessful) {
                            val returnedId = response.body()?.additionalData?.recordId
                            if (returnedId != null) {
                                maintenanceRepository.saveRecord(maintRecord.copy(lubeLoggerId = returnedId))
                            }
                        }
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
