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
    /**
     * Optional second registration photo (back side / second page). Many European registration
     * documents (e.g. Polish "dowód rejestracyjny", German "Zulassungsbescheinigung Teil I")
     * are two-sided or folded booklets, so a single photo is not enough.
     */
    val registrationImageUri2: String? = null,

    // State Vehicle Inspection
    val inspectionExpiration: Long? = null,
    val inspectionDate: Long? = null,
    val inspectionResult: String? = null,
    val inspectionNotes: String? = null,

    // Emissions / Smog Testing
    val emissionsExpiration: Long? = null,
    val emissionsDate: Long? = null,
    val emissionsResult: String? = null,
    val emissionsNotes: String? = null,

    // State / Local Inspection Stickers
    val inspectionStickerExpiration: Long? = null,
    val inspectionStickerNumber: String? = null,
    val inspectionStickerNotes: String? = null,

    // Insurance Policy
    val insuranceProvider: String? = null,
    val policyNumber: String? = null,
    val insuranceExpiration: Long? = null,
    val insurancePremium: Double? = null,
    val insuranceAgentContact: String? = null,
    val insuranceNotes: String? = null,
    val insuranceImageUri: String? = null,

    // Toll Pass & Parking Permits
    val tollPassParkingExpiration: Long? = null,
    val tollPassParkingAccount: String? = null,
    val tollPassParkingNotes: String? = null,

    // Driver's License
    val driversLicenseExpiration: Long? = null,
    val driversLicenseNumber: String? = null,
    val driversLicenseState: String? = null,
    val driversLicenseNotes: String? = null,

    // Purchase / Bill of Sale
    val purchasePrice: Double? = null,
    val purchaseDate: Long? = null,
    /** Dealer or private seller name. */
    val purchaseSeller: String? = null,
    val purchaseNotes: String? = null,
    /** Photo of the bill of sale / purchase contract. */
    val purchaseImageUri: String? = null,
) {
    val hasPurchaseData: Boolean
        get() = (purchasePrice != null) || (purchaseDate != null) || !purchaseSeller.isNullOrBlank() ||
            !purchaseNotes.isNullOrBlank() || (purchaseImageUri != null)

    /** True when every field is null or blank, i.e. nothing worth displaying yet. */
    fun isEmpty(): Boolean = (listOf(
        licensePlate, registrationState, registrationNotes,
        inspectionResult, inspectionNotes,
        emissionsResult, emissionsNotes,
        inspectionStickerNumber, inspectionStickerNotes,
        insuranceProvider, policyNumber, insuranceAgentContact, insuranceNotes,
        tollPassParkingAccount, tollPassParkingNotes,
        driversLicenseNumber, driversLicenseState, driversLicenseNotes,
    ).all { it.isNullOrBlank() }) &&
        !hasPurchaseData &&
        registrationExpiration == null &&
        registrationFee == null &&
        registrationImageUri == null &&
        registrationImageUri2 == null &&
        inspectionExpiration == null &&
        inspectionDate == null &&
        emissionsExpiration == null &&
        emissionsDate == null &&
        inspectionStickerExpiration == null &&
        insuranceExpiration == null &&
        insurancePremium == null &&
        insuranceImageUri == null &&
        tollPassParkingExpiration == null &&
        driversLicenseExpiration == null
}
