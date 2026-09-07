package com.fearmikey.garage.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleRepositoryTest {

    @Test
    fun `cleanSpec passes through real values`() {
        assertEquals("V6", VehicleRepository.cleanSpec("V6"))
        assertEquals("Toyota", VehicleRepository.cleanSpec("  Toyota  "))
    }

    @Test
    fun `cleanSpec blanks out NHTSA placeholder values`() {
        assertNull(VehicleRepository.cleanSpec("Not Applicable"))
        assertNull(VehicleRepository.cleanSpec("N/A"))
        assertNull(VehicleRepository.cleanSpec("Not Available"))
        assertNull(VehicleRepository.cleanSpec("unknown"))
        assertNull(VehicleRepository.cleanSpec("None"))
    }

    @Test
    fun `cleanSpec blanks out null and empty values`() {
        assertNull(VehicleRepository.cleanSpec(null))
        assertNull(VehicleRepository.cleanSpec(""))
        assertNull(VehicleRepository.cleanSpec("   "))
    }
}
