package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.local.entity.VehicleType
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
    /** Car vs. motorcycle, derived from NHTSA's `VehicleType`; null when the decoder didn't say. */
    val vehicleType: VehicleType? = null,
    /** Extended specs to persist alongside the vehicle once it's saved; see [VehicleSpecs]. */
    val specs: VehicleSpecs,
)

sealed interface VinLookupResult {
    /** [warning] is set when NHTSA decoded the VIN but flagged parts of it as unreliable/missing. */
    data class Success(val info: VinInfo, val warning: String? = null) : VinLookupResult
    data class Error(val message: String) : VinLookupResult
}

@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val vehicleSpecsDao: VehicleSpecsDao,
    private val vehiclePartsDao: VehiclePartsDao,
    private val vehicleRegistrationDao: VehicleRegistrationDao,
    private val vinDecoderApi: VinDecoderApi,
) {
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleDao.getVehicleById(vehicleId)

    suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleDao.getVehicleByIdOnce(vehicleId)

    suspend fun saveVehicle(vehicle: Vehicle): Long = vehicleDao.upsert(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.delete(vehicle)

    fun getVehicleSpecs(vehicleId: Long): Flow<VehicleSpecs?> = vehicleSpecsDao.getByVehicleId(vehicleId)

    suspend fun getVehicleSpecsOnce(vehicleId: Long): VehicleSpecs? = vehicleSpecsDao.getByVehicleIdOnce(vehicleId)

    /** Persists specs decoded from a VIN, keyed to the (by-then known) vehicle id. */
    suspend fun saveVehicleSpecs(vehicleId: Long, specs: VehicleSpecs) =
        vehicleSpecsDao.upsert(specs.copy(vehicleId = vehicleId))

    fun getVehicleParts(vehicleId: Long): Flow<VehiclePartsInfo?> = vehiclePartsDao.getByVehicleId(vehicleId)

    suspend fun saveVehicleParts(info: VehiclePartsInfo) = vehiclePartsDao.upsert(info)

    fun getRegistrationInsurance(vehicleId: Long): Flow<VehicleRegistrationInsurance?> =
        vehicleRegistrationDao.getByVehicleId(vehicleId)

    suspend fun saveRegistrationInsurance(info: VehicleRegistrationInsurance) =
        vehicleRegistrationDao.upsert(info)

    suspend fun deleteRegistrationInsurance(vehicleId: Long) =
        vehicleRegistrationDao.deleteByVehicleId(vehicleId)

    /** Calls the NHTSA vPIC API and maps its quirky "HTTP 200 + error code" contract into a typed result. */
    suspend fun decodeVin(vin: String): VinLookupResult {
        return try {
            val result = vinDecoderApi.decodeVin(vin).results.firstOrNull()
                ?: return VinLookupResult.Error("No decode results returned for this VIN.")

            // NHTSA's ErrorCode is a comma-separated list (e.g. "0,14" = decoded clean, plus a
            // minor note). Most codes are warnings that still come with usable data, which is
            // very common for non-US (e.g. European) VINs, so data presence decides success.
            val codes = parseErrorCodes(result.errorCode)
            val hasData = !result.make.isNullOrBlank() || !result.model.isNullOrBlank()
            if (!hasData) {
                return VinLookupResult.Error(errorMessageFor(codes))
            }
            // Code 11: position 10 isn't a valid model-year code (EU VINs aren't required to
            // encode it there), so the decoded year can't be trusted.
            val yearUnreliable = CODE_INVALID_MODEL_YEAR in codes

            VinLookupResult.Success(
                info = VinInfo(
                    year = if (yearUnreliable) null else result.modelYear?.toIntOrNull(),
                    make = result.make.orEmpty(),
                    model = result.model.orEmpty(),
                    trim = result.trim.orEmpty(),
                    drivetrain = driveTypeToDrivetrain(result.driveType),
                    vehicleType = VehicleType.fromNhtsa(cleanSpec(result.vehicleType)),
                    specs = result.toVehicleSpecs(),
                ),
                warning = warningFor(codes)
                    ?: if (result.model.isNullOrBlank()) "Model not found for this VIN; please enter it manually." else null,
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
        fuelTypeSecondary = cleanSpec(fuelTypeSecondary),
        electrificationLevel = cleanSpec(electrificationLevel),
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
        trailerBrakedCapacity = cleanSpec(trailerBrakedCapacity),
        trailerUnbrakedCapacity = cleanSpec(trailerUnbrakedCapacity),
        gcwr = cleanSpec(gcwr),
        curbWeight = cleanSpec(curbWeight),
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
        private const val CODE_INCOMPLETE_VIN = 6
        private const val CODE_MANUFACTURER_NOT_REGISTERED = 7
        private const val CODE_INVALID_MODEL_YEAR = 11
        private const val CODE_INVALID_CHARACTERS = 400

        /** Codes that are purely informational and need no user-facing note. */
        private val INFORMATIONAL_CODES = setOf(0, 14)

        private const val NON_US_HINT =
            "The NHTSA database mainly covers vehicles sold in the US, so vehicles from " +
                "other markets may need to be entered manually."

        /** Parses NHTSA's comma-separated `ErrorCode` field (e.g. "0,14") into a set of ints. */
        internal fun parseErrorCodes(raw: String?): Set<Int> =
            raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

        internal fun errorMessageFor(codes: Set<Int>): String = when {
            CODE_INVALID_CHARACTERS in codes -> "This VIN contains invalid characters."
            CODE_INCOMPLETE_VIN in codes -> "This VIN appears to be incomplete. Check it's 17 characters with no typos."
            CODE_MANUFACTURER_NOT_REGISTERED in codes ->
                "This manufacturer isn't in the NHTSA database. $NON_US_HINT"
            else -> "No vehicle details found for this VIN. $NON_US_HINT"
        }

        internal fun warningFor(codes: Set<Int>): String? = when {
            CODE_INVALID_MODEL_YEAR in codes -> "Model year couldn't be determined from this VIN; please enter it manually."
            (codes - INFORMATIONAL_CODES).isNotEmpty() -> "VIN only partially decoded; please verify the details."
            else -> null
        }

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
