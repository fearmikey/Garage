package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.remote.dto.VinDecodeResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `parseErrorCodes splits NHTSA comma-separated codes`() {
        assertEquals(setOf(0, 14), VehicleRepository.parseErrorCodes("0,14"))
        assertEquals(setOf(6, 7, 11), VehicleRepository.parseErrorCodes(" 6, 7 ,11"))
        assertEquals(emptySet<Int>(), VehicleRepository.parseErrorCodes(null))
    }

    @Test
    fun `clean decode with informational code 14 has no warning`() {
        assertNull(VehicleRepository.warningFor(setOf(0, 14)))
        assertNull(VehicleRepository.warningFor(setOf(0)))
    }

    @Test
    fun `invalid model year produces a warning`() {
        assertTrue(VehicleRepository.warningFor(setOf(7, 11))!!.contains("Model year"))
    }

    @Test
    fun `incomplete VIN gets a specific error message`() {
        assertTrue(VehicleRepository.errorMessageFor(setOf(6, 7, 11)).contains("incomplete"))
    }

    @Test
    fun `decodeVin succeeds when NHTSA returns 0 and 14`() = runBlocking {
        val repo = repoReturning(VinDecodeResult(errorCode = "0,14", make = "KTM", model = "390 Duke", modelYear = "2021"))
        val result = repo.decodeVin("VBKJPJ407MC000000") as VinLookupResult.Success
        assertEquals("KTM", result.info.make)
        assertEquals(2021, result.info.year)
        assertNull(result.warning)
    }

    @Test
    fun `decodeVin drops untrusted year on code 11 but keeps other data`() = runBlocking {
        val repo = repoReturning(VinDecodeResult(errorCode = "7,11", make = "Ducati", modelYear = "2001"))
        val result = repo.decodeVin("ZDM1234567B000000") as VinLookupResult.Success
        assertEquals("Ducati", result.info.make)
        assertNull(result.info.year)
        assertNotNull(result.warning)
    }

    @Test
    fun `decodeVin fails when no vehicle data is returned`() = runBlocking {
        val repo = repoReturning(VinDecodeResult(errorCode = "7"))
        assertTrue(repo.decodeVin("ZDM1234567B000000") is VinLookupResult.Error)
    }

    private fun repoReturning(result: VinDecodeResult): VehicleRepository {
        val api = object : VinDecoderApi {
            override suspend fun decodeVin(vin: String, format: String) = VinDecodeResponse(listOf(result))
        }
        // DAOs are never touched by decodeVin, so unimplemented proxies are sufficient.
        return VehicleRepository(
            vehicleDao = unusedDao(),
            vehicleSpecsDao = unusedDao(),
            vehiclePartsDao = unusedDao(),
            vehicleRegistrationDao = unusedDao(),
            vinDecoderApi = api,
        )
    }

    private inline fun <reified T> unusedDao(): T =
        java.lang.reflect.Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ ->
            throw UnsupportedOperationException()
        } as T

    @Test
    fun `cleanSpec handles towing capacity strings`() {
        assertEquals("6400", VehicleRepository.cleanSpec("6400"))
        assertEquals("6400 lbs", VehicleRepository.cleanSpec(" 6400 lbs "))
        assertNull(VehicleRepository.cleanSpec("Not Applicable"))
    }
}
