package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.remote.VinDecoderApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a VIN decode attempt, kept separate from [Vehicle] so a failed/partial
 * decode never silently corrupts already-entered form fields in the UI. */
data class VinInfo(
    val year: Int?,
    val make: String,
    val model: String,
    val trim: String,
)

sealed interface VinLookupResult {
    data class Success(val info: VinInfo) : VinLookupResult
    data class Error(val message: String) : VinLookupResult
}

@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val vinDecoderApi: VinDecoderApi,
) {
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleDao.getVehicleById(vehicleId)

    suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleDao.getVehicleByIdOnce(vehicleId)

    suspend fun saveVehicle(vehicle: Vehicle): Long = vehicleDao.upsert(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.delete(vehicle)

    /** Calls the NHTSA vPIC API and maps its quirky "HTTP 200 + error code" contract into a typed result. */
    suspend fun decodeVin(vin: String): VinLookupResult {
        return try {
            val result = vinDecoderApi.decodeVin(vin).results.firstOrNull()
                ?: return VinLookupResult.Error("No decode results returned for this VIN.")

            val errorCode = result.errorCode?.trim()
            if (errorCode != null && errorCode != "0") {
                return VinLookupResult.Error(result.errorText?.takeIf { it.isNotBlank() } ?: "Unable to decode VIN.")
            }
            if (result.make.isNullOrBlank() && result.model.isNullOrBlank()) {
                return VinLookupResult.Error("VIN decoded but returned no vehicle details.")
            }

            VinLookupResult.Success(
                VinInfo(
                    year = result.modelYear?.toIntOrNull(),
                    make = result.make.orEmpty(),
                    model = result.model.orEmpty(),
                    trim = result.trim.orEmpty(),
                )
            )
        } catch (e: Exception) {
            VinLookupResult.Error(e.message ?: "Network error while decoding VIN.")
        }
    }
}
