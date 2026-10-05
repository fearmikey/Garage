package com.fearmikey.garage.obd

import com.fearmikey.garage.ui.util.UnitSystem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure decoding tests using captured/representative ELM327 output. */
class ObdResponseParserTest {

    private fun msgs(raw: String, command: String? = null) = ObdResponseParser.messages(raw, command)

    @Test
    fun `strips SEARCHING prefix so the first reply isn't lost`() {
        val data = ObdResponseParser.pidData(msgs("SEARCHING...\r41 05 7B\r"), 0x05)
        assertEquals(0x7B, data?.get(0))
    }

    @Test
    fun `strips echo and bus init chatter`() {
        val messages = msgs("0105\rBUS INIT: ...OK\r41 05 7B", "0105")
        assertEquals(listOf("41057B"), messages)
    }

    @Test
    fun `no data style replies produce no messages`() {
        assertTrue(msgs("NO DATA").isEmpty())
        assertTrue(msgs("?").isEmpty())
        assertTrue(msgs("STOPPED").isEmpty())
    }

    @Test(expected = ObdAdapterException::class)
    fun `unable to connect is reported as an adapter error`() {
        msgs("SEARCHING...\rUNABLE TO CONNECT")
    }

    @Test(expected = ObdAdapterException::class)
    fun `bus init error is reported as an adapter error`() {
        msgs("BUS INIT: ...ERROR")
    }

    @Test(expected = ObdAdapterException::class)
    fun `can error is reported as an adapter error`() {
        msgs("CAN ERROR")
    }

    // region DTCs

    @Test
    fun `legacy dtc reply with padding`() {
        assertEquals(listOf("P0133"), ObdResponseParser.parseDtcs("43 01 33 00 00 00 00"))
    }

    @Test
    fun `legacy dtc reply with no codes`() {
        assertEquals(emptyList<String>(), ObdResponseParser.parseDtcs("43 00 00 00 00 00 00"))
    }

    @Test
    fun `legacy multi line reply from several ecus is not merged into garbage`() {
        val raw = "43 01 33 41 33 00 00\r43 03 00 00 00 00 00"
        assertEquals(listOf("P0133", "C0133", "P0300"), ObdResponseParser.parseDtcs(raw))
    }

    @Test
    fun `can dtc reply honours the count byte`() {
        // Previously the count byte (02) was decoded as part of the first code.
        assertEquals(listOf("P0133", "P0134"), ObdResponseParser.parseDtcs("43 02 01 33 01 34"))
    }

    @Test
    fun `can dtc reply with zero codes`() {
        assertEquals(emptyList<String>(), ObdResponseParser.parseDtcs("43 00"))
    }

    @Test
    fun `can multi frame dtc reply`() {
        val raw = "00A\r0: 43 04 01 33 01 34\r1: 03 00 C1 23 00 00 00"
        assertEquals(listOf("P0133", "P0134", "P0300", "U0123"), ObdResponseParser.parseDtcs(raw))
    }

    @Test
    fun `can replies from two ecus are both read and deduplicated`() {
        val raw = "43 01 04 20\r43 02 04 20 07 00"
        assertEquals(listOf("P0420", "P0700"), ObdResponseParser.parseDtcs(raw))
    }

    @Test
    fun `pending and permanent modes use their own response byte`() {
        assertEquals(listOf("P0171"), ObdResponseParser.parseDtcs(msgs("47 01 01 71"), "47"))
        assertEquals(listOf("P0420"), ObdResponseParser.parseDtcs(msgs("4A 01 04 20"), "4A"))
        assertEquals(emptyList<String>(), ObdResponseParser.parseDtcs(msgs("47 01 01 71"), "43"))
    }

    @Test
    fun `decodes all system letters`() {
        assertEquals("P0123", ObdResponseParser.decodeDtc("0123"))
        assertEquals("C0123", ObdResponseParser.decodeDtc("4123"))
        assertEquals("B0123", ObdResponseParser.decodeDtc("8123"))
        assertEquals("U0123", ObdResponseParser.decodeDtc("C123"))
        assertEquals("P2A1F", ObdResponseParser.decodeDtc("2A1F"))
    }

    @Test
    fun `clear acknowledgement`() {
        assertTrue(ObdResponseParser.isClearAcknowledged(msgs("44")))
        assertFalse(ObdResponseParser.isClearAcknowledged(msgs("NO DATA")))
    }

