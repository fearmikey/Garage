package com.fearmikey.garage.ui.obd

import android.content.ContextWrapper
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.remote.VinDecoderApi
import com.fearmikey.garage.data.remote.dto.VinDecodeResponse
import com.fearmikey.garage.data.repository.MaintenanceRepository
import com.fearmikey.garage.data.repository.PreferencesRepository
import com.fearmikey.garage.data.repository.VehicleRepository
import com.fearmikey.garage.obd.FakeElm327
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdAdapterType
import com.fearmikey.garage.obd.ObdConnectionManager
import com.fearmikey.garage.obd.ObdDevice
import com.fearmikey.garage.obd.ObdParser
import com.fearmikey.garage.obd.ObdPid
import com.fearmikey.garage.obd.ObdScanSummary
import com.fearmikey.garage.ui.util.AppCurrency
import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObdViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeObdConnectionManager(
        private val dispatcher: CoroutineDispatcher,
        var elm: FakeElm327 = FakeElm327.canCar(),
    ) : ObdConnectionManager(ContextWrapper(null)) {
        val connectAttempts = mutableListOf<ObdAdapterConfig>()
        var connectResult = true
        var fakePairedDevices = listOf(ObdDevice("OBDII", "00:11:22:33:44:55"))
        var fakeBleDevices = listOf(
            ObdDevice("Heart Rate Strap", "11:11:11:11:11:11", ObdAdapterType.BLE),
            ObdDevice("IOS-Vlink", "22:22:22:22:22:22", ObdAdapterType.BLE),
        )

        /** Called on each connect; lets a test swap in a fresh emulator after a dropout. */
        var onConnect: () -> Unit = {}

        override fun getPairedObdDevices(): List<ObdDevice> = fakePairedDevices
        override fun scanBleDevices(): Flow<ObdDevice> = flow { fakeBleDevices.forEach { emit(it) } }
        override suspend fun connect(config: ObdAdapterConfig): Boolean {
            connectAttempts += config
            onConnect()
            return connectResult
        }
        override fun createSession(): ObdParser = ObdParser(elm.input, elm.output, dispatcher)
        override fun disconnect() {}
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        private var nextId = 1L
        override fun getRecordsForVehicleByDate(vehicleId: Long) =
            records.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date } }
        override fun getRecordsForVehicleByMileage(vehicleId: Long) =
            records.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.mileage } }
        override fun getLatestMileageForVehicle(vehicleId: Long) =
            records.map { list -> list.filter { it.vehicleId == vehicleId }.maxOfOrNull { it.mileage } }
        override suspend fun upsert(record: MaintenanceRecord): Long {
            val id = nextId++
            records.value = records.value + record.copy(id = id)
            return id
        }
        override suspend fun update(record: MaintenanceRecord) {}
        override suspend fun delete(record: MaintenanceRecord) {}
    }

    private class FakeVehicleDao(vin: String) : VehicleDao {
        val vehicle = MutableStateFlow<Vehicle?>(Vehicle(id = VEHICLE_ID, vin = vin, make = "Honda", model = "Accord", year = 2003))
        override fun getAllVehicles(): Flow<List<Vehicle>> = vehicle.map { listOfNotNull(it) }
        override fun getVehicleById(vehicleId: Long): Flow<Vehicle?> = vehicle
        override suspend fun getVehicleByIdOnce(vehicleId: Long): Vehicle? = vehicle.value
        override suspend fun upsert(vehicle: Vehicle): Long {
            this.vehicle.value = vehicle
            return vehicle.id
        }
        override suspend fun update(vehicle: Vehicle) {
            this.vehicle.value = vehicle
        }
        override suspend fun delete(vehicle: Vehicle) {}
    }

    private class FakePreferencesRepository(adapter: ObdAdapterConfig?) : PreferencesRepository {
        val adapterFlow = MutableStateFlow(adapter)
        override val savedObdAdapter: Flow<ObdAdapterConfig?> = adapterFlow
        override val unitsType: Flow<String> = MutableStateFlow("metric")
        override val unitSystem: Flow<UnitSystem> = MutableStateFlow(UnitSystem.METRIC)
        override val currencyCode: Flow<String> = MutableStateFlow("USD")
        override val appCurrency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.USD)
        override val themeType: Flow<String> = MutableStateFlow("system")
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override val defaultVehicleId: Flow<Long?> = MutableStateFlow(null)
        override val maintenanceMileageWindow: Flow<Int> = MutableStateFlow(500)
        override val appOpenCount: Flow<Int> = MutableStateFlow(1)
        override val buyMeACoffeeDontAskAgain: Flow<Boolean> = MutableStateFlow(false)
        override val buyMeACoffeeNextPromptOpenCount: Flow<Int> = MutableStateFlow(2)

        override suspend fun setSavedObdAdapter(config: ObdAdapterConfig?) {
            adapterFlow.value = config
        }
        override suspend fun setUnitsType(units: String) {}
        override suspend fun setCurrencyCode(currencyCode: String) {}
        override suspend fun setThemeType(theme: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setDefaultVehicleId(vehicleId: Long?) {}
        override suspend fun setMaintenanceMileageWindow(miles: Int) {}
        override suspend fun incrementAppOpenCount(): Int = 1
        override suspend fun setBuyMeACoffeeDontAskAgain(dontAskAgain: Boolean) {}
        override suspend fun setBuyMeACoffeeNextPromptOpenCount(openCount: Int) {}
    }

    private class Harness(
        val manager: FakeObdConnectionManager,
        val dao: FakeMaintenanceDao,
        val vehicleDao: FakeVehicleDao,
        val prefs: FakePreferencesRepository,
        val viewModel: ObdViewModel,
    )

    private fun vehicleRepository(vehicleDao: VehicleDao) = VehicleRepository(
        vehicleDao = vehicleDao,
        vehicleSpecsDao = object : VehicleSpecsDao {
            override fun getByVehicleId(vehicleId: Long): Flow<VehicleSpecs?> = MutableStateFlow(null)
            override suspend fun upsert(specs: VehicleSpecs) {}
        },
        vehiclePartsDao = object : VehiclePartsDao {
            override fun getByVehicleId(vehicleId: Long): Flow<VehiclePartsInfo?> = MutableStateFlow(null)
            override suspend fun upsert(info: VehiclePartsInfo) {}
        },
        vehicleRegistrationDao = object : VehicleRegistrationDao {
            override fun getByVehicleId(vehicleId: Long) = MutableStateFlow(null)
            override suspend fun upsert(registrationInsurance: VehicleRegistrationInsurance) {}
            override suspend fun deleteByVehicleId(vehicleId: Long) {}
        },
        vinDecoderApi = object : VinDecoderApi {
            override suspend fun decodeVin(vin: String, format: String): VinDecodeResponse =
                VinDecodeResponse(results = emptyList())
        },
    )

    private fun harness(
        adapter: ObdAdapterConfig? = CLASSIC_ADAPTER,
        elm: FakeElm327 = FakeElm327.canCar(),
        vehicleVin: String = CAR_VIN,
    ): Harness {
        val manager = FakeObdConnectionManager(testDispatcher, elm)
        val dao = FakeMaintenanceDao()
        val vehicleDao = FakeVehicleDao(vehicleVin)
        val prefs = FakePreferencesRepository(adapter)
        val viewModel = ObdViewModel(manager, MaintenanceRepository(dao), prefs, vehicleRepository(vehicleDao), ContextWrapper(null))
        viewModel.setVehicleId(VEHICLE_ID)
        return Harness(manager, dao, vehicleDao, prefs, viewModel)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun idle() = testDispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `auto connects to saved adapter once and runs a full scan`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        assertEquals(listOf(CLASSIC_ADAPTER), h.manager.connectAttempts)
        val state = h.viewModel.uiState.value
        assertEquals(ObdViewModel.ConnectionStatus.CONNECTED, state.connectionStatus)
        val result = state.scanResult!!
        assertEquals(listOf("P0420", "P0700"), result.storedDtcs)
        assertEquals(listOf("Transmission (TCM)"), result.dtcSources["P0700"])
        assertEquals(listOf("P0171"), result.pendingDtcs)
        assertTrue(result.monitorStatus!!.milOn)
        assertEquals(CAR_VIN, result.vin)
        assertEquals(500, result.distanceSinceClearedKm)
        assertNull(result.odometerKm)
        assertEquals(12.6, result.adapterVoltage!!, 0.001)
        assertTrue(result.readings.any { it.pid == ObdPid.COOLANT_TEMP })
        assertNull(state.progressMessage)
        assertEquals(ObdViewModel.VinCheck.MATCH, state.vinCheck)

        // Re-granting (e.g. config change) must not trigger a second auto-connect.
        h.viewModel.onPermissionsGranted()
        idle()
        assertEquals(1, h.manager.connectAttempts.size)
    }

    @Test
    fun `freeze frame is read when there are stored codes`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        val frame = h.viewModel.uiState.value.scanResult!!.freezeFrame!!
        assertEquals("P0420", frame.dtc)
        assertTrue(frame.readings.any { it.pid == ObdPid.RPM })
    }

    @Test
    fun `freeze frame is skipped when there are no stored codes`() = runTest(testDispatcher) {
        val elm = FakeElm327.canCar().apply {
            headerResponses["03"] = "7E8 02 43 00"
        }
        val h = harness(elm = elm)
        h.viewModel.onPermissionsGranted()
        idle()

        assertNull(h.viewModel.uiState.value.scanResult!!.freezeFrame)
        assertFalse("020200" in elm.commands)
    }

    @Test
    fun `scan is logged to the timeline once and repeated identical scans are skipped`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        val scans = h.dao.records.value.filter { it.taskName == ObdScanSummary.SCAN_TASK_NAME }
        assertEquals(1, scans.size)
        assertTrue(scans.single().description.contains("P0420"))
        assertEquals(ObdViewModel.TimelineLogResult.SAVED, h.viewModel.uiState.value.timelineLogResult)

        h.viewModel.rescan()
        idle()
        assertEquals(1, h.dao.records.value.size)
        assertEquals(ObdViewModel.TimelineLogResult.UNCHANGED, h.viewModel.uiState.value.timelineLogResult)
    }

    @Test
    fun `vin mismatch holds back timeline logging until confirmed`() = runTest(testDispatcher) {
        val h = harness(vehicleVin = "JH4KA8260MC000000")
        h.viewModel.onPermissionsGranted()
        idle()

        val state = h.viewModel.uiState.value
        assertEquals(ObdViewModel.VinCheck.MISMATCH, state.vinCheck)
        assertEquals("JH4KA8260MC000000", state.vehicleVin)
        assertEquals(ObdViewModel.TimelineLogResult.SKIPPED_VIN_MISMATCH, state.timelineLogResult)
        assertTrue(h.dao.records.value.isEmpty())

        h.viewModel.logScanAnyway()
        idle()
        assertEquals(1, h.dao.records.value.size)
        assertEquals(ObdViewModel.TimelineLogResult.SAVED, h.viewModel.uiState.value.timelineLogResult)
    }

    @Test
    fun `vehicle without a vin can adopt the scanned vin`() = runTest(testDispatcher) {
        val h = harness(vehicleVin = "")
        h.viewModel.onPermissionsGranted()
        idle()

        assertEquals(ObdViewModel.VinCheck.VEHICLE_HAS_NO_VIN, h.viewModel.uiState.value.vinCheck)
        // Scans are still logged when the vehicle simply has no VIN on file.
        assertEquals(1, h.dao.records.value.size)

        h.viewModel.saveVinToVehicle()
        idle()
        assertEquals(CAR_VIN, h.vehicleDao.vehicle.value!!.vin)
        assertEquals(ObdViewModel.VinCheck.MATCH, h.viewModel.uiState.value.vinCheck)
    }

    @Test
    fun `odometer reading is converted to miles and logged`() = runTest(testDispatcher) {
        val elm = FakeElm327.canCar().apply {
            // Advertise PID A6 via the 0x40/0x60/0x80/0xA0 supported-PID bitmaps.
            responses["0140"] = "41 40 44 00 00 01"
            responses["0160"] = "41 60 00 00 00 01"
            responses["0180"] = "41 80 00 00 00 01"
            responses["01A0"] = "41 A0 04 00 00 00"
            responses["01A6"] = "41 A6 00 01 D4 C0" // 120000 -> 12000.0 km
        }
        val h = harness(elm = elm)
        h.viewModel.onPermissionsGranted()
        idle()

        assertEquals(12000, h.viewModel.uiState.value.scanResult!!.odometerKm)
        assertEquals(7456, h.dao.records.value.single().mileage)
    }

    @Test
    fun `stalled request is skipped without aborting the scan`() = runTest(testDispatcher) {
        val elm = FakeElm327.canCar().apply { silentCommands += "0902" }
        val h = harness(elm = elm)
        h.viewModel.onPermissionsGranted()
        idle()

        val state = h.viewModel.uiState.value
        assertEquals(ObdViewModel.ConnectionStatus.CONNECTED, state.connectionStatus)
        val result = state.scanResult!!
        assertNull(result.vin)
        assertEquals(listOf("P0420", "P0700"), result.storedDtcs)
        assertTrue(result.readings.isNotEmpty())
        assertEquals(1, result.failedRequests)
    }

    @Test
    fun `ignition off reports a helpful error`() = runTest(testDispatcher) {
        val elm = FakeElm327(mapOf("0100" to "SEARCHING...\rUNABLE TO CONNECT"))
        val h = harness(elm = elm)
        h.viewModel.onPermissionsGranted()
        idle()

        val state = h.viewModel.uiState.value
        assertEquals(ObdViewModel.ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertTrue(state.error!!.contains("ignition", ignoreCase = true))
        assertTrue(h.dao.records.value.isEmpty())
    }

    @Test
    fun `failed connection reports a type specific message`() = runTest(testDispatcher) {
        val h = harness(adapter = WIFI_ADAPTER)
        h.manager.connectResult = false
        h.viewModel.onPermissionsGranted()
        idle()

        val state = h.viewModel.uiState.value
        assertEquals(ObdViewModel.ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertTrue(state.error!!.contains("Wi-Fi"))
        assertEquals(WIFI_ADAPTER, h.manager.connectAttempts.single())
    }

    @Test
    fun `clearing codes logs the cleared codes and rescans`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        // After clearing, the car reports no codes.
        h.manager.elm.headerResponses["03"] = "7E8 02 43 00"
        h.manager.elm.headerResponses["07"] = "7E8 02 47 00"
        h.manager.elm.responses["0101"] = "41 01 00 07 65 65"
        h.viewModel.clearCodes()
        idle()

        assertTrue("04" in h.manager.elm.commands)
        val clearRecord = h.dao.records.value.single { it.taskName == ObdScanSummary.CLEAR_TASK_NAME }
        assertEquals("OBD2 trouble codes cleared: P0420, P0700, P0171.", clearRecord.description)
        assertFalse(h.viewModel.uiState.value.scanResult!!.hasAnyCodes)
        assertEquals(2, h.dao.records.value.count { it.taskName == ObdScanSummary.SCAN_TASK_NAME })
    }

    @Test
    fun `live data polls supported pids until stopped`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        h.viewModel.startLiveData()
        testDispatcher.scheduler.advanceTimeBy(2_000)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertTrue(state.isLiveDataActive)
        assertTrue(state.liveReadings.any { it.pid == ObdPid.RPM })
        assertTrue(state.liveReadings.any { it.pid == ObdPid.SPEED })
        assertTrue(h.manager.elm.commands.count { it == "010C" } > 2)

        h.viewModel.stopLiveData()
        val countAfterStop = h.manager.elm.commands.size
        idle()
        assertFalse(h.viewModel.uiState.value.isLiveDataActive)
        assertEquals(countAfterStop, h.manager.elm.commands.size)
    }

    @Test
    fun `live data reconnects once after a dropout`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        h.viewModel.startLiveData()
        testDispatcher.scheduler.advanceTimeBy(1_000)
        testDispatcher.scheduler.runCurrent()

        // Simulate the link dropping; the reconnect gets a fresh, working adapter.
        h.manager.onConnect = { h.manager.elm = FakeElm327.canCar() }
        h.manager.elm.broken = true
        testDispatcher.scheduler.advanceTimeBy(3_000)
        testDispatcher.scheduler.runCurrent()

        val state = h.viewModel.uiState.value
        assertEquals(2, h.manager.connectAttempts.size)
        assertTrue(state.isLiveDataActive)
        assertEquals(ObdViewModel.ConnectionStatus.CONNECTED, state.connectionStatus)
        assertTrue(h.manager.elm.commands.contains("010C"))

        h.viewModel.stopLiveData()
        idle()
    }

    @Test
    fun `live data gives up after a second dropout`() = runTest(testDispatcher) {
        val h = harness()
        h.viewModel.onPermissionsGranted()
        idle()

        h.viewModel.startLiveData()
        testDispatcher.scheduler.advanceTimeBy(1_000)
        testDispatcher.scheduler.runCurrent()

        h.manager.connectResult = false
        h.manager.elm.broken = true
        idle()

        val state = h.viewModel.uiState.value
        assertFalse(state.isLiveDataActive)
        assertEquals(ObdViewModel.ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertNotNull(state.error)
    }

    @Test
    fun `ble scan lists likely obd adapters first and deduplicates`() = runTest(testDispatcher) {
        val h = harness(adapter = null)
        h.manager.fakeBleDevices = h.manager.fakeBleDevices + h.manager.fakeBleDevices
        h.viewModel.startBleScan()
        idle()

        val state = h.viewModel.uiState.value
        assertFalse(state.isBleScanning)
        assertEquals(listOf("IOS-Vlink", "Heart Rate Strap"), state.bleDevices.map { it.name })
    }

    @Test
    fun `does not auto connect when no adapter is saved`() = runTest(testDispatcher) {
        val h = harness(adapter = null)
        h.viewModel.onPermissionsGranted()
        idle()

        assertTrue(h.manager.connectAttempts.isEmpty())
        assertTrue(h.viewModel.uiState.value.isPreferenceLoaded)
        assertFalse(h.viewModel.uiState.value.hasSavedDevice)
        assertEquals(h.manager.fakePairedDevices, h.viewModel.uiState.value.devices)
    }

    @Test
    fun `selecting an adapter persists it and connects`() = runTest(testDispatcher) {
        val h = harness(adapter = null)

        h.viewModel.openDevicePicker()
        assertTrue(h.viewModel.uiState.value.isDevicePickerOpen)

        val ble = ObdDevice("IOS-Vlink", "22:22:22:22:22:22", ObdAdapterType.BLE).toConfig()
        h.viewModel.selectAdapter(ble)
        idle()

        assertFalse(h.viewModel.uiState.value.isDevicePickerOpen)
        assertEquals(ble, h.prefs.adapterFlow.value)
        assertEquals(listOf(ble), h.manager.connectAttempts)
    }

    @Test
    fun `clearing saved device forgets it`() = runTest(testDispatcher) {
        val h = harness()
        idle()

        h.viewModel.clearSavedDevice()
        idle()

        assertNull(h.prefs.adapterFlow.value)
        assertNull(h.viewModel.uiState.value.savedAdapter)
        assertEquals(ObdViewModel.ConnectionStatus.IDLE, h.viewModel.uiState.value.connectionStatus)
    }

    private companion object {
        const val VEHICLE_ID = 7L
        const val CAR_VIN = "1HGCM82633A004352"
        val CLASSIC_ADAPTER = ObdAdapterConfig(ObdAdapterType.CLASSIC, "AA:BB:CC:DD:EE:FF", "Veepeak")
        val WIFI_ADAPTER = ObdAdapterConfig.wifi("192.168.0.10", 35000)
    }
}
