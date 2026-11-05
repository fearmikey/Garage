package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.CustomDocument
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.repository.ReminderStatus
import java.util.concurrent.TimeUnit

/**
 * Evaluates document renewal expirations (registration, state inspection, emissions,
 * inspection sticker, insurance, toll/parking pass, driver's license) against a
 * target window to determine reminder status.
 */
data class DocumentExpirationReminder(
    val idKey: String,
    val title: String,
    val expirationDate: Long,
    val status: ReminderStatus,
)

object DocumentExpirationEngine {

    fun checkExpirations(
        record: VehicleRegistrationInsurance,
        upcomingWindowDays: Int = 30,
        now: Long = System.currentTimeMillis(),
    ): List<DocumentExpirationReminder> {
        val windowMillis = TimeUnit.DAYS.toMillis(upcomingWindowDays.coerceAtLeast(1).toLong())
        return listOfNotNull(
            record.registrationExpiration?.let { exp ->
                DocumentExpirationReminder("registration", "Vehicle Registration Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.inspectionExpiration?.let { exp ->
                DocumentExpirationReminder("inspection", "State Vehicle Inspection Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.emissionsExpiration?.let { exp ->
                DocumentExpirationReminder("emissions", "Emissions / Smog Test Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.inspectionStickerExpiration?.let { exp ->
                DocumentExpirationReminder("inspection_sticker", "Inspection Sticker Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.insuranceExpiration?.let { exp ->
                DocumentExpirationReminder("insurance", "Insurance Policy Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.tollPassParkingExpiration?.let { exp ->
                DocumentExpirationReminder("toll_parking", "Toll Pass / Parking Permit Renewal", exp, statusFor(exp, windowMillis, now))
            },
            record.driversLicenseExpiration?.let { exp ->
                DocumentExpirationReminder("drivers_license", "Driver's License Renewal", exp, statusFor(exp, windowMillis, now))
            },
        )
    }

    /**
     * Expirations for a vehicle's fixed document sections ([record], may be null) plus its
     * user-named [customDocuments].
     */
    fun checkAll(
        record: VehicleRegistrationInsurance?,
        customDocuments: List<CustomDocument>,
        upcomingWindowDays: Int = 30,
        now: Long = System.currentTimeMillis(),
    ): List<DocumentExpirationReminder> {
        val windowMillis = TimeUnit.DAYS.toMillis(upcomingWindowDays.coerceAtLeast(1).toLong())
        val fixed = record?.let { checkExpirations(it, upcomingWindowDays, now) }.orEmpty()
        val custom = customDocuments.mapNotNull { doc ->
            doc.expiration?.let { exp ->
                DocumentExpirationReminder(
                    idKey = "custom_${doc.id}",
                    title = "${doc.title.ifBlank { "Document" }} Renewal",
                    expirationDate = exp,
                    status = statusFor(exp, windowMillis, now),
                )
            }
        }
        return fixed + custom
    }

    private fun statusFor(expirationDate: Long, windowMillis: Long, now: Long): ReminderStatus {
        return when {
            expirationDate <= now -> ReminderStatus.OVERDUE
            expirationDate <= (now + windowMillis) -> ReminderStatus.UPCOMING
            else -> ReminderStatus.OK
        }
    }
}