    // endregion

    // region Mode 01

    @Test
    fun `supported pid bitmap`() {
        val supported = ObdResponseParser.parseSupportedPids(msgs("41 00 BE 1F A8 13"), 0x00)
        assertTrue(0x01 in supported)
        assertTrue(0x05 in supported)
        assertTrue(0x0C in supported)
        assertTrue(0x20 in supported)
        assertFalse(0x02 in supported)
    }

    @Test
    fun `supported pid bitmaps from multiple ecus are merged`() {
        val supported = ObdResponseParser.parseSupportedPids(msgs("41 00 80 00 00 00\r41 00 40 00 00 00"), 0x00)
        assertEquals(setOf(0x01, 0x02), supported)
    }

    @Test
    fun `monitor status decodes mil and readiness`() {
        val status = ObdResponseParser.parseMonitorStatus(msgs("41 01 81 07 65 04"))!!
        assertTrue(status.milOn)
        assertEquals(1, status.storedDtcCount)
        assertFalse(status.isDiesel)
        val byName = status.monitors.associate { it.name to it.complete }
        assertEquals(true, byName["Misfire"])
        assertEquals(true, byName["Catalyst"])
        assertEquals(false, byName["Evaporative System"])
        assertEquals(true, byName["Oxygen Sensor"])
        assertNull(byName["EGR/VVT System"])
        assertEquals(1, status.incompleteCount)
    }

    @Test
    fun `monitor status with mil off and no codes`() {
        val status = ObdResponseParser.parseMonitorStatus(msgs("41 01 00 07 00 00"))!!
        assertFalse(status.milOn)
        assertEquals(0, status.storedDtcCount)
        assertEquals(3, status.monitors.size)
    }

    @Test
    fun `odometer`() {
        assertEquals(1000, ObdResponseParser.parseOdometerKm(msgs("41 A6 00 00 27 10")))
        assertNull(ObdResponseParser.parseOdometerKm(msgs("NO DATA")))
    }

    @Test
    fun `pid data ignores replies for other pids`() {
        assertNull(ObdResponseParser.pidData(msgs("41 0D 32"), 0x05))
    }

    @Test
    fun `pid decoding`() {
        fun decode(pid: ObdPid, raw: String) =
            pid.decode(ObdResponseParser.pidData(msgs(raw), pid.pid, pid.bytes)!!)

        assertEquals(83.0, decode(ObdPid.COOLANT_TEMP, "41 05 7B"), 0.001)
        assertEquals(775.0, decode(ObdPid.RPM, "41 0C 0C 1C"), 0.001)
        assertEquals(14.0, decode(ObdPid.CONTROL_MODULE_VOLTAGE, "41 42 36 B0"), 0.001)
        assertEquals(50.2, decode(ObdPid.FUEL_LEVEL, "41 2F 80"), 0.1)
    }

    // endregion

    // region VIN

    @Test
    fun `can multi frame vin`() {
        val raw = "014\r0: 49 02 01 31 48 47\r1: 43 4D 38 32 36 33 33\r2: 41 30 30 34 33 35 32"
        assertEquals("1HGCM82633A004352", ObdResponseParser.parseVin(msgs(raw)))
    }

    @Test
    fun `legacy five line vin`() {
        val raw = "49 02 01 00 00 00 31\r" +
            "49 02 02 48 47 43 4D\r" +
            "49 02 03 38 32 36 33\r" +
            "49 02 04 33 41 30 30\r" +
            "49 02 05 34 33 35 32"
        assertEquals("1HGCM82633A004352", ObdResponseParser.parseVin(msgs(raw)))
    }

    @Test
    fun `legacy vin lines out of order`() {
        val raw = "49 02 02 48 47 43 4D\r" +
            "49 02 01 00 00 00 31\r" +
            "49 02 04 33 41 30 30\r" +
            "49 02 03 38 32 36 33\r" +
            "49 02 05 34 33 35 32"
        assertEquals("1HGCM82633A004352", ObdResponseParser.parseVin(msgs(raw)))
    }

    @Test
    fun `incomplete vin is rejected`() {
        assertNull(ObdResponseParser.parseVin(msgs("49 02 01 31 48 47")))
    }

    // endregion

    // region Headered CAN

