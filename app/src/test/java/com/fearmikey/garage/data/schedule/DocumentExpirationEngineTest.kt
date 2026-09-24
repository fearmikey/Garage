package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.repository.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DocumentExpirationEngineTest {

    @Test
    fun `checkExpirations returns empty list when no expiration dates are set`() {
        val record = VehicleRegistrationInsurance(vehicleId = 1L)
        val reminders = DocumentExpirationEngine.checkExpirations(record)
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `checkExpirations correctly identifies overdue expirations`() {
        val now = 1_000_000_000_000L
        val pastDate = now - TimeUnit.DAYS.toMillis(10)

        val record = VehicleRegistrationInsurance(
            vehicleId = 1L,
            registrationExpiration = pastDate,
            inspectionExpiration = pastDate,
            emissionsExpiration = pastDate,
            inspectionStickerExpiration = pastDate,
            insuranceExpiration = pastDate,
            tollPassParkingExpiration = pastDate,
            driversLicenseExpiration = pastDate,
        )

        val reminders = DocumentExpirationEngine.checkExpirations(
            record = record,
            upcomingWindowDays = 30,
            now = now,
        )

        assertEquals(7, reminders.size)
        assertTrue(reminders.all { it.status == ReminderStatus.OVERDUE })
    }

    @Test
    fun `checkExpirations correctly identifies upcoming expirations within window`() {
        val now = 1_000_000_000_000L
        val upcomingDate = now + TimeUnit.DAYS.toMillis(15)

        val record = VehicleRegistrationInsurance(
            vehicleId = 1L,
            emissionsExpiration = upcomingDate,
            driversLicenseExpiration = upcomingDate,
        )

        val reminders = DocumentExpirationEngine.checkExpirations(
            record = record,
            upcomingWindowDays = 30,
            now = now,
        )

        assertEquals(2, reminders.size)
        assertTrue(reminders.all { it.status == ReminderStatus.UPCOMING })
    }

    @Test
    fun `checkExpirations correctly identifies active expirations outside window`() {
        val now = 1_000_000_000_000L
        val futureDate = now + TimeUnit.DAYS.toMillis(90)

        val record = VehicleRegistrationInsurance(
            vehicleId = 1L,
            tollPassParkingExpiration = futureDate,
            inspectionStickerExpiration = futureDate,
        )

        val reminders = DocumentExpirationEngine.checkExpirations(
            record = record,
            upcomingWindowDays = 30,
            now = now,
        )

        assertEquals(2, reminders.size)
        assertTrue(reminders.all { it.status == ReminderStatus.OK })
    }
}
