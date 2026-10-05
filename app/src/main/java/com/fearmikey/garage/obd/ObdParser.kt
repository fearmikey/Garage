package com.fearmikey.garage.obd

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream

/**
 * A single ELM327 conversation over an already-open connection.
 *
 * All I/O is non-blocking (polls [InputStream.available]) so every command honours its
 * timeout and coroutine cancellation, even if the adapter or vehicle stops responding.
 * Commands are serialized with a [Mutex] so a live-data loop and a scan can never interleave
 * on the wire.
 */
class ObdParser(
    private val input: InputStream,
    private val output: OutputStream,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    /** PIDs the vehicle reported as supported; empty if it never answered the bitmap query. */
    var supportedPids: Set<Int> = emptySet()
        private set

    var protocolName: String? = null
        private set

    /** ELM327 protocol number (`AT DPN`), e.g. '6' for 11-bit 500k CAN. */
    var protocolNumber: Char? = null
        private set

    /** CAN protocols let us turn on headers to tell which module reported a code. */
    val isCan: Boolean get() = protocolNumber in '6'..'9'
    private val canHeaderLength: Int get() = if (protocolNumber == '7' || protocolNumber == '9') 8 else 3

    /**
     * Resets and configures the adapter, lets it auto-detect the vehicle protocol, and reads
     * the supported-PID bitmaps.
     *
     * @throws ObdAdapterException if the vehicle can't be reached.
     * @throws ObdTimeoutException if the adapter doesn't respond.
     */
    suspend fun init() {
        sendRaw("ATZ", RESET_TIMEOUT_MS)
        delay(RESET_SETTLE_MS)
        for (command in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATSP0")) {
            sendRaw(command, DEFAULT_TIMEOUT_MS)
        }

        // The first real request triggers the protocol search, which can take several seconds.
        val supported = ObdResponseParser.parseSupportedPids(query("0100", PROTOCOL_SEARCH_TIMEOUT_MS), 0x00).toMutableSet()
        var base = 0x20
        while (base <= 0xC0 && base in supported) {
            val next = runCatching { ObdResponseParser.parseSupportedPids(query("01%02X".format(base)), base) }
                .getOrElse { if (it is ObdTimeoutException) emptySet() else throw it }
            if (next.isEmpty()) break
            supported += next
            base += 0x20
        }
        supportedPids = supported

        protocolNumber = runCatching {
            ObdResponseParser.parseProtocolNumber(ObdResponseParser.cleanLines(sendRaw("ATDPN")))
        }.getOrNull()
        protocolName = ObdResponseParser.protocolName(protocolNumber)
    }

    /** Whether it's worth asking for [pid]. Unknown support (empty bitmap) means "try it". */
    fun isSupported(pid: Int): Boolean = supportedPids.isEmpty() || pid in supportedPids

    fun isSupported(pid: ObdPid): Boolean =
        if (supportedPids.isEmpty()) pid in ObdPid.FALLBACK else pid.pid in supportedPids

    suspend fun readPid(pid: ObdPid): ObdReading? {
        if (!isSupported(pid)) return null
        val data = ObdResponseParser.pidData(query("01%02X".format(pid.pid)), pid.pid, pid.bytes) ?: return null
        return ObdReading(pid, pid.decode(data))
    }

    suspend fun readMonitorStatus(): MonitorStatus? {
        if (!isSupported(0x01)) return null
        return ObdResponseParser.parseMonitorStatus(query("0101"))
    }

    suspend fun readOdometerKm(): Int? {
        if (!isSupported(0xA6)) return null
        return ObdResponseParser.parseOdometerKm(query("01A6"))
    }

    suspend fun readDistanceSinceClearedKm(): Int? {
        if (!isSupported(0x31)) return null
        val data = ObdResponseParser.pidData(query("0131"), 0x31, 2) ?: return null
        return data[0] * 256 + data[1]
    }

    /** Stored (confirmed) codes, Mode 03. */
    suspend fun readStoredDtcs(): DtcReadResult = readDtcs("03", "43")

    /** Pending codes, Mode 07: faults seen once but not yet confirmed. */
    suspend fun readPendingDtcs(): DtcReadResult = readDtcs("07", "47")

    /** Permanent codes, Mode 0A (2010+): can't be cleared by a scan tool. */
    suspend fun readPermanentDtcs(): DtcReadResult = readDtcs("0A", "4A")

    /**
     * On CAN vehicles, headers are switched on for the request so replies from several modules
     * can be separated reliably and labelled (ECM, TCM, ...). Other protocols use the plain
     * header-less format.
     */
    private suspend fun readDtcs(command: String, responseMode: String): DtcReadResult {
        if (!isCan) return DtcReadResult(ObdResponseParser.parseDtcs(query(command, DTC_TIMEOUT_MS), responseMode))
        sendRaw("ATH1")
        try {
            val lines = ObdResponseParser.cleanLines(sendRaw(command, DTC_TIMEOUT_MS), command)
            return ObdResponseParser.parseDtcsWithSources(
                ObdResponseParser.assembleCanMessages(lines, canHeaderLength),
                responseMode,
            )
        } finally {
            withContext(NonCancellable) { runCatching { sendRaw("ATH0") } }
        }
    }

    /**
     * Reads freeze frame 0 (Mode 02): the trigger code and engine conditions when it was set.
     * Returns null when the vehicle has no stored freeze frame.
     */
    suspend fun readFreezeFrame(): FreezeFrame? {
        val dtc = ObdResponseParser.parseFreezeFrameDtc(query("020200"))
        val readings = mutableListOf<ObdReading>()
        for (pid in ObdPid.FREEZE_FRAME) {
            if (supportedPids.isNotEmpty() && pid.pid !in supportedPids) continue
            val data = try {
                ObdResponseParser.freezeFrameData(query("02%02X00".format(pid.pid)), pid.pid, pid.bytes)
            } catch (_: ObdTimeoutException) {
                null
            } ?: continue
            readings += ObdReading(pid, pid.decode(data))
        }
        return if (dtc == null && readings.isEmpty()) null else FreezeFrame(dtc, readings)
    }

    /** Mode 04. Returns true if at least one module acknowledged the clear. */
    suspend fun clearDtcs(): Boolean = ObdResponseParser.isClearAcknowledged(query("04", CLEAR_TIMEOUT_MS))

    suspend fun readVin(): String? = ObdResponseParser.parseVin(query("0902", VIN_TIMEOUT_MS))

    /** Battery voltage measured by the adapter itself at the OBD2 port (`AT RV`). */
    suspend fun readAdapterVoltage(): Double? =
        ObdResponseParser.parseAdapterVoltage(ObdResponseParser.cleanLines(sendRaw("ATRV"), "ATRV"))

    suspend fun query(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): List<String> =
        ObdResponseParser.messages(sendRaw(command, timeoutMs), command)

    /**
     * Sends [command] and returns everything up to the `>` prompt.
     *
     * @throws ObdTimeoutException if no prompt arrives within [timeoutMs].
     */
    suspend fun sendRaw(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String = mutex.withLock {
        withContext(ioDispatcher) {
            drainStaleInput()
            output.write("$command\r".toByteArray(Charsets.US_ASCII))
            output.flush()

            val builder = StringBuilder()
            var promptSeen = false
            withTimeoutOrNull(timeoutMs) {
                val buffer = ByteArray(256)
                while (!promptSeen) {
                    val available = input.available()
                    if (available <= 0) {
                        delay(POLL_INTERVAL_MS)
                        continue
                    }
                    val read = input.read(buffer, 0, minOf(buffer.size, available))
                    if (read < 0) throw java.io.IOException("Connection closed")
                    for (i in 0 until read) {
                        val c = buffer[i].toInt().toChar()
                        if (c == '>') {
                            promptSeen = true
                            break
                        }
                        if (c != '\u0000') builder.append(c)
                    }
                }
            }
            if (!promptSeen) throw ObdTimeoutException(command)
            builder.toString()
        }
    }

    /** Discards bytes left over from a previous timed-out command so replies stay aligned. */
    private fun drainStaleInput() {
        var available = input.available()
        while (available > 0) {
            input.skip(available.toLong())
            available = input.available()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 2_500L
        const val DTC_TIMEOUT_MS = 4_000L
        const val CLEAR_TIMEOUT_MS = 5_000L
        const val VIN_TIMEOUT_MS = 5_000L
        const val RESET_TIMEOUT_MS = 4_000L
        const val PROTOCOL_SEARCH_TIMEOUT_MS = 15_000L
        private const val RESET_SETTLE_MS = 300L
        private const val POLL_INTERVAL_MS = 10L
    }
}