    @Test
    fun `headered single frames from two modules`() {
        val lines = ObdResponseParser.cleanLines("7E8 04 43 01 04 20 00 00\r7E9 04 43 01 07 00")
        val messages = ObdResponseParser.assembleCanMessages(lines, 3)
        assertEquals(listOf(CanMessage("7E8", "43010420"), CanMessage("7E9", "43010700")), messages)
    }

    @Test
    fun `headered multi frame reply is reassembled per module and padding trimmed`() {
        val raw = "7E8 10 14 49 02 01 31 48 47\r" +
            "7E8 21 43 4D 38 32 36 33 33\r" +
            "7E8 22 41 30 30 34 33 35 32"
        val messages = ObdResponseParser.assembleCanMessages(ObdResponseParser.cleanLines(raw), 3)
        assertEquals(1, messages.size)
        assertEquals("1HGCM82633A004352", ObdResponseParser.parseVin(messages.map { it.payload }))
    }

    @Test
    fun `interleaved multi frame replies from two modules stay separate`() {
        val raw = "7E8 10 0A 43 04 01 33 01 34\r" +
            "7E9 04 43 01 07 00\r" +
            "7E8 21 03 00 C1 23 00 00 00"
        val result = ObdResponseParser.parseDtcsWithSources(
            ObdResponseParser.assembleCanMessages(ObdResponseParser.cleanLines(raw), 3),
            "43",
        )
        assertEquals(listOf("P0700", "P0133", "P0134", "P0300", "U0123"), result.codes)
        assertEquals(listOf("Engine (ECM)"), result.sources["P0133"])
        assertEquals(listOf("Transmission (TCM)"), result.sources["P0700"])
    }

    @Test
    fun `29 bit headers`() {
        val lines = ObdResponseParser.cleanLines("18 DA F1 10 04 43 01 04 20\r18 DA F1 28 04 43 01 41 23")
        val result = ObdResponseParser.parseDtcsWithSources(ObdResponseParser.assembleCanMessages(lines, 8), "43")
        assertEquals(listOf("P0420", "C0123"), result.codes)
        assertEquals(listOf("Engine (ECM)"), result.sources["P0420"])
        assertEquals(listOf("Module 28"), result.sources["C0123"])
    }

    @Test
    fun `module names`() {
        assertEquals("Engine (ECM)", ObdResponseParser.moduleName("7E8"))
        assertEquals("Transmission (TCM)", ObdResponseParser.moduleName("7E9"))
        assertEquals("Module 3", ObdResponseParser.moduleName("7EA"))
        assertEquals("Transmission (TCM)", ObdResponseParser.moduleName("18DAF118"))
    }

    // endregion

    // region Freeze frame

    @Test
    fun `freeze frame dtc and data`() {
        assertEquals("P0420", ObdResponseParser.parseFreezeFrameDtc(msgs("42 02 00 04 20")))
        assertNull(ObdResponseParser.parseFreezeFrameDtc(msgs("42 02 00 00 00")))
        assertNull(ObdResponseParser.parseFreezeFrameDtc(msgs("NO DATA")))
        val data = ObdResponseParser.freezeFrameData(msgs("42 0C 00 26 48"), 0x0C, 2)!!
        assertEquals(2450.0, ObdPid.RPM.decode(data), 0.001)
    }

    // endregion

    @Test
    fun `adapter voltage and protocol`() {
        assertEquals(12.6, ObdResponseParser.parseAdapterVoltage(ObdResponseParser.cleanLines("12.6V"))!!, 0.001)
        assertEquals('6', ObdResponseParser.parseProtocolNumber(ObdResponseParser.cleanLines("A6")))
        assertEquals('3', ObdResponseParser.parseProtocolNumber(ObdResponseParser.cleanLines("3")))
    }
}

class DtcDescriptionsTest {
    @Test
    fun `known code has exact description`() {
        val description = DtcDescriptions.describe("p0420")
        assertTrue(description.isExact)
        assertEquals("Catalyst system efficiency below threshold (Bank 1)", description.title)
    }

    @Test
    fun `bundled list loads and every entry is well formed`() {
        assertTrue(DtcDescriptions.exactCount > 300)
        val raw = javaClass.getResourceAsStream("/obd/dtc_generic.tsv")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        val codes = raw.map { it.substringBefore('\t') }
        assertEquals("Duplicate codes in dtc_generic.tsv", codes.size, codes.toSet().size)
        val pattern = Regex("^[PBCU][0-3][0-9A-F]{3}$")
        codes.forEach { assertTrue("Malformed code '$it'", pattern.matches(it)) }
        raw.forEach { assertTrue("Missing tab in '$it'", it.contains('\t')) }
    }

