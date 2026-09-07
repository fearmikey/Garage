package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResult
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
    val drivetrain: Drivetrain?,
    /** Extended specs to persist alongside the vehicle once it's saved; see [VehicleSpecs]. */
    val specs: VehicleSpecs,
)

sealed interface VinLookupResult {
    data class Success(val info: VinInfo) : VinLookupResult
    data class Error(val message: String) : VinLookupResult
}

@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val vehicleSpecsDao: VehicleSpecsDao,
    private val vinDecoderApi: VinDecoderApi,
) {
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleDao.getVehicleById(vehicleId)

    suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleDao.getVehicleByIdOnce(vehicleId)

    suspend fun saveVehicle(vehicle: Vehicle): Long = vehicleDao.upsert(vehicle)

    fun getVehicleSpecs(vehicleId: Long): Flow<VehicleSpecs?> = vehicleSpecsDao.getByVehicleId(vehicleId)

    /** Persists specs decoded from a VIN, keyed to the (by-then known) vehicle id. */
    suspend fun saveVehicleSpecs(vehicleId: Long, specs: VehicleSpecs) =
        vehicleSpecsDao.upsert(specs.copy(vehicleId = vehicleId))

    /** Calls the NHTSA vPIC API and maps its quirky "HTTP 200 + error code" contract into a typed result. */
    suspend fun decodeVin(vin: String): VinLookupResult {
        return try {
            val result = vinDecoderApi.decodeVin(vin).results.firstOrNull()
                ?: return VinLookupResult.Error("No decode results returned for this VIN.")

            val errorCode = result.errorCode?.trim()
            if ((errorCode != null) && (errorCode != "0")) {
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
                    drivetrain = driveTypeToDrivetrain(result.driveType),
                    specs = result.toVehicleSpecs(),
                ),
            )
        } catch (e: Exception) {
            VinLookupResult.Error(e.message ?: "Network error while decoding VIN.")
        }
    }

    /**
     * Maps the raw NHTSA decode fields into [VehicleSpecs], using [cleanSpec] to blank out
     * NHTSA's "unknown value" placeholders (e.g. "Not Applicable") so the Specs tab doesn't
     * show noise for fields the decoder couldn't actually determine.
     */
    private fun VinDecodeResult.toVehicleSpecs(): VehicleSpecs = VehicleSpecs(
        vehicleId = 0,
        engineCylinders = cleanSpec(engineCylinders),
        displacementL = cleanSpec(displacementL),
        engineHp = cleanSpec(engineHp),
        fuelType = cleanSpec(fuelTypePrimary),
        transmissionStyle = cleanSpec(transmissionStyle),
        transmissionSpeeds = cleanSpec(transmissionSpeeds),
        bodyClass = cleanSpec(bodyClass),
        doors = cleanSpec(doors),
        gvwr = cleanSpec(gvwr),
        series = cleanSpec(series),
        vehicleType = cleanSpec(vehicleType),
        plantCity = cleanSpec(plantCity),
        plantState = cleanSpec(plantState),
        plantCountry = cleanSpec(plantCountry),
        manufacturer = cleanSpec(manufacturer),
    )

    /**
     * Best-effort mapping from NHTSA's free-text `DriveType` field (e.g.
     * "4x4", "4WD/4-Wheel Drive/4x4", "AWD", "FWD/Front-Wheel Drive") to our
     * [Drivetrain] enum. Returns null (leave whatever the user already picked
     * untouched) when the text is blank or unrecognized.
     */
    private fun driveTypeToDrivetrain(driveType: String?): Drivetrain? {
        val text = driveType?.lowercase() ?: return null
        return when {
            text.isBlank() -> null
            ("4x4" in text) || ("4wd" in text) || ("4-wheel" in text) -> Drivetrain.FOUR_WD
            ("awd" in text) || ("all-wheel" in text) -> Drivetrain.AWD
            ("4x2" in text) && ("rear" in text) -> Drivetrain.RWD
            ("rwd" in text) || ("rear-wheel" in text) -> Drivetrain.RWD
            ("fwd" in text) || ("front-wheel" in text) || ("4x2" in text) -> Drivetrain.FWD
            else -> null
        }
    }

    companion object {
        /**
         * NHTSA returns literal placeholder strings (rather than omitting the field) when a
         * value couldn't be decoded. Filtering these out keeps the Specs tab from showing
         * meaningless noise like "Not Applicable" next to half the fields.
         */
        private val BLANK_SPEC_VALUES = setOf(
            "not applicable", "n/a", "na", "not available", "unknown", "none",
        )

        internal fun cleanSpec(value: String?): String? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrBlank()) return null
            return if (trimmed.lowercase() in BLANK_SPEC_VALUES) null else trimmed
        }
    }
}
