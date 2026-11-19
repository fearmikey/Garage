package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.ChargerSpeed
import com.fearmikey.garage.data.local.entity.ChargerVendorType
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationCategory
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.VehicleNote
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Covers data LubeLogger keeps that Garage must not wipe on update, and the Garage fields
 * synced through "Garage ..." extra fields.
 */
class LubeLoggerSyncFieldsTest {

    private val day: Long = Calendar.getInstance().apply { set(2025, Calendar.MARCH, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private val attachment = JsonObject().apply { addProperty("name", "receipt.pdf"); addProperty("location", "/documents/receipt.pdf") }
    private val userField = LubeLoggerExtraFieldDto(name = "Shop", value = "Joe's Garage")

    // --- Server-only data survives a Garage edit -----------------------------------------------

    @Test
    fun `service update keeps server notes, tags, attachments and user extra fields`() {
        val server = LubeLoggerServiceRecordDto(
            id = 7, date = "2025-03-01", description = "Oil change", notes = "Used 5W-30", tags = "oil diy",
            extraFields = listOf(userField, LubeLoggerExtraFieldDto("Garage Category", "OTHER")),
            files = listOf(attachment),
        )
        val local = MaintenanceRecord(
            vehicleId = 1, date = day, mileage = 50000.0, description = "Oil change", cost = 60.0,
            category = MaintenanceCategory.FLUIDS, taskName = "Oil change", lubeLoggerId = 7,
        )

        val dto = local.toLubeLoggerDto(42, remote = server)

        assertEquals("Used 5W-30", dto.notes)
        assertEquals("oil diy", dto.tags)
        assertEquals(listOf(attachment), dto.files)
        val fields = dto.extraFields!!.associate { it.name to it.value }
        assertEquals("Joe's Garage", fields["Shop"])
        assertEquals("FLUIDS", fields["Garage Category"])
        assertEquals(1, dto.extraFields!!.count { it.name == "Garage Category" })
    }

    @Test
    fun `fuel update keeps missed fuel-up, notes, tags and attachments`() {
        val server = LubeLoggerGasRecordDto(
            id = 3, date = "2025-03-01", missedFuelUp = "True", notes = "Road trip", tags = "trip",
            files = listOf(attachment), extraFields = listOf(userField),
        )
        val local = FuelRecord(vehicleId = 1, date = day, mileage = 1000.0, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, lubeLoggerId = 3)

        val dto = local.toLubeLoggerDto(42, remote = server)

        assertEquals("True", dto.missedFuelUp)
        assertEquals("Road trip", dto.notes)
        assertEquals("trip", dto.tags)
        assertEquals(listOf(attachment), dto.files)
        assertEquals(listOf(userField), dto.extraFields)
    }

    @Test
    fun `new fuel record is not a missed fuel-up`() {
        val local = FuelRecord(vehicleId = 1, date = day, mileage = 1000.0, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5)
        assertEquals("False", local.toLubeLoggerDto(42).missedFuelUp)
    }

    @Test
    fun `odometer update keeps equipment links, initial odometer and attachments`() {
        val server = LubeLoggerOdometerRecordDto(
            id = "9", date = "2025-03-01", initialOdometer = "49000", odometer = "50000",
            equipmentRecordId = "2 5", files = listOf(attachment), tags = "winter",
        )
        val checkIn = MaintenanceRecord(
            vehicleId = 1, date = day, mileage = 50100.0, description = "Odometer check-in", cost = 0.0,
            category = MaintenanceCategory.INSPECTION, taskName = ODOMETER_CHECK_IN_TASK, lubeLoggerId = 9,
        )

        val dto = checkIn.toLubeLoggerOdometerDto(42, remote = server)

        assertEquals("2 5", dto.equipmentRecordId)
        assertEquals("49000", dto.initialOdometer)
        assertEquals("winter", dto.tags)
        assertEquals(listOf(attachment), dto.files)
        assertEquals("50100", dto.odometer)
    }

    // --- Fuel grade --------------------------------------------------------------------------

    @Test
    fun `fuel grade round-trips through an extra field`() {
        val local = FuelRecord(vehicleId = 1, date = day, mileage = 1000.0, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5, fuelGrade = "PREMIUM")
        val pulled = local.toLubeLoggerDto(42).copy(id = 3).toFuelRecord(1)!!
        assertEquals("PREMIUM", pulled.fuelGrade)
        assertEquals(local.syncFingerprint(), pulled.syncFingerprint())
    }

    @Test
    fun `fuel fingerprint without a grade keeps the pre-grade format`() {
        val local = FuelRecord(vehicleId = 1, date = day, mileage = 1000.0, gallons = 10.0, totalCost = 35.0, pricePerGallon = 3.5)
        assertEquals("2025-03-01|1000|10.00|35.00|true", local.syncFingerprint())
    }

    // --- Charging --------------------------------------------------------------------------

    @Test
    fun `charging fingerprint compares by day, not time of day`() {
        val morning = ChargingRecord(vehicleId = 1, date = day + 9 * 3600_000L, mileage = 1000.0, kwhAdded = 40.0)
        val pulled = morning.toLubeLoggerDto(42).copy(id = 5).toChargingRecord(1)!!
        assertEquals(morning.syncFingerprint(), pulled.syncFingerprint())
    }

    @Test
    fun `charging details round-trip in metric`() {
        val local = ChargingRecord(
            vehicleId = 1, date = day, mileage = 1000.0, kwhAdded = 40.0,
            chargerSpeed = ChargerSpeed.DC_FAST, vendorType = ChargerVendorType.PUBLIC, estimatedRangeAt100 = 300,
            vendor = "Ionna",
        )
        val pulled = local.toLubeLoggerDto(42, "metric").copy(id = 5).toChargingRecord(1, "metric")!!
        assertEquals(ChargerSpeed.DC_FAST, pulled.chargerSpeed)
        assertEquals(ChargerVendorType.PUBLIC, pulled.vendorType)
        assertEquals(300, pulled.estimatedRangeAt100)
        assertEquals("Ionna", pulled.vendor)
        assertEquals(local.syncFingerprint("metric"), pulled.syncFingerprint("metric"))
    }

    // --- Maintenance deferral and linking ---------------------------------------------------

    @Test
    fun `deferral intervals round-trip in metric`() {
        val local = MaintenanceRecord(
            vehicleId = 1, date = day, mileage = 50000.0, description = "Brake pads checked", cost = 0.0,
            category = MaintenanceCategory.BRAKES, taskName = "Brake pads", isDeferred = true,
            deferredMiles = 5000.0, deferredMonths = 6,
        )
        val pulled = local.toLubeLoggerDto(42, "metric").copy(id = 8).toMaintenanceRecord(1, "metric")!!
        assertEquals(5000.0, pulled.deferredMiles!!, 0.5)
        assertEquals(6, pulled.deferredMonths)
        assertEquals(local.syncFingerprint("metric"), pulled.syncFingerprint("metric"))
    }

    @Test
    fun `different jobs at the same visit are not the same work`() {
        val oil = MaintenanceRecord(vehicleId = 1, date = day, mileage = 50000.0, description = "Oil change", cost = 60.0, category = MaintenanceCategory.FLUIDS, taskName = "Oil change")
        val tires = oil.copy(description = "Tire rotation", category = MaintenanceCategory.TIRES, taskName = "Tire rotation")
        assertFalse(oil.describesSameWorkAs(tires))
        assertTrue(oil.describesSameWorkAs(oil.copy(description = " oil CHANGE ")))
        assertTrue(oil.describesSameWorkAs(oil.copy(description = "Synthetic oil + filter")))
    }

    // --- Upgrades ---------------------------------------------------------------------------

    @Test
    fun `upgrade title containing a dash survives the round trip`() {
        val mod = ModificationRecord(
            vehicleId = 1, title = "Exhaust - Borla", category = ModificationCategory.EXHAUST,
            description = "", date = day, cost = 900.0, productUrl = "https://example.com/borla",
        )
        val pulled = mod.toLubeLoggerUpgradeDto(42).copy(id = 4).toModificationRecord(1)!!
        assertEquals("Exhaust - Borla", pulled.title)
        assertEquals("", pulled.description)
        assertEquals(ModificationCategory.EXHAUST, pulled.category)
        assertEquals("https://example.com/borla", pulled.productUrl)
        assertEquals(mod.syncFingerprint(), pulled.syncFingerprint())
    }

    @Test
    fun `legacy Garage upgrade format is still split`() {
        val legacy = LubeLoggerUpgradeRecordDto(id = 4, date = "2025-03-01", description = "Lift Kit - 2 inch lift", cost = "1200")
        val pulled = legacy.toModificationRecord(1)!!
        assertEquals("Lift Kit", pulled.title)
        assertEquals("2 inch lift", pulled.description)
    }

    @Test
    fun `upgrade created in LubeLogger uses notes as the description`() {
        val native = LubeLoggerUpgradeRecordDto(id = 4, date = "2025-03-01", description = "LED headlights - pair", notes = "Installed at dealer")
        val pulled = native.toModificationRecord(1)!!
        assertEquals("LED headlights - pair", pulled.title)
        assertEquals("Installed at dealer", pulled.description)
    }

    @Test
    fun `upgrade update keeps the server odometer`() {
        val server = LubeLoggerUpgradeRecordDto(id = 4, date = "2025-03-01", description = "Lift Kit", odometer = "61000", mileage = 61000)
        val mod = ModificationRecord(vehicleId = 1, title = "Lift Kit", category = ModificationCategory.SUSPENSION, description = "", date = day, lubeLoggerId = 4)
        assertEquals("61000", mod.toLubeLoggerUpgradeDto(42, latestMileage = 45000.0, remote = server).odometer)
    }

    // --- Notes ------------------------------------------------------------------------------

    @Test
    fun `notes round-trip and keep the pinned flag`() {
        val note = VehicleNote(vehicleId = 1, title = "Jack points", content = "Use the pinch welds", category = "General", lastUpdated = day, lubeLoggerId = 2)
        val server = LubeLoggerNoteRecordDto(id = "2", description = "Jack points", noteText = "old", pinned = "True")
        val dto = note.toLubeLoggerNoteDto(42, server)
        assertEquals("True", dto.pinned)
        val pulled = dto.toVehicleNote(1)!!
        assertEquals(note.syncFingerprint(), pulled.syncFingerprint())
    }

    @Test
    fun `blank notes are not pushed`() {
        val note = VehicleNote(vehicleId = 1, title = "Empty", content = "  ", category = "General", lastUpdated = day)
        assertFalse(note.toLubeLoggerNoteDto(42).isPushable)
    }

    // --- Purchase info ----------------------------------------------------------------------

    private fun info(date: String, price: String) = VehiclePurchaseInfo(date, price)

    @Test
    fun `purchase info normalizes both sides the same way`() {
        val local = VehicleRegistrationInsurance(vehicleId = 1, purchaseDate = day, purchasePrice = 15000.0).purchaseInfo()
        val remote = LubeLoggerVehicleDto(id = 1, purchaseDate = "2025-03-01", purchasePrice = 15000.0).purchaseInfo()
        assertEquals(local, remote)
        assertEquals(info("", ""), LubeLoggerVehicleDto(id = 1, purchaseDate = "", purchasePrice = 0.0).purchaseInfo())
        assertEquals(info("", ""), (null as VehicleRegistrationInsurance?).purchaseInfo())
    }

    @Test
    fun `first purchase sync only fills what Garage lacks`() {
        val merged = mergePurchaseInfo(info("", "15000.00"), info("2025-03-01", "14000.00"), base = null, garageCanPush = true)
        assertEquals(info("2025-03-01", "15000.00"), merged)
    }

    @Test
    fun `server purchase change is pulled, Garage change is kept`() {
        val base = info("2025-03-01", "15000.00")
        assertEquals(info("2025-03-02", "15000.00"), mergePurchaseInfo(base, base.copy(date = "2025-03-02"), base, garageCanPush = true))
        assertEquals(info("2025-03-01", "16000.00"), mergePurchaseInfo(base.copy(price = "16000.00"), base, base, garageCanPush = true))
        // Both changed: Garage wins when it can push...
        assertEquals("16000.00", mergePurchaseInfo(base.copy(price = "16000.00"), base.copy(price = "17000.00"), base, true).price)
        // ...and the server wins when Garage can't (API key auth).
        assertEquals("17000.00", mergePurchaseInfo(base.copy(price = "16000.00"), base.copy(price = "17000.00"), base, false).price)
    }

    @Test
    fun `purchase price form values avoid culture-specific decimals for whole amounts`() {
        assertEquals("15000", purchasePriceFormValue("15000.00"))
        assertEquals("15000.50", purchasePriceFormValue("15000.50"))
        assertEquals("0", purchasePriceFormValue(""))
    }

    @Test
    fun `older servers without purchase fields report nothing`() {
        val old = LubeLoggerVehicleDto(id = 1)
        assertNull(old.purchaseDate)
        assertNull(old.purchasePrice)
    }
}