    @Test
    fun `parse ignores comments and malformed lines`() {
        val parsed = DtcDescriptions.parse("# comment\nP0420\tCatalyst\nbad line\n\nu0100\tLost comms\n".reader())
        assertEquals(mapOf("P0420" to "Catalyst", "U0100" to "Lost comms"), parsed)
    }

    @Test
    fun `unknown codes fall back to subsystem`() {
        assertEquals("Generic ignition system or misfire fault", DtcDescriptions.describe("P0399").title)
        assertEquals("Manufacturer-specific fuel and air metering fault", DtcDescriptions.describe("P1234").title)
        assertEquals("Generic network / module communication fault", DtcDescriptions.describe("U0999").title)
        assertFalse(DtcDescriptions.describe("B1000").isExact)
    }
}

class ObdFormatterTest {
    @Test
    fun `temperature respects unit system`() {
        assertEquals("90°C", ObdFormatter.temperature(90.0, UnitSystem.METRIC))
        assertEquals("194°F", ObdFormatter.temperature(90.0, UnitSystem.IMPERIAL))
    }

    @Test
    fun `speed respects unit system`() {
        assertEquals("100 km/h", ObdFormatter.format(ObdUnit.SPEED_KMH, 100.0, UnitSystem.METRIC))
        assertEquals("62 mph", ObdFormatter.format(ObdUnit.SPEED_KMH, 100.0, UnitSystem.IMPERIAL))
    }
}

class ObdScanSummaryTest {
    @Test
    fun `summary lists codes with descriptions and readiness`() {
        val result = ObdScanResult(
            storedDtcs = listOf("P0420"),
            monitorStatus = MonitorStatus(
                milOn = true,
                storedDtcCount = 1,
                isDiesel = false,
                monitors = listOf(ReadinessMonitor("Catalyst", true), ReadinessMonitor("Evaporative System", false)),
            ),
        )
        assertEquals(
            "OBD2 scan — check engine light ON. Stored: P0420 (Catalyst system efficiency below threshold (Bank 1)). " +
                "Readiness: 1/2 monitors complete (not ready: Evaporative System).",
            ObdScanSummary.describe(result),
        )
    }

    @Test
    fun `summary for clean scan`() {
        assertEquals("OBD2 scan. No trouble codes.", ObdScanSummary.describe(ObdScanResult()))
    }

    @Test
    fun `readiness round trips through the saved description`() {
        val result = ObdScanResult(
            monitorStatus = MonitorStatus(
                milOn = false,
                storedDtcCount = 0,
                isDiesel = false,
                monitors = listOf(
                    ReadinessMonitor("Catalyst", true),
                    ReadinessMonitor("Evaporative System", false),
                    ReadinessMonitor("Oxygen Sensor", false),
                ),
            ),
        )
        val saved = ObdScanSummary.parseReadiness(ObdScanSummary.describe(result))!!
        assertEquals(false, saved.milOn)
        assertEquals(1, saved.complete)
        assertEquals(3, saved.total)
        assertEquals(listOf("Evaporative System", "Oxygen Sensor"), saved.notReady)
        assertFalse(saved.likelyReady)
    }

    @Test
    fun `readiness missing from description`() {
        assertNull(ObdScanSummary.parseReadiness("OBD2 scan. No trouble codes."))
        assertTrue(ObdScanSummary.parseReadiness("OBD2 scan. Readiness: 5/5 monitors complete.")!!.likelyReady)
    }
}

class ObdAdapterConfigTest {
    @Test
    fun `wifi host and port`() {
        assertEquals("192.168.0.10" to 35000, ObdAdapterConfig.wifi("192.168.0.10", 35000).wifiHostPort)
        assertEquals(
            "10.0.0.5" to 35000,
            ObdAdapterConfig(ObdAdapterType.WIFI, "10.0.0.5", "x").wifiHostPort,
        )
        assertNull(ObdAdapterConfig(ObdAdapterType.CLASSIC, "AA:BB", "x").wifiHostPort)
    }

