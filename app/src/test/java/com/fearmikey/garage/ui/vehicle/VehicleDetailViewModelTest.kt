package com.fearmikey.garage.ui.vehicle

import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.ImageStorageManager
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.ui.navigation.Destinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        val vehicleFlow = MutableStateFlow<List<Vehicle>>(
            listOf(
                Vehicle(
                    id = 1L,
                    make = "Toyota",
                    model = "Tacoma",
                    year = 2020,
                )
            )
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleFlow.map { list -> list.find { it.id == vehicleId } }
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleFlow.value.find { it.id == vehicleId }
        override suspend fun upsert(vehicle: Vehicle): Long = 1L
        override suspend fun update(vehicle: Vehicle) {}
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakeVehicleSpecsDao : VehicleSpecsDao {
        override fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?> = MutableStateFlow(null)
        override suspend fun upsert(specs: VehicleSpecs) {}
    }

    private class FakeVehiclePartsDao : VehiclePartsDao {
        override fun getByVehicleId(vehicleId: Long): Flow<VehiclePartsInfo?> = MutableStateFlow(null)
        override suspend fun upsert(info: VehiclePartsInfo) {}
    }

    private class FakeVehicleRegistrationDao : VehicleRegistrationDao {
        override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
        override suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance) {}
        override suspend fun deleteByVehicleId(vehicleId: Long) {}
    }

    private class FakeVinDecoderApi : VinDecoderApi {
        override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse = VinDecodeResponse(emptyList())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shouldOpenAddSheet is true initially when openAdd arg is set and false after consumeAddSheet`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                Destinations.VEHICLE_ID_ARG to 1L,
                Destinations.VEHICLE_DETAIL_TAB_ARG to VehicleTab.FUEL.ordinal,
                Destinations.VEHICLE_DETAIL_OPEN_ADD_ARG to true,
            )
        )

        val vehicleRepository = VehicleRepository(
            vehicleDao = FakeVehicleDao(),
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = VehicleDetailViewModel(
            savedStateHandle = savedStateHandle,
            vehicleRepository = vehicleRepository,
            imageStorageManager = ImageStorageManager(context = ContextWrapper(null)),
        )

        assertEquals(VehicleTab.FUEL.ordinal, viewModel.initialTab)
        assertTrue(viewModel.shouldOpenAddSheet.value)

        viewModel.consumeAddSheet()

        assertFalse(viewModel.shouldOpenAddSheet.value)
        assertEquals(false, savedStateHandle.get<Boolean>(Destinations.VEHICLE_DETAIL_OPEN_ADD_ARG))
    }
}
