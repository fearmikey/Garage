package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.remote.RecallApi
import com.fearmikey.garage.data.remote.dto.RecallDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single open NHTSA safety recall applicable to a vehicle's year/make/model.
 *
 * NHTSA has no VIN-keyed recalls lookup, so a match here isn't verified
 * against the vehicle's specific VIN or trim -- just its year, make and model.
 */
data class VehicleRecall(
    val campaignNumber: String,
    val component: String,
    val summary: String,
    val consequence: String,
    val remedy: String,
    val reportedDate: String?,
    /** True if NHTSA advises not driving the vehicle until repaired. */
    val parkIt: Boolean,
    /** True if NHTSA advises parking outside (e.g. fire risk) until repaired. */
    val parkOutside: Boolean,
)

sealed interface RecallLookupResult {
    data class Success(val recalls: List<VehicleRecall>) : RecallLookupResult
    data class Error(val message: String) : RecallLookupResult
}

/** Looks up open NHTSA safety recalls for a vehicle's year/make/model. */
@Singleton
class RecallRepository @Inject constructor(
    private val recallApi: RecallApi,
) {
    suspend fun getRecalls(year: Int, make: String, model: String): RecallLookupResult {
        return try {
            val response = recallApi.getRecalls(make = make, model = model, modelYear = year)
            RecallLookupResult.Success(response.results.mapNotNull { it.toVehicleRecallOrNull() })
        } catch (e: Exception) {
            RecallLookupResult.Error(e.message ?: "Network error while checking for recalls.")
        }
    }

    /** Entries with no campaign number aren't actionable/displayable, so they're dropped. */
    private fun RecallDto.toVehicleRecallOrNull(): VehicleRecall? {
        val number = campaignNumber?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return VehicleRecall(
            campaignNumber = number,
            component = component?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown component",
            summary = summary.orEmpty(),
            consequence = consequence.orEmpty(),
            remedy = remedy.orEmpty(),
            reportedDate = reportReceivedDate?.trim()?.takeIf { it.isNotBlank() },
            parkIt = parkIt,
            parkOutside = parkOutside,
        )
    }
}
