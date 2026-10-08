package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        // Description is sent unchanged; category and task travel in custom fields.
        assertEquals("Oil Change", dto.description)
        assertEquals("FLUIDS", dto.extraFields?.first { it.name == "Garage Category" }?.value)
        assertEquals("Engine Oil", dto.extraFields?.first { it.name == "Garage Task" }?.value)
    }

    @Test
    fun `service record round trips category task and deferral`() {
        val original = MaintenanceRecord(
            vehicleId = 5L, date = 0L, mileage = 1000, description = "Brake Pads (front)",
            cost = 12.5, category = MaintenanceCategory.BRAKES, taskName = "Brake pad replacement",
            isDeferred = true, lubeLoggerId = 7,
        )
        val parsed = original.toLubeLoggerDto(42).copy(id = 7).toMaintenanceRecord(5L)!!

        assertEquals("Brake Pads (front)", parsed.description)
        assertEquals(MaintenanceCategory.BRAKES, parsed.category)
        assertEquals("Brake pad replacement", parsed.taskName)
        assertEquals(true, parsed.isDeferred)
        assertEquals(original.syncFingerprint(), parsed.syncFingerprint())
    }

    @Test
    fun `legacy task suffix is removed only for known tasks`() {
        val legacy = LubeLoggerServiceRecordDto(id = 3, date = "2025-07-25", odometer = "1000", description = "Oil & filter change (Engine oil change)")
        val parsed = legacy.toMaintenanceRecord(5L)!!
        assertEquals("Oil & filter change", parsed.description)
        assertEquals("Engine oil change", parsed.taskName)
        assertEquals(MaintenanceCategory.FLUIDS, parsed.category)

        val userText = LubeLoggerServiceRecordDto(id = 4, date = "2025-07-25", odometer = "1000", description = "Brake pads (front)")
        assertEquals("Brake pads (front)", userText.toMaintenanceRecord(5L)!!.description)
    }

    @Test
    fun `repair records default to repair category`() {
        val dto = LubeLoggerRepairRecordDto(id = 9, date = "2025-07-25", odometer = "1000", description = "Replaced alternator")
        val parsed = dto.toMaintenanceRecord(5L)!!
        assertEquals(MaintenanceCategory.REPAIR, parsed.category)
        assertEquals(com.fearmikey.garage.data.local.entity.LubeLoggerRecordType.REPAIR, parsed.lubeLoggerRecordType)
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
        assertEquals("Brake Pads", dto.description)
        assertEquals("true", dto.extraFields?.first { it.name == "Garage Deferred" }?.value)
    }

    private fun checkIn(mileage: Int, description: String = "Odometer check-in") = MaintenanceRecord(
        vehicleId = 5L,
        date = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 7, 12, 0, 0) }.timeInMillis,
        mileage = mileage,
        description = description,
        cost = 0.0,
        category = MaintenanceCategory.INSPECTION,
        taskName = ODOMETER_CHECK_IN_TASK,
    )

    @Test
    fun `odometer check-in maps to odometer dto without id or files`() {
        val dto = checkIn(86100).toLubeLoggerOdometerDto(1)

        assertEquals("1", dto.vehicleId)
        assertEquals("2026-10-07", dto.date)
        assertEquals("86100", dto.odometer)
        assertEquals("", dto.notes)
        assertNull(dto.id)
    }

    @Test
    fun `odometer check-in keeps custom notes`() {
        val dto = checkIn(86100, "Weekly log").toLubeLoggerOdometerDto(1)
        assertEquals("Weekly log", dto.notes)
    }

    @Test
    fun `isOdometerCheckIn only matches check-ins`() {
        assertTrue(checkIn(1).isOdometerCheckIn)
        assertFalse(checkIn(1).copy(category = MaintenanceCategory.OTHER).isOdometerCheckIn)
        assertFalse(checkIn(1).copy(taskName = null).isOdometerCheckIn)
    }

    @Test
    fun `remote odometer record in default culture maps to check-in`() {
        val dto = LubeLoggerOdometerRecordDto(
            id = "8", vehicleId = "1", date = "10/08/2026", initialOdometer = "83200",
            odometer = "83200", notes = "",
        )
        val record = dto.toOdometerCheckIn(localVehicleId = 5L)!!

        assertEquals(8, record.lubeLoggerId)
        assertEquals(83200, record.mileage)
        assertEquals("Odometer check-in", record.description)
        assertTrue(record.isOdometerCheckIn)
        val cal = Calendar.getInstance().apply { timeInMillis = record.date }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(8, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `remote odometer record in invariant culture maps to check-in`() {
        val dto = LubeLoggerOdometerRecordDto(id = "15", date = "2026-10-08", odometer = "83300.0", notes = "Trip")
        val record = dto.toOdometerCheckIn(localVehicleId = 5L)!!

        assertEquals(15, record.lubeLoggerId)
        assertEquals(83300, record.mileage)
        assertEquals("Trip", record.description)
    }

    @Test
    fun `metric odometer record is converted to miles`() {
        val dto = LubeLoggerOdometerRecordDto(id = "3", date = "2026-10-08", odometer = "160934")
        val record = dto.toOdometerCheckIn(localVehicleId = 5L, lubeLoggerUnitSystem = "metric")!!
        assertEquals(100000, record.mileage)
    }

    @Test
    fun `blank placeholder odometer records are ignored`() {
        val ghost = LubeLoggerOdometerRecordDto(
            id = "0", vehicleId = "0", date = "01/01/0001", initialOdometer = "0", odometer = "0",
        )
        assertNull(ghost.toOdometerCheckIn(5L))
        assertNull(ghost.copy(id = "27").toOdometerCheckIn(5L))
        assertNull(ghost.copy(id = "27", odometer = "5000").toOdometerCheckIn(5L))
    }

    @Test
    fun `gas record parses capitalized booleans`() {
        val dto = LubeLoggerGasRecordDto(id = 1, date = "10/07/2026", odometer = "82828", fuelConsumed = "18.2", cost = "4.44", isFillToFull = "False")
        val record = dto.toFuelRecord(5L)!!
        assertFalse(record.isFullTank)
        assertEquals(82828, record.mileage)
    }

    @Test
    fun `json is flattened to asp net form fields`() {
        val json = com.google.gson.JsonParser.parseString(
            """{"id":1,"make":"Chevy","mapLocation":null,"isDiesel":false,"tags":["a","b"],
               "extraFields":[{"name":"Color","value":"Red"}],"dashboardMetrics":[0,2]}"""
        )
        val fields = flattenJsonToFormFields(json)

        assertEquals(
            mapOf(
                "id" to "1", "make" to "Chevy", "isDiesel" to "false", "tags[0]" to "a", "tags[1]" to "b",
                "extraFields[0].name" to "Color", "extraFields[0].value" to "Red",
                "dashboardMetrics[0]" to "0", "dashboardMetrics[1]" to "2",
            ),
            fields,
        )
    }

    @Test
    fun `vin is read from license plate when it is a valid vin`() {
        val vehicle = LubeLoggerVehicleDto(id = 2, licensePlate = "19uub2f37ha007169 ")
        assertEquals("19UUB2F37HA007169", vehicle.findVin())
    }

    @Test
    fun `real license plate is not treated as a vin`() {
        assertNull(LubeLoggerVehicleDto(id = 1, licensePlate = "GASHURTS").findVin())
    }

    @Test
    fun `vin extra field takes priority over license plate`() {
        val vehicle = LubeLoggerVehicleDto(
            id = 1,
            licensePlate = "GASHURTS",
            extraFields = listOf(
                LubeLoggerExtraFieldDto(name = "Color", value = "Silver"),
                LubeLoggerExtraFieldDto(name = "vin", value = "1GNDT13W6W2123456"),
            ),
        )
        assertEquals("1GNDT13W6W2123456", vehicle.findVin())
    }

    private fun remoteWith(vararg fields: Pair<String, String>) = LubeLoggerVehicleDto(
        id = 1,
        extraFields = fields.map { (name, value) -> LubeLoggerExtraFieldDto(name, value) },
    )

    private val blankLocal = com.fearmikey.garage.data.local.entity.Vehicle(year = 1998, make = "Chevy", model = "Blazer")

    @Test
    fun `purchase condition and mileage are read from extra fields`() {
        val remote = remoteWith("purchase condition" to "New", "Purchase-Mileage" to "12,345 mi")
        assertEquals(true, remote.findPurchasedNew())
        assertEquals(12345, remote.findPurchaseOdometer())
        assertEquals(false, remoteWith("Condition" to "Used").findPurchasedNew())
        assertNull(remoteWith("Condition" to "Mint").findPurchasedNew())
        assertNull(remoteWith().findPurchaseOdometer())
    }

    private fun merge(local: com.fearmikey.garage.data.local.entity.Vehicle, remote: LubeLoggerVehicleDto, base: VehicleSyncDetails?, unit: String = "imperial") =
        local.withSyncDetails(mergeVehicleSyncDetails(local.syncDetails(), remote.syncDetails(unit), base))

    @Test
    fun `first sync fills blank garage vehicle from lubelogger`() {
        val remote = remoteWith("Purchase Condition" to "New", "Purchase Mileage" to "15")
            .copy(licensePlate = "1GNDT13W6W2123456")
        val result = merge(blankLocal, remote, base = null)

        assertEquals("1GNDT13W6W2123456", result.vin)
        assertTrue(result.purchasedNew)
        assertEquals(15, result.initialMileage)
    }

    @Test
    fun `first sync keeps values already set in garage`() {
        val local = blankLocal.copy(vin = "1GNDT13W6W2999999", purchasedNew = true, initialMileage = 40000)
        val remote = remoteWith("Purchase Condition" to "Used", "Purchase Mileage" to "50000")
            .copy(licensePlate = "1GNDT13W6W2123456")

        assertEquals(local, merge(local, remote, base = null))
    }

    @Test
    fun `change made in garage wins and is pushed`() {
        val base = VehicleSyncDetails("", "New", "100")
        val local = blankLocal.copy(purchasedNew = false, initialMileage = 200)
        val remote = remoteWith("Purchase Condition" to "New", "Purchase Mileage" to "100")

        val result = merge(local, remote, base)
        assertEquals(false, result.purchasedNew)
        assertEquals(200, result.initialMileage)

        val update = remote.copy(year = 1998, make = "Chevy", model = "Blazer", licensePlate = "GASHURTS")
            .toUpdateDto(result.syncDetails(), "imperial")!!
        val values = update.extraFields.associate { it.name to it.value }
        assertEquals("Used", values["Purchase Condition"])
        assertEquals("200", values["Purchase Mileage"])
    }

    @Test
    fun `change made in lubelogger is pulled`() {
        val base = VehicleSyncDetails("", "Used", "100")
        val local = blankLocal.copy(purchasedNew = false, initialMileage = 100)
        val remote = remoteWith("VIN" to "1GNDT13W6W2123456", "Purchase Condition" to "New", "Purchase Mileage" to "300")

        val result = merge(local, remote, base)
        assertEquals("1GNDT13W6W2123456", result.vin)
        assertTrue(result.purchasedNew)
        assertEquals(300, result.initialMileage)
    }

    @Test
    fun `no update is sent when server already matches`() {
        val remote = remoteWith("VIN" to "1gndt13w6w2123456", "Purchase Condition" to "used", "Purchase Mileage" to "100")
            .copy(year = 1998, make = "Chevy", model = "Blazer", licensePlate = "GASHURTS")
        assertNull(remote.toUpdateDto(VehicleSyncDetails("1GNDT13W6W2123456", "Used", "100"), "imperial"))
    }

    @Test
    fun `update keeps other server fields and existing field names`() {
        val remote = LubeLoggerVehicleDto(
            id = 3, year = 2023, make = "TOYOTA", model = "Tacoma", licensePlate = "3TYDZ5BN2PT021795",
            vehicleIdentifier = "LicensePlate", isDiesel = true, useHours = false, odometerOptional = true,
            tags = listOf("daily", "truck"),
            extraFields = listOf(
                LubeLoggerExtraFieldDto("Color", "Gray", isRequired = true, fieldType = 0),
                LubeLoggerExtraFieldDto("Condition", "Used", isRequired = false, fieldType = 0),
            ),
        )
        val update = remote.toUpdateDto(VehicleSyncDetails("3TYDZ5BN2PT021795", "New", ""), "imperial")!!

        assertEquals("Diesel", update.fuelType)
        assertEquals("true", update.odometerOptional)
        assertEquals("daily truck", update.tags)
        assertEquals("3TYDZ5BN2PT021795", update.licensePlate)
        assertEquals(
            listOf(
                LubeLoggerExtraFieldDto("Color", "Gray", true, 0),
                LubeLoggerExtraFieldDto("Condition", "New", false, 0),
                LubeLoggerExtraFieldDto("VIN", "3TYDZ5BN2PT021795", false, 0),
            ),
            update.extraFields,
        )
    }

    @Test
    fun `metric purchase mileage is converted both ways`() {
        val remote = remoteWith("Purchase Mileage" to "160934")
            .copy(year = 1998, make = "Chevy", model = "Blazer", licensePlate = "GASHURTS")
        assertEquals(100000, merge(blankLocal, remote, null, "metric").initialMileage)

        val update = remote.toUpdateDto(VehicleSyncDetails("", "Used", "50000"), "metric")!!
        assertEquals("80467", update.extraFields.first { it.name == "Purchase Mileage" }.value)
    }

    @Test
    fun `clearing a value in lubelogger clears it in garage`() {
        val base = VehicleSyncDetails("1GNDT13W6W2123456", "Used", "100")
        val local = blankLocal.copy(vin = "1GNDT13W6W2123456", initialMileage = 100)
        val remote = remoteWith("VIN" to "", "Purchase Condition" to "Used", "Purchase Mileage" to "")

        val result = merge(local, remote, base)
        assertEquals("", result.vin)
        assertNull(result.initialMileage)
    }

    private val serverVehicle = LubeLoggerVehicleDto(
        id = 3, year = 2023, make = "TOYOTA", model = "Tacoma", licensePlate = "ABC1234",
        extraFields = listOf(LubeLoggerExtraFieldDto("Trim", "SR5"), LubeLoggerExtraFieldDto("Purchase Condition", "Used")),
    )

    @Test
    fun `identity trim and plate pull from lubelogger when only the server changed`() {
        val local = com.fearmikey.garage.data.local.entity.Vehicle(year = 2023, make = "TOYOTA", model = "Tacoma", trim = "SR5")
        val base = local.syncDetails(plate = "ABC1234")
        val remote = serverVehicle.copy(model = "Tacoma TRD", licensePlate = "XYZ9876",
            extraFields = listOf(LubeLoggerExtraFieldDto("Trim", "TRD Off-Road"), LubeLoggerExtraFieldDto("Purchase Condition", "Used")))

        val merged = mergeVehicleSyncDetails(local.syncDetails(plate = "ABC1234"), remote.syncDetails("imperial"), base)
        val result = local.withSyncDetails(merged)
        assertEquals("Tacoma TRD", result.model)
        assertEquals("TRD Off-Road", result.trim)
        assertEquals("XYZ9876", merged.plate)
        assertNull(remote.toUpdateDto(merged, "imperial"))
    }

    @Test
    fun `identity trim and plate edited in garage are pushed`() {
        val original = com.fearmikey.garage.data.local.entity.Vehicle(year = 2023, make = "TOYOTA", model = "Tacoma", trim = "SR5")
        val base = original.syncDetails(plate = "ABC1234")
        val edited = original.copy(year = 2024, trim = "Limited")

        val merged = mergeVehicleSyncDetails(edited.syncDetails(plate = "NEW123"), serverVehicle.syncDetails("imperial"), base)
        val update = serverVehicle.toUpdateDto(merged, "imperial")!!
        assertEquals("2024", update.year)
        assertEquals("NEW123", update.licensePlate)
        assertEquals("Limited", update.extraFields.first { it.name == "Trim" }.value)
    }

    @Test
    fun `vin or N-A stored in license plate is not treated as a plate`() {
        assertEquals("", serverVehicle.copy(licensePlate = "3TYDZ5BN2PT021795").syncDetails("imperial").plate)
        assertEquals("", serverVehicle.copy(licensePlate = "N/A").syncDetails("imperial").plate)
    }

    @Test
    fun `blank garage plate never clears the server plate`() {
        val local = com.fearmikey.garage.data.local.entity.Vehicle(year = 2023, make = "TOYOTA", model = "Tacoma", trim = "SR5")
        val merged = mergeVehicleSyncDetails(local.syncDetails(plate = ""), serverVehicle.syncDetails("imperial"), local.syncDetails(plate = "ABC1234"))
        assertEquals("", merged.plate)
        assertNull(serverVehicle.toUpdateDto(merged, "imperial"))
    }

    @Test
    fun `blank vin extra field overrides vin in license plate`() {
        val remote = remoteWith("VIN" to "").copy(licensePlate = "1GNDT13W6W2123456")
        assertNull(remote.findVin())
    }

    @Test
    fun `createLubeLoggerOdometerDto creates correct DTO with imperial and metric conversion`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.MARCH, 1, 12, 0, 0)
        }
        val dateMillis = cal.timeInMillis

        // Imperial
        val dtoImperial = createLubeLoggerOdometerDto(
            lubeLoggerVehicleId = 42,
            dateMillis = dateMillis,
            mileageMiles = 50000,
            notes = "Fuel fill-up",
            lubeLoggerUnitSystem = "imperial"
        )
        assertEquals("42", dtoImperial.vehicleId)
        assertEquals("2025-03-01", dtoImperial.date)
        assertEquals("50000", dtoImperial.odometer)
        assertEquals("Fuel fill-up", dtoImperial.notes)

        // Metric (50000 miles -> 80467 km)
        val dtoMetric = createLubeLoggerOdometerDto(
            lubeLoggerVehicleId = 42,
            dateMillis = dateMillis,
            mileageMiles = 50000,
            notes = "Oil Change",
            lubeLoggerUnitSystem = "metric"
        )
        assertEquals("42", dtoMetric.vehicleId)
        assertEquals("2025-03-01", dtoMetric.date)
        assertEquals("80467", dtoMetric.odometer)
        assertEquals("Oil Change", dtoMetric.notes)
    }
}