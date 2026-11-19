package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.SupplyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class LubeLoggerSupplyMapperTest {

    private val purchaseDate = Calendar.getInstance().apply {
        set(2025, Calendar.MARCH, 3, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val supply = SupplyRecord(
        id = 7,
        vehicleId = 3,
        description = " Oil filter ",
        partNumber = "PF48",
        supplier = "AutoZone",
        quantity = 2.0,
        cost = 17.98,
        date = purchaseDate,
        notes = "Box of two",
        lubeLoggerId = 42,
    )

    @Test
    fun `supply maps to LubeLogger dto with invariant numbers`() {
        val dto = supply.toLubeLoggerSupplyDto(lubeLoggerVehicleId = 9)

        assertEquals("42", dto.id)
        assertEquals("9", dto.vehicleId)
        assertEquals("2025-03-03", dto.date)
        assertEquals("Oil filter", dto.description)
        assertEquals("PF48", dto.partNumber)
        assertEquals("AutoZone", dto.partSupplier)
        assertEquals("2.00", dto.partQuantity)
        assertEquals("17.98", dto.cost)
        assertEquals("Box of two", dto.notes)
    }

    @Test
    fun `dto round trip keeps the fingerprint and the local id`() {
        val dto = supply.toLubeLoggerSupplyDto(lubeLoggerVehicleId = 9)
        val pulled = dto.toSupplyRecord(localVehicleId = 3, existing = supply)!!

        assertEquals(supply.id, pulled.id)
        assertEquals(42, pulled.lubeLoggerId)
        assertEquals(supply.syncFingerprint(), pulled.syncFingerprint())
    }

    @Test
    fun `server supply with string numbers and US date is parsed`() {
        val dto = LubeLoggerSupplyRecordDto(
            id = "15",
            date = "03/03/2025",
            description = "Coolant",
            partQuantity = "1.5",
            cost = "1,024.50",
        )

        val local = dto.toSupplyRecord(localVehicleId = 3)!!

        assertEquals(0L, local.id)
        assertEquals("Coolant", local.description)
        assertEquals("", local.partNumber)
        assertEquals(1.5, local.quantity, 0.0)
        assertEquals(1024.5, local.cost, 0.0)
        assertEquals("2025-03-03", formatDate(local.date))
    }

    @Test
    fun `supply without a usable id is skipped`() {
        assertNull(LubeLoggerSupplyRecordDto(id = null, description = "x").toSupplyRecord(1))
        assertNull(LubeLoggerSupplyRecordDto(id = "0", description = "x").toSupplyRecord(1))
    }

    @Test
    fun `quantity change alters the fingerprint`() {
        // LubeLogger lowers the quantity when a supply is used in a record; Garage must pull that.
        assertNotEquals(supply.syncFingerprint(), supply.copy(quantity = 1.0).syncFingerprint())
    }
}