    @Test
    fun `unknown type defaults to classic`() {
        assertEquals(ObdAdapterType.CLASSIC, ObdAdapterType.fromName(null))
        assertEquals(ObdAdapterType.BLE, ObdAdapterType.fromName("BLE"))
    }
}

/** Session-level tests running against the in-memory ELM327 emulator. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObdParserSessionTest {

    @Test
    fun `init reads supported pids and protocol`() = runTest {
        val elm = FakeElm327.canCar()
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()

        assertEquals("ISO 15765-4 CAN (11 bit, 500 kbaud)", parser.protocolName)
        assertTrue(parser.isSupported(0x05))
        assertTrue(parser.isSupported(0x42))
        assertFalse(parser.isSupported(0xA6))
        assertTrue("0140" in elm.commands)
        assertFalse("0160" in elm.commands)
    }

    @Test
    fun `unsupported pids are never requested`() = runTest {
        val elm = FakeElm327.canCar()
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()

        assertNull(parser.readOdometerKm())
        assertFalse("01A6" in elm.commands)
    }

    @Test
    fun `reads codes with module sources on can and restores headers`() = runTest {
        val elm = FakeElm327.canCar()
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()
        assertTrue(parser.isCan)

        val stored = parser.readStoredDtcs()
        assertEquals(listOf("P0420", "P0700"), stored.codes)
        assertEquals(listOf("Engine (ECM)"), stored.sources["P0420"])
        assertEquals(listOf("Transmission (TCM)"), stored.sources["P0700"])
        assertFalse(elm.headersOn)
    }

    @Test
    fun `non can protocols read codes without headers`() = runTest {
        val elm = FakeElm327.canCar().apply { responses["ATDPN"] = "A3" }
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()
        assertFalse(parser.isCan)

        assertEquals(listOf("P0420"), parser.readStoredDtcs().codes)
        assertFalse("ATH1" in elm.commands)
    }

    @Test
    fun `reads freeze frame`() = runTest {
        val elm = FakeElm327.canCar()
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()

        val frame = parser.readFreezeFrame()!!
        assertEquals("P0420", frame.dtc)
        val byPid = frame.readings.associate { it.pid to it.value }
        assertEquals(2450.0, byPid[ObdPid.RPM]!!, 0.001)
        assertEquals(88.0, byPid[ObdPid.SPEED]!!, 0.001)
        assertEquals(91.0, byPid[ObdPid.COOLANT_TEMP]!!, 0.001)
        assertEquals(7.8, byPid[ObdPid.LONG_FUEL_TRIM_1]!!, 0.1)
    }

    @Test
    fun `no freeze frame returns null`() = runTest {
        val elm = FakeElm327(mapOf("ATDPN" to "A6"))
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        assertNull(parser.readFreezeFrame())
    }

    @Test
    fun `reads codes vin and readings`() = runTest {
        val elm = FakeElm327.canCar()
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()

        assertEquals(listOf("P0420", "P0700"), parser.readStoredDtcs().codes)
        assertEquals(listOf("P0171"), parser.readPendingDtcs().codes)
        assertEquals(emptyList<String>(), parser.readPermanentDtcs().codes)
        assertEquals("1HGCM82633A004352", parser.readVin())
        assertEquals(500, parser.readDistanceSinceClearedKm())
        assertEquals(12.6, parser.readAdapterVoltage()!!, 0.001)
        assertEquals(83.0, parser.readPid(ObdPid.COOLANT_TEMP)!!.value, 0.001)
        assertTrue(parser.clearDtcs())
    }

    @Test(expected = ObdTimeoutException::class)
    fun `stalled adapter times out instead of hanging`() = runTest {
        val elm = FakeElm327(silentCommands = setOf("0105"))
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.query("0105")
    }

    @Test
    fun `stale bytes from a timed out command are discarded`() = runTest {
        val elm = FakeElm327(mapOf("0105" to "41 05 7B"))
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        elm.injectStale("41 0C 0C 1C\r>")
        assertEquals(83.0, parser.readPid(ObdPid.COOLANT_TEMP)!!.value, 0.001)
    }

    @Test(expected = ObdAdapterException::class)
    fun `ignition off surfaces as adapter error during init`() = runTest {
        val elm = FakeElm327(mapOf("0100" to "SEARCHING...\rUNABLE TO CONNECT"))
        val parser = ObdParser(elm.input, elm.output, StandardTestDispatcher(testScheduler))
        parser.init()
    }
}
