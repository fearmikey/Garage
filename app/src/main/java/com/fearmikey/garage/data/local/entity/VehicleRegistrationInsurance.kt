package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Vehicle registration, tag/license plate, state inspection, and insurance policy details for a vehicle.
 *
 * Kept as a separate 1:1 table (same pattern as [VehicleSpecs] and [VehiclePartsInfo]).
 */
@Entity(
    tableName = "vehicle_registration_insurance",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VehicleRegistrationInsurance(
    @PrimaryKey val vehicleId: Long,
    // Tag / Vehicle Registration
    val licensePlate: String? = null,
    val registrationState: String? = null,
    val registrationExpiration: Long? = null,
    val registrationFee: Double? = null,
    val registrationNotes: String? = null,
    val registrationImageUri: String? = null,

    // State Vehicle Inspection
    val inspectionExpiration: Long? = null,
    val inspectionDate: Long? = null,
    val inspectionResult: String? = null,
    val inspectionNotes: String? = null,

    // Insurance Policy
    val insuranceProvider: String? = null,
    val policyNumber: String? = null,
    val insuranceExpiration: Long? = null,
    val insurancePremium: Double? = null,
    val insuranceAgentContact: String? = null,
    val insuranceNotes: String? = null,
    val insuranceImageUri: String? = null,
) {
    /** True when every field is null or blank, i.e. nothing worth displaying yet. */
    fun isEmpty(): Boolean = listOf(
        licensePlate, registrationState, registrationNotes,
        inspectionResult, inspectionNotes,
        insuranceProvider, policyNumber, insuranceAgentContact, insuranceNotes,
    ).all { it.isNullOrBlank() } &&
        registrationExpiration == null &&
        registrationFee == null &&
        registrationImageUri == null &&
        inspectionExpiration == null &&
        inspectionDate == null &&
        insuranceExpiration == null &&
        insurancePremium == null &&
        insuranceImageUri == null
}
