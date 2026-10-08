package com.fearmikey.garage.notification.lubelogger

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerApiFactory
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerApiService
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerCredentialsManager
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerOperationResponse
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerVehicleDto
import com.fearmikey.garage.data.remote.lubelogger.LubeLoggerVehicleImportDto
import com.fearmikey.garage.data.remote.lubelogger.cleanLegacyTaskSuffix
import com.fearmikey.garage.data.remote.lubelogger.createLubeLoggerOdometerDto
import com.fearmikey.garage.data.remote.lubelogger.describeLubeLoggerFailure
import com.fearmikey.garage.data.remote.lubelogger.describeLubeLoggerHttpFailure
import com.fearmikey.garage.data.remote.lubelogger.flattenJsonToFormFields
import com.fearmikey.garage.data.remote.lubelogger.isOdometerCheckIn
import com.fearmikey.garage.data.remote.lubelogger.mergeVehicleSyncDetails
import com.fearmikey.garage.data.remote.lubelogger.parseLubeLoggerInt
import com.fearmikey.garage.data.remote.lubelogger.syncDetails
import com.fearmikey.garage.data.remote.lubelogger.syncFingerprint
import com.fearmikey.garage.data.remote.lubelogger.syncLubeLoggerRecords
import com.fearmikey.garage.data.remote.lubelogger.syncablePlate
import com.fearmikey.garage.data.remote.lubelogger.toChargingRecord
import com.fearmikey.garage.data.remote.lubelogger.toFuelRecord
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerDto
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerOdometerDto
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerRepairDto
import com.fearmikey.garage.data.remote.lubelogger.toLubeLoggerUpgradeDto
import com.fearmikey.garage.data.remote.lubelogger.toMaintenanceRecord
import com.fearmikey.garage.data.remote.lubelogger.toModificationRecord
import com.fearmikey.garage.data.remote.lubelogger.toOdometerCheckIn
import com.fearmikey.garage.data.remote.lubelogger.toUpdateDto
import com.fearmikey.garage.data.remote.lubelogger.withSyncDetails
import com.fearmikey.garage.data.repository.ChargingRepository
import com.fearmikey.garage.data.repository.FuelRepository
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.ModificationRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import kotlin.math.abs

