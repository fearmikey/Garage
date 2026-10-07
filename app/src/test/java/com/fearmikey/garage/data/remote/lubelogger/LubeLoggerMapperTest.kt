package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LubeLoggerMapperTest {

    @Test
    fun `fuel record maps correctly to dto`() {
        // Arrange
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.JANUARY, 15, 12, 0, 0)
        }
        val fuelRecord = FuelRecord(
            id = 1L,
            vehicleId = 5L,
            date = cal.timeInMillis,
            mileage = 150000,
            gallons = 12.5,
            totalCost = 45.0,
            pricePerGallon = 3.6,
            isFullTank = true
        )
        val lubeLoggerVehicleId = 99

        // Act
        val dto = fuelRecord.toLubeLoggerDto(lubeLoggerVehicleId)

        // Assert
        assertEquals(99, dto.vehicleId)
        assertEquals("2025-01-15", dto.date)
        assertEquals("150000", dto.odometer)
        assertEquals("12.500", dto.fuelConsumed)
        assertEquals("45.00", dto.cost)
        assertEquals("true", dto.isFillToFull)
        assertEquals("false", dto.missedFuelUp) // Always false currently
    }

    @Test
    fun `maintenance record maps correctly to dto`() {
        // Arrange
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.FEBRUARY, 10, 12, 0, 0)
        }
        val maintRecord = MaintenanceRecord(
            id = 2L,
            vehicleId = 5L,
            date = cal.timeInMillis,
            mileage = 152000,
            description = "Oil Change",
            cost = 65.0,
            category = MaintenanceCategory.FLUIDS,
            taskName = "Engine Oil",
            isDeferred = false
        )
        val lubeLoggerVehicleId = 42

        // Act
        val dto = maintRecord.toLubeLoggerDto(lubeLoggerVehicleId)

        // Assert
        assertEquals(42, dto.vehicleId)
        assertEquals("2025-02-10", dto.date)
        assertEquals("152000", dto.odometer)
        assertEquals("65.00", dto.cost)
        assertEquals("Oil Change (Engine Oil)", dto.description)
    }

    @Test
    fun `maintenance record description includes deferred status`() {
        // Arrange
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.MARCH, 1, 12, 0, 0)
        }
        val maintRecord = MaintenanceRecord(
            id = 3L,
            vehicleId = 5L,
            date = cal.timeInMillis,
            mileage = 155000,
            description = "Brake Pads",
            cost = 0.0,
            category = MaintenanceCategory.BRAKES,
            isDeferred = true
        )
        val lubeLoggerVehicleId = 42

        // Act
        val dto = maintRecord.toLubeLoggerDto(lubeLoggerVehicleId)

        // Assert
        assertEquals("Brake Pads [Deferred]", dto.description)
    }
}