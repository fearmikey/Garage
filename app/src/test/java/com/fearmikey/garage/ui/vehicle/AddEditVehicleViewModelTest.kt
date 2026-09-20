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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditVehicleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        var savedVehicle: Vehicle? = null
        val vehicleFlow = MutableStateFlow<List<Vehicle>>(
            listOf(
                Vehicle(
                    id = 1L,
                    make = "Toyota",
                    model = "Tacoma",
                    year = 2020,
                    imageUri = "img1.jpg",
                    imageOffsetY = 0.1f,
                    imageUri2 = "img2.jpg",
                    imageOffsetY2 = 0.2f,
                )
            )
        )
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicleFlow.map { list -> list.find { it.id == vehicleId } }
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicleFlow.value.find { it.id == vehicleId }
        override suspend fun upsert(vehicle: Vehicle): Long {
            savedVehicle = vehicle
            return vehicle.id
        }
        override suspend fun update(vehicle: Vehicle) { savedVehicle = vehicle }
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

    private class FakeImageStorageManager : ImageStorageManager(ContextWrapper(null)) {
        override fun imageFile(filename: String): File = File(filename)
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
    fun `editing vehicle loads existing photos up to 3`() = runTest {
        val fakeDao = FakeVehicleDao()
        val vehicleRepository = VehicleRepository(
            vehicleDao = fakeDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = AddEditVehicleViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L)),
            vehicleRepository = vehicleRepository,
            imageStorageManager = FakeImageStorageManager(),
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val photos = viewModel.uiState.value.photos
        assertEquals(2, photos.size)
        assertEquals("img1.jpg", photos[0].filename)
        assertEquals(0.1f, photos[0].offsetY)
        assertEquals("img2.jpg", photos[1].filename)
        assertEquals(0.2f, photos[1].offsetY)
    }

    @Test
    fun `removing a photo updates state and save persists correct fields`() = runTest {
        val fakeDao = FakeVehicleDao()
        val vehicleRepository = VehicleRepository(
            vehicleDao = fakeDao,
            vehicleSpecsDao = FakeVehicleSpecsDao(),
            vehiclePartsDao = FakeVehiclePartsDao(),
            vehicleRegistrationDao = FakeVehicleRegistrationDao(),
            vinDecoderApi = FakeVinDecoderApi(),
        )

        val viewModel = AddEditVehicleViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Destinations.VEHICLE_ID_ARG to 1L)),
            vehicleRepository = vehicleRepository,
            imageStorageManager = FakeImageStorageManager(),
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRemovePhoto(0)

        assertEquals(1, viewModel.uiState.value.photos.size)
        assertEquals("img2.jpg", viewModel.uiState.value.photos[0].filename)

        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeDao.savedVehicle
        assertEquals("img2.jpg", saved?.imageUri)
        assertEquals(0.2f, saved?.imageOffsetY)
        assertNull(saved?.imageUri2)
        assertNull(saved?.imageUri3)
    }
}