@HiltWorker
class LubeLoggerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiFactory: LubeLoggerApiFactory,
    private val credentialsManager: LubeLoggerCredentialsManager,
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val chargingRepository: ChargingRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val modificationRepository: ModificationRepository,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao,
    private val registrationDao: VehicleRegistrationDao,
    private val imageStorageManager: ImageStorageManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!credentialsManager.isConfigured()) {
            return Result.success()
        }

        val api = apiFactory.createApiService()
            ?: return fail("The LubeLogger server URL is invalid.", retry = false)

        // Several triggers (periodic, immediate, auto-backup, "Sync now") can start this worker at
        // the same time; running them concurrently pushes the same unsynced record more than once.
        return syncMutex.withLock {
            credentialsManager.markSyncStarted()
            sync(api)
        }
    }

    /** Records a failed sync so Settings can show it, and returns the matching worker result. */
    private fun fail(message: String, retry: Boolean): Result {
        credentialsManager.markSyncFailed(message)
        val data = workDataOf(KEY_ERROR to message)
        // Retrying a manual "Sync now" leaves the button spinning for minutes; fail it right away
        // instead. Periodic syncs still retry with backoff.
        return if (retry && !tags.contains(TAG_MANUAL)) Result.retry() else Result.failure(data)
    }

    private suspend fun sync(api: LubeLoggerApiService): Result {
        try {
            // 1. Fetch remote vehicles to build/update mappings
            val vehiclesResponse = api.getVehicles()
            if (vehiclesResponse.code() == 401 || vehiclesResponse.code() == 403) {
                // Bad credentials won't fix themselves; retrying just hammers the server.
                Log.w(TAG, "LubeLogger rejected credentials (HTTP ${vehiclesResponse.code()})")
                return fail(describeLubeLoggerHttpFailure(vehiclesResponse.code()), retry = false)
            }
            if (!vehiclesResponse.isSuccessful) {
                return fail(describeLubeLoggerHttpFailure(vehiclesResponse.code()), retry = true)
            }

            var remoteVehicles = vehiclesResponse.body() ?: emptyList()
            var localVehicles = vehicleRepository.getAllVehicles().first()

            // A) PUSH LOCAL VEHICLES TO LUBELOGGER IF NOT MAPPED / NOT MATCHED
            for (local in localVehicles) {
                if (credentialsManager.getVehicleMapping(local.id) != null) continue

                // Check if any unmapped remote vehicle matches by unique VIN or License Plate
                val match = remoteVehicles.find { remote ->
                    val isRemoteMapped = localVehicles.any { credentialsManager.getVehicleMapping(it.id) == remote.id }
                    !isRemoteMapped && vehiclesMatch(local, remote)
                }

                if (match != null) {
                    credentialsManager.saveVehicleMapping(local.id, match.id)
                } else if (local.year != null && local.make.isNotBlank() && local.model.isNotBlank()) {
                    val localRegistration = registrationDao.getByVehicleId(local.id).first()
                    val plateToUse = localRegistration?.licensePlate?.trim()?.takeIf { it.isNotBlank() }
                        ?: local.vin.trim().takeIf { it.isNotBlank() }
                        ?: "N/A"

                    val importDto = LubeLoggerVehicleImportDto(
                        year = local.year.toString(),
                        make = local.make,
                        model = local.model,
                        licensePlate = plateToUse,
                    )
                    val createResp = api.addVehicle(importDto)
                    if (createResp.isSuccessful) {
                        createResp.body()?.additionalData?.recordId?.let { newRemoteId ->
                            credentialsManager.saveVehicleMapping(local.id, newRemoteId)
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
                val isMapped = localVehicles.any { local ->
                    credentialsManager.getVehicleMapping(local.id) == remote.id
                }

                if (!isMapped && remote.year != null && !remote.make.isNullOrBlank() && !remote.model.isNullOrBlank()) {
                    val remoteVin = remote.findVin().orEmpty()
                    val remoteTrim = remote.matchingExtraFields(
                        com.fearmikey.garage.data.remote.lubelogger.VehicleSyncDetails.TRIM_FIELD_NAME
                    ).firstOrNull()?.value?.trim().orEmpty()
                    val remotePlate = remote.syncablePlate(remoteVin.ifBlank { null })?.trim().orEmpty()
                    val remotePurchasedNew = remote.findPurchasedNew() ?: false
                    val remoteInitialMileage = remote.findPurchaseOdometer()

                    val newVehicle = Vehicle(
                        year = remote.year,
                        make = remote.make.orEmpty(),
                        model = remote.model.orEmpty(),
                        trim = remoteTrim,
                        vin = remoteVin,
                        purchasedNew = remotePurchasedNew,
                        initialMileage = remoteInitialMileage,
                    )
                    val newLocalId = vehicleRepository.saveVehicle(newVehicle)
                    credentialsManager.saveVehicleMapping(newLocalId, remote.id)

                    if (remotePlate.isNotBlank()) {
                        registrationDao.upsert(
                            VehicleRegistrationInsurance(
                                vehicleId = newLocalId,
                                licensePlate = remotePlate,
                            )
                        )
                    }
                }
            }

            // Refetch local vehicles to ensure all mapped vehicles participate in record sync
            localVehicles = vehicleRepository.getAllVehicles().first()

            val unitSystem = credentialsManager.getUnitSystem()

            for (mappedLocal in localVehicles) {
                val mappedId = credentialsManager.getVehicleMapping(mappedLocal.id) ?: continue
                val remoteVehicle = remoteVehicles.find { it.id == mappedId }

                // Two-way sync of VIN / purchase condition / purchase mileage.
                val local = remoteVehicle?.let { syncVehicleDetails(api, mappedLocal, it, unitSystem) } ?: mappedLocal

                syncVehicleImage(api, local, mappedId, remoteVehicle)

                cleanUpLegacyMaintenanceRecords(local.id)
                moveRecordsBetweenServiceAndRepair(api, local.id)

                val pendingDeletes = pendingDeleteDao.getPendingDeletesForVehicleSync(local.id)
                if (remoteVehicle?.isElectric == true || local.isPureEv()) {
                    syncChargingRecords(api, local.id, mappedId, unitSystem, pendingDeletes)
                } else {
                    syncGasRecords(api, local.id, mappedId, unitSystem, pendingDeletes)
                }
                syncServiceRecords(api, local.id, mappedId, unitSystem, pendingDeletes)
                syncRepairRecords(api, local.id, mappedId, unitSystem, pendingDeletes)
                syncUpgradeRecords(api, local.id, mappedId, pendingDeletes)
                syncOdometerRecords(api, local.id, mappedId, unitSystem, pendingDeletes)
            }
            credentialsManager.markSyncSucceeded()
            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // A newer sync request replaced this one; unsynced records are reconciled next run.
            credentialsManager.markSyncCancelled()
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "LubeLogger sync failed", e)
            return fail(describeLubeLoggerFailure(e), retry = true)
        }
    }

    // ---------------------------------------------------------------------
    // Per-type record sync (see LubeLoggerRecordSync.kt for the three-way merge)
    // ---------------------------------------------------------------------

    private suspend fun syncGasRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        unitSystem: String,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        // Only sync when the server list loaded; an error must never look like "everything was deleted".
        val remoteRecords = api.getGasRecords(mappedId).bodyIfSuccessful()?.filter { (it.id ?: 0) > 0 } ?: return
        val localRecords = fuelRepository.getRecordsForVehicle(localVehicleId).first()

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.FUEL },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { it.id ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toFuelRecord(localVehicleId, unitSystem)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerDto(mappedId, unitSystem) },
            toLocal = { dto, existing -> dto.toFuelRecord(localVehicleId, unitSystem, existing) },
            isSameReadingAs = { local, remote ->
                isSameFuelReading(local.mileage, local.date, remote.toFuelRecord(localVehicleId, unitSystem))
            },
            addLocal = { fuelRepository.saveRecord(it) },
            updateLocal = { fuelRepository.saveRecord(it) },
            deleteLocal = { fuelRepository.deleteRecordFromSync(it) },
            addRemote = { api.addGasRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateGasRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteGasRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash -> local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash) },
            onDeletesSkipped = ::logSkippedDeletes,
        )
    }

    private suspend fun syncServiceRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        unitSystem: String,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        val remoteRecords = api.getServiceRecords(mappedId).bodyIfSuccessful()?.filter { (it.id ?: 0) > 0 } ?: return
        val localRecords = maintenanceRepository.getRecordsForVehicle(localVehicleId).first()
            .filter { it.effectiveRecordType == LubeLoggerRecordType.SERVICE }

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.SERVICE },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { it.id ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toMaintenanceRecord(localVehicleId, unitSystem)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerDto(mappedId, unitSystem) },
            toLocal = { dto, existing -> dto.toMaintenanceRecord(localVehicleId, unitSystem, existing) },
            isSameReadingAs = { local, remote ->
                isSameMaintReading(local.mileage, local.date, remote.toMaintenanceRecord(localVehicleId, unitSystem))
            },
            addLocal = { maintenanceRepository.saveRecord(it) },
            updateLocal = { maintenanceRepository.saveRecord(it) },
            deleteLocal = { maintenanceRepository.deleteRecordFromSync(it) },
            addRemote = { api.addServiceRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateServiceRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteServiceRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash ->
                local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash, lubeLoggerRecordType = LubeLoggerRecordType.SERVICE)
            },
            onDeletesSkipped = ::logSkippedDeletes,
            onRemoteAdded = { record, newRemoteId ->
                uploadRecordAttachments(api, newRemoteId, entityType = 0, listOfNotNull(record.receiptUri))
            },
        )
    }

    private suspend fun syncRepairRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        unitSystem: String,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        val remoteRecords = api.getRepairRecords(mappedId).bodyIfSuccessful()?.filter { (it.id ?: 0) > 0 } ?: return
        val localRecords = maintenanceRepository.getRecordsForVehicle(localVehicleId).first()
            .filter { it.effectiveRecordType == LubeLoggerRecordType.REPAIR }

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.REPAIR },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { it.id ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toMaintenanceRecord(localVehicleId, unitSystem)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerRepairDto(mappedId, unitSystem) },
            toLocal = { dto, existing -> dto.toMaintenanceRecord(localVehicleId, unitSystem, existing) },
            isSameReadingAs = { local, remote ->
                isSameMaintReading(local.mileage, local.date, remote.toMaintenanceRecord(localVehicleId, unitSystem))
            },
            addLocal = { maintenanceRepository.saveRecord(it) },
            updateLocal = { maintenanceRepository.saveRecord(it) },
            deleteLocal = { maintenanceRepository.deleteRecordFromSync(it) },
            addRemote = { api.addRepairRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateRepairRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteRepairRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash ->
                local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash, lubeLoggerRecordType = LubeLoggerRecordType.REPAIR)
            },
            onDeletesSkipped = ::logSkippedDeletes,
            onRemoteAdded = { record, newRemoteId ->
                uploadRecordAttachments(api, newRemoteId, entityType = 1, listOfNotNull(record.receiptUri))
            },
        )
    }

    private suspend fun syncUpgradeRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        val remoteRecords = api.getUpgradeRecords(mappedId).bodyIfSuccessful()?.filter { (it.id ?: 0) > 0 } ?: return
        val localRecords = modificationRepository.getModsForVehicle(localVehicleId).first()

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.UPGRADE },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { it.id ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toModificationRecord(localVehicleId)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerUpgradeDto(mappedId) },
            toLocal = { dto, existing -> dto.toModificationRecord(localVehicleId, existing) },
            // Modifications have no mileage: link same-day records with the same title.
            isSameReadingAs = { local, remote ->
                val candidate = remote.toModificationRecord(localVehicleId)
                candidate != null && abs(local.date - candidate.date) < ONE_DAY_MILLIS &&
                    local.title.trim().equals(candidate.title.trim(), ignoreCase = true)
            },
            addLocal = { modificationRepository.saveMod(it) },
            updateLocal = { modificationRepository.saveMod(it) },
            deleteLocal = { modificationRepository.deleteModFromSync(it) },
            addRemote = { api.addUpgradeRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateUpgradeRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteUpgradeRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash -> local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash) },
            onDeletesSkipped = ::logSkippedDeletes,
            onRemoteAdded = { record, newRemoteId ->
                uploadRecordAttachments(api, newRemoteId, entityType = 2, record.imageUris)
            },
        )
    }

    private suspend fun syncOdometerRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        unitSystem: String,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        val remoteRecords = api.getOdometerRecords(mappedId).bodyIfSuccessful()
            ?.filter { (parseLubeLoggerInt(it.id) ?: 0) > 0 } ?: return
        val allMaintenance = maintenanceRepository.getRecordsForVehicle(localVehicleId).first()
        val localRecords = allMaintenance.filter { it.effectiveRecordType == LubeLoggerRecordType.ODOMETER }

        val fuelRecords = fuelRepository.getRecordsForVehicle(localVehicleId).first().filter { it.mileage > 0 }
        val chargingRecords = chargingRepository.getRecordsForVehicle(localVehicleId).first().filter { it.mileage > 0 }
        val serviceRepairRecords = allMaintenance.filterNot { it.isOdometerCheckIn }.filter { it.mileage > 0 }

        // Gather all non-check-in local readings (Fuel, Service/Repair, Charging)
        val nonCheckInReadings = mutableListOf<Triple<Int, Long, String>>() // (mileage, date, notes)
        fuelRecords.forEach { fuel ->
            nonCheckInReadings.add(Triple(fuel.mileage, fuel.date, "Fuel fill-up"))
        }
        serviceRepairRecords.forEach { maint ->
            val notes = maint.taskName?.ifBlank { null }
                ?: maint.description.ifBlank { null }
                ?: if (maint.category == MaintenanceCategory.REPAIR) "Repair" else "Service"
            nonCheckInReadings.add(Triple(maint.mileage, maint.date, notes))
        }
        chargingRecords.forEach { charge ->
            nonCheckInReadings.add(Triple(charge.mileage, charge.date, charge.vendor.ifBlank { "Charging" }))
        }

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.ODOMETER },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { parseLubeLoggerInt(it.id) ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toOdometerCheckIn(localVehicleId, unitSystem)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerOdometerDto(mappedId, unitSystem) },
            toLocal = { dto, existing -> dto.toOdometerCheckIn(localVehicleId, unitSystem, existing) },
            isSameReadingAs = { local, remote ->
                isSameMaintReading(local.mileage, local.date, remote.toOdometerCheckIn(localVehicleId, unitSystem))
            },
            addLocal = { record ->
                if (nonCheckInReadings.none { (mileage, date, _) -> isSameReading(mileage, date, record.mileage, record.date) }) {
                    maintenanceRepository.saveRecord(record)
                }
            },
            updateLocal = { maintenanceRepository.saveRecord(it) },
            deleteLocal = { maintenanceRepository.deleteRecordFromSync(it) },
            addRemote = { api.addOdometerRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateOdometerRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteOdometerRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash ->
                local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash, lubeLoggerRecordType = LubeLoggerRecordType.ODOMETER)
            },
            onDeletesSkipped = ::logSkippedDeletes,
        )

        // Ensure LubeLogger's odometer log includes every non-check-in local reading
        val updatedRemoteRecords = api.getOdometerRecords(mappedId).bodyIfSuccessful()
            ?.filter { (parseLubeLoggerInt(it.id) ?: 0) > 0 } ?: remoteRecords
        val remoteReadings = updatedRemoteRecords.mapNotNull { dto ->
            dto.toOdometerCheckIn(localVehicleId, unitSystem)
        }.toMutableList()

        for ((mileage, date, notes) in nonCheckInReadings) {
            val existsInRemote = remoteReadings.any { remote ->
                isSameReading(mileage, date, remote.mileage, remote.date)
            }
            if (!existsInRemote) {
                val dto = createLubeLoggerOdometerDto(
                    lubeLoggerVehicleId = mappedId,
                    dateMillis = date,
                    mileageMiles = mileage,
                    notes = notes,
                    lubeLoggerUnitSystem = unitSystem,
                )
                val newRemoteId = api.addOdometerRecord(mappedId, dto).recordIdOrNull()
                if (newRemoteId != null) {
                    val newCheckIn = dto.copy(id = newRemoteId.toString()).toOdometerCheckIn(localVehicleId, unitSystem)
                    if (newCheckIn != null) {
                        remoteReadings.add(newCheckIn)
                    }
                }
            }
        }
    }

    private suspend fun syncChargingRecords(
        api: LubeLoggerApiService,
        localVehicleId: Long,
        mappedId: Int,
        unitSystem: String,
        pendingDeletes: List<LubeLoggerPendingDelete>,
    ) {
        val remoteRecords = api.getGasRecords(mappedId).bodyIfSuccessful()?.filter { (it.id ?: 0) > 0 } ?: return
        val localRecords = chargingRepository.getRecordsForVehicle(localVehicleId).first()

        syncLubeLoggerRecords(
            localRecords = localRecords,
            remoteRecords = remoteRecords,
            pendingDeletes = pendingDeletes.filter { it.type == LubeLoggerRecordType.FUEL },
            getLubeLoggerId = { it.lubeLoggerId },
            getRemoteId = { it.id ?: 0 },
            getStoredHash = { it.lubeLoggerSyncHash },
            getLocalFingerprint = { it.syncFingerprint() },
            getRemoteFingerprint = { it.toChargingRecord(localVehicleId, unitSystem)?.syncFingerprint().orEmpty() },
            toDto = { it.toLubeLoggerDto(mappedId, unitSystem) },
            toLocal = { dto, existing -> dto.toChargingRecord(localVehicleId, unitSystem, existing) },
            isSameReadingAs = { local, remote ->
                isSameChargingReading(local.mileage, local.date, remote.toChargingRecord(localVehicleId, unitSystem))
            },
            addLocal = { chargingRepository.saveRecord(it) },
            updateLocal = { chargingRepository.saveRecord(it) },
            deleteLocal = { chargingRepository.deleteRecordFromSync(it) },
            addRemote = { api.addGasRecord(mappedId, it).recordIdOrNull() },
            updateRemote = { api.updateGasRecord(it).isOperationSuccess },
            deleteRemote = { api.deleteGasRecord(it).isDeleted },
            onPendingDeleteProcessed = { pendingDeleteDao.delete(it.id) },
            withSyncState = { local, remoteId, hash -> local.copy(lubeLoggerId = remoteId, lubeLoggerSyncHash = hash) },
            onDeletesSkipped = ::logSkippedDeletes,
        )
    }

    /**
     * One-time cleanup of records pulled by older Garage versions, whose description had the task
     * name appended as " (task)". Runs only on records that have never been fingerprinted.
     */
    private suspend fun cleanUpLegacyMaintenanceRecords(localVehicleId: Long) {
        maintenanceRepository.getRecordsForVehicle(localVehicleId).first()
            .filter { it.lubeLoggerId != null && it.lubeLoggerSyncHash == null && !it.isOdometerCheckIn }
            .forEach { record ->
                val cleaned = record.cleanLegacyTaskSuffix()
                if (cleaned != record) maintenanceRepository.saveRecord(cleaned)
            }
    }

    /**
     * A record whose category changed between Repair and non-Repair lives in the wrong LubeLogger
     * list: delete it there and unlink it so the next step re-adds it to the right one.
     */
    private suspend fun moveRecordsBetweenServiceAndRepair(api: LubeLoggerApiService, localVehicleId: Long) {
        maintenanceRepository.getRecordsForVehicle(localVehicleId).first()
            .filter { it.lubeLoggerId != null && it.lubeLoggerRecordType != null }
            .filter { it.lubeLoggerRecordType != it.categoryRecordType }
            .forEach { record ->
                val oldId = record.lubeLoggerId ?: return@forEach
                val deleted = when (record.lubeLoggerRecordType) {
                    LubeLoggerRecordType.SERVICE -> api.deleteServiceRecord(oldId).isSuccessful
                    LubeLoggerRecordType.REPAIR -> api.deleteRepairRecord(oldId).isSuccessful
                    LubeLoggerRecordType.ODOMETER -> api.deleteOdometerRecord(oldId).isSuccessful
                    else -> false
                }
                if (deleted) {
                    maintenanceRepository.saveRecord(
                        record.copy(lubeLoggerId = null, lubeLoggerSyncHash = null, lubeLoggerRecordType = null),
                    )
                }
            }
    }

    // ---------------------------------------------------------------------
    // Vehicle details and image
    // ---------------------------------------------------------------------

    /**
     * Checks if an unmapped Garage vehicle and an unmapped LubeLogger vehicle match the same
     * physical vehicle based on unique identifiers (VIN or License Plate).
     */
    private suspend fun vehiclesMatch(local: Vehicle, remote: LubeLoggerVehicleDto): Boolean {
        val localVin = local.vin.trim().uppercase()
        val remoteVin = remote.findVin()?.trim()?.uppercase().orEmpty()
        if (localVin.isNotBlank() && remoteVin.isNotBlank() && localVin == remoteVin) {
            return true
        }

        val localRegistration = registrationDao.getByVehicleId(local.id).first()
        val localPlate = localRegistration?.licensePlate?.trim().orEmpty()
        val remotePlate = remote.syncablePlate(remoteVin.ifBlank { null })?.trim().orEmpty()

        if (localPlate.isNotBlank() &&
            !localPlate.equals("N/A", ignoreCase = true) &&
            remotePlate.isNotBlank() &&
            !remotePlate.equals("N/A", ignoreCase = true) &&
            localPlate.equals(remotePlate, ignoreCase = true)
        ) {
            return true
        }

        return false
    }

    /**
     * Keeps VIN, purchase condition/mileage, year, make, model, trim and license plate identical
     * in Garage and LubeLogger, using the last synced values to decide which side changed.
     * Returns the possibly-updated Garage vehicle.
     */
    private suspend fun syncVehicleDetails(
        api: LubeLoggerApiService,
        local: Vehicle,
        remote: LubeLoggerVehicleDto,
        unitSystem: String,
    ): Vehicle {
        val registration = registrationDao.getByVehicleId(local.id).first()
        val remoteDetails = remote.syncDetails(unitSystem)
        val merged = mergeVehicleSyncDetails(
            local = local.syncDetails(plate = registration?.licensePlate.orEmpty()),
            remote = remoteDetails,
            base = credentialsManager.getVehicleDetailsSnapshot(local.id),
        )

        val updatedLocal = local.withSyncDetails(merged)
        if (updatedLocal != local) vehicleRepository.saveVehicle(updatedLocal)

        val mergedPlate = merged.plate
        if (mergedPlate != null && mergedPlate != registration?.licensePlate.orEmpty()) {
            registrationDao.upsert(
                registration?.copy(licensePlate = mergedPlate.ifBlank { null })
                    ?: VehicleRegistrationInsurance(vehicleId = local.id, licensePlate = mergedPlate.ifBlank { null }),
            )
        }

        val update = remote.toUpdateDto(merged, unitSystem)
        if (update != null) {
            val response = api.updateVehicle(update)
            if (response.body()?.success != true) {
                // Leave the snapshot alone so the change is retried next sync.
                Log.w(TAG, "LubeLogger rejected vehicle ${remote.id} update: ${response.code()} ${response.body()?.message}")
                return updatedLocal
            }
        }
        // A blank Garage plate never clears LubeLogger's (it's a required field there), so record
        // what the server actually kept; otherwise its plate would look "changed" next sync.
        val snapshot = if (merged.plate.isNullOrBlank()) merged.copy(plate = remoteDetails.plate ?: merged.plate) else merged
        credentialsManager.saveVehicleDetailsSnapshot(local.id, snapshot)
        return updatedLocal
    }

    /**
     * Keeps Garage's primary photo and the LubeLogger vehicle image in sync. Compared with the
     * last sync: a new Garage photo is uploaded, a new server image is downloaded (Garage wins if
     * both changed). Before the first sync, only missing images are filled in.
     */
    private suspend fun syncVehicleImage(api: LubeLoggerApiService, local: Vehicle, mappedId: Int, remoteVehicle: LubeLoggerVehicleDto?) {
        if (remoteVehicle == null) return
        val imageLocation = remoteVehicle.imageLocation?.takeIf { it.isNotBlank() && !it.contains("noimage.png") }
        val localImage = local.imageUri
        val snapshot = credentialsManager.getVehiclePhotoSnapshot(local.id)
        val localChanged = snapshot != null && localImage != snapshot.first
        val remoteChanged = snapshot != null && imageLocation != snapshot.second
        try {
            var finalLocal = localImage
            var finalRemote = imageLocation
            when {
                localImage != null && (localChanged || (snapshot == null && imageLocation == null)) -> {
                    // Garage photo is new (or the server has none yet): upload it.
                    if (!uploadVehicleImage(api, localImage, mappedId)) return
                    finalRemote = api.getVehicles().takeIf { it.isSuccessful }?.body()
                        ?.firstOrNull { it.id == mappedId }?.imageLocation ?: return
                }
                imageLocation != null && (remoteChanged || (snapshot == null && localImage == null)) -> {
                    // Server image is new (or Garage has none yet): download it as the primary photo.
                    val imgResp = api.downloadFile(imageLocation)
                    val bytes = if (imgResp.isSuccessful) imgResp.body()?.bytes() else null
                    if (bytes == null || bytes.isEmpty()) return
                    finalLocal = imageStorageManager.saveImageBytesToInternalStorage(bytes)
                    val current = vehicleRepository.getVehicleByIdOnce(local.id) ?: local
                    vehicleRepository.saveVehicle(current.copy(imageUri = finalLocal))
                    localImage?.let { imageStorageManager.deleteImage(it) }
                }
            }
            credentialsManager.saveVehiclePhotoSnapshot(local.id, finalLocal, finalRemote)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle image sync failed for vehicle $mappedId", e)
        }
    }

    /** Returns true if LubeLogger accepted the new vehicle image. */
    private suspend fun uploadVehicleImage(api: LubeLoggerApiService, imageFilename: String, mappedId: Int): Boolean {
        if (!credentialsManager.usesBasicAuth()) {
            Log.i(TAG, "Skipping vehicle image upload: LubeLogger only allows it with username/password auth")
            return false
        }
        val file = imageStorageManager.imageFile(imageFilename)
        if (!file.exists() || imageStorageManager.isPdfFile(imageFilename)) return false

        val mimeType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaType()))
        val tempLocation = api.uploadTempFile(part).takeIf { it.isSuccessful }?.body()
        if (tempLocation.isNullOrBlank()) return false

        // SaveVehicle replaces every field, so send the complete vehicle back with only the image changed.
        val rawVehicle = api.getVehiclesRaw().takeIf { it.isSuccessful }?.body()
            ?.firstOrNull { it.get("id")?.asInt == mappedId } ?: return false
        val fields = flattenJsonToFormFields(rawVehicle).toMutableMap()
        fields.keys.removeAll { it.equals("imageLocation", ignoreCase = true) }
        fields["imageLocation"] = tempLocation

        val saveResp = api.saveVehicle(fields)
        if (saveResp.body()?.success != true) {
            Log.w(TAG, "LubeLogger rejected vehicle image update: ${saveResp.code()} ${saveResp.body()?.message}")
            return false
        }
        return true
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** The LubeLogger list a maintenance record belongs in, based on its Garage category. */
    private val MaintenanceRecord.categoryRecordType: LubeLoggerRecordType
        get() = when {
            isOdometerCheckIn -> LubeLoggerRecordType.ODOMETER
            category == MaintenanceCategory.REPAIR -> LubeLoggerRecordType.REPAIR
            else -> LubeLoggerRecordType.SERVICE
        }

    /** The list a record is currently synced to, or should be added to if it isn't yet. */
    private val MaintenanceRecord.effectiveRecordType: LubeLoggerRecordType
        get() = lubeLoggerRecordType ?: categoryRecordType

    private fun logSkippedDeletes(skipped: Int, linked: Int) {
        Log.w(TAG, "Safety limit: skipped $skipped of $linked server-side deletes (>50%); will retry next sync")
    }

    private suspend fun uploadRecordAttachments(
        api: LubeLoggerApiService,
        recordId: Int,
        entityType: Int,
        imageFilenames: List<String>,
    ) {
        for (filename in imageFilenames) {
            val file = imageStorageManager.imageFile(filename)
            if (!file.exists()) continue
            val mimeType = if (imageStorageManager.isPdfFile(filename)) "application/pdf"
            else when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaType()))
            val resp = api.uploadDocument(part, entityId = recordId, entityType = entityType)
            if (!resp.isSuccessful) {
                Log.w(TAG, "Document upload failed for record $recordId (type $entityType): ${resp.code()}")
            }
        }
    }

    private fun <T> Response<List<T>>.bodyIfSuccessful(): List<T>? = if (isSuccessful) body() else null

    private fun Response<LubeLoggerOperationResponse>.recordIdOrNull(): Int? =
        if (isSuccessful && body()?.success == true) body()?.additionalData?.recordId?.takeIf { it > 0 } else null

    /** A delete succeeded, or the record was already gone from the server. */
    private val Response<LubeLoggerOperationResponse>.isDeleted: Boolean
        get() = (isSuccessful && body()?.success == true) || code() == 404

    private val Response<LubeLoggerOperationResponse>.isOperationSuccess: Boolean
        get() = isSuccessful && body()?.success == true

    private fun isSameMaintReading(mileage: Int, date: Long, other: MaintenanceRecord?) =
        other != null && isSameReading(mileage, date, other.mileage, other.date)

    private fun isSameFuelReading(mileage: Int, date: Long, other: com.fearmikey.garage.data.local.entity.FuelRecord?) =
        other != null && isSameReading(mileage, date, other.mileage, other.date)

    private fun isSameChargingReading(mileage: Int, date: Long, other: ChargingRecord?) =
        other != null && isSameReading(mileage, date, other.mileage, other.date)

    private fun isSameReading(mileageA: Int, dateA: Long, mileageB: Int, dateB: Long) =
        abs(mileageA - mileageB) <= 1 && abs(dateA - dateB) < ONE_DAY_MILLIS

    companion object {
        /** Output-data key carrying a human-readable reason when the sync fails. */
        const val KEY_ERROR = "lubelogger_error"

        /** Tag on user-initiated syncs; they fail fast instead of retrying with backoff. */
        const val TAG_MANUAL = "lubelogger_manual_sync"

        /** Unique work name for "Sync now", so Settings can observe it. */
        const val MANUAL_WORK_NAME = "LubeLoggerManualSync"
        private const val TAG = "LubeLoggerSync"
        private const val ONE_DAY_MILLIS = 24 * 3600 * 1000L
        private val syncMutex = Mutex()
    }
}
