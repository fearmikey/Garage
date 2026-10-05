package com.fearmikey.garage.obd

/**
 * Thrown when the adapter reports a condition that means it cannot talk to the vehicle
 * (e.g. `UNABLE TO CONNECT`, `CAN ERROR`, `BUS INIT: ...ERROR`).
 */
class ObdAdapterException(message: String) : Exception(message)

/** Thrown when the adapter doesn't finish a reply (`>` prompt) within the allotted time. */
class ObdTimeoutException(command: String) : java.io.IOException("Adapter did not respond to '$command'")

/** Decoded Mode 01 PID 01: check engine light state and emissions readiness monitors. */
data class MonitorStatus(
    val milOn: Boolean,
    val storedDtcCount: Int,
    val isDiesel: Boolean,
    val monitors: List<ReadinessMonitor>,
) {
    val completeCount: Int get() = monitors.count { it.complete }
    val incompleteCount: Int get() = monitors.count { !it.complete }
}

data class ReadinessMonitor(val name: String, val complete: Boolean)

/** One logical CAN message, reassembled from headered frames. */
data class CanMessage(val header: String, val payload: String)

/** Trouble codes plus, when the adapter reported headers, which module(s) set each code. */
data class DtcReadResult(
    val codes: List<String>,
    val sources: Map<String, List<String>> = emptyMap(),
)

/** Engine conditions captured by the ECU at the moment [dtc] was set (Mode 02, frame 0). */
data class FreezeFrame(val dtc: String?, val readings: List<ObdReading>)

/**
 * Pure (I/O-free) decoding of ELM327 replies. Everything here is deterministic so it can be
 * unit tested with captured adapter output.
 *
 * Assumes the adapter is configured with headers off (`AT H0`), which is the ELM327 default.
 */
object ObdResponseParser {

    private val FRAME_LINE = Regex("^([0-9A-F]):([0-9A-F]*)$")
    private val HEX_ONLY = Regex("^[0-9A-F]+$")
    private val BYTE_COUNT_LINE = Regex("^[0-9A-F]{3}$")
    private val BUS_INIT = Regex("BUSINIT:?\\.*(OK)?")

    private val FATAL_MARKERS = listOf(
        "UNABLETOCONNECT" to "The adapter couldn't communicate with the vehicle. Is the ignition on?",
        "CANERROR" to "CAN bus error. Check that the adapter is fully seated in the OBD2 port.",
        "BUSERROR" to "Vehicle bus error. Check that the adapter is fully seated in the OBD2 port.",
        "FBERROR" to "Vehicle bus feedback error. Check the adapter connection.",
        "LVRESET" to "The adapter reset due to low voltage. Check the vehicle battery.",
        "ERROR" to "The adapter reported a bus initialization error. Is the ignition on?",
    )

    /** Replies that just mean "no answer for this request" and are not a connection problem. */
    private val NO_DATA_MARKERS = listOf("NODATA", "?", "STOPPED", "BUFFERFULL", "DATAERROR", "<DATAERROR", "NORESPONSE")

    /**
     * Splits a raw reply into normalized lines: uppercase, no spaces, no echo, no
     * `SEARCHING...`/`BUS INIT` chatter. Returns an empty list for "no data" style replies.
     *
     * @throws ObdAdapterException for replies that mean the vehicle can't be reached.
     */
    fun cleanLines(raw: String, command: String? = null): List<String> {
        val echo = command?.replace(" ", "")?.uppercase()
        val lines = raw.uppercase()
            .split('\r', '\n')
            .map { it.replace(" ", "").replace("\t", "") }
            .map { it.replace("SEARCHING...", "").replace(BUS_INIT, "") }
            .filter { it.isNotEmpty() && it != echo }

        for (line in lines) {
            if (line.startsWith("ERR") && line.drop(3).all { it.isDigit() } && line.length > 3) {
                throw ObdAdapterException("The adapter reported internal error $line.")
            }
            FATAL_MARKERS.firstOrNull { (marker, _) -> line.contains(marker) && !line.contains("DATAERROR") }
                ?.let { (_, message) -> throw ObdAdapterException(message) }
        }
        return lines.filterNot { line -> NO_DATA_MARKERS.any { line.contains(it) } }
    }

    /**
     * Reassembles logical messages from cleaned lines. Handles ISO 15765 (CAN) multi-frame
     * replies (`014` / `0:...` / `1:...`) by stitching frames and trimming padding using the
     * byte count line, and treats every other hex line as its own message (one per ECU on
     * older protocols, or one per ECU for single-frame CAN replies).
     */
    fun assembleMessages(lines: List<String>): List<String> {
        val messages = mutableListOf<String>()
        var current: StringBuilder? = null
        var expectedChars: Int? = null

        fun flush() {
            current?.let { builder ->
                val text = builder.toString()
                messages += expectedChars?.let { text.take(it) } ?: text
            }
            current = null
            expectedChars = null
        }

        for (line in lines) {
            val frame = FRAME_LINE.matchEntire(line)
            when {
                frame != null -> {
                    val index = frame.groupValues[1].toInt(16)
                    if (index == 0 && current != null && current!!.isNotEmpty()) {
                        val pendingExpected = expectedChars
                        flush()
                        expectedChars = pendingExpected
                    }
                    val builder = current ?: StringBuilder().also { current = it }
                    builder.append(frame.groupValues[2])
                }
                BYTE_COUNT_LINE.matches(line) -> {
                    flush()
                    expectedChars = line.toInt(16) * 2
                    current = StringBuilder()
                }
                HEX_ONLY.matches(line) && line.length % 2 == 0 -> {
                    flush()
                    messages += line
                }
                else -> Unit // Non-hex chatter (adapter banners, "OK", etc.)
            }
        }
        flush()
        return messages.filter { it.isNotEmpty() }
    }

    fun messages(raw: String, command: String? = null): List<String> =
        assembleMessages(cleanLines(raw, command))

    // region DTCs

    /**
     * Decodes trouble codes from a Mode 03 / 07 / 0A reply.
     *
     * ISO 15765 (CAN) replies carry a code-count byte after the mode byte; legacy protocols
     * don't, and always send codes in groups of three (6 bytes, zero padded). Since each code
     * is exactly 2 bytes, an odd payload length reliably identifies the CAN layout.
     *
     * @param responseMode the positive response mode byte, e.g. "43" for Mode 03.
     */
    fun parseDtcs(messages: List<String>, responseMode: String = "43"): List<String> {
        val codes = LinkedHashSet<String>()
        for (message in messages) {
            if (!message.startsWith(responseMode)) continue
            var data = message.substring(responseMode.length)
            if ((data.length / 2) % 2 == 1) {
                val count = data.take(2).toIntOrNull(16) ?: 0
                data = data.drop(2).take(count * 4)
            }
            data.chunked(4)
                .filter { it.length == 4 && it != "0000" }
                .mapNotNullTo(codes) { decodeDtc(it) }
        }
        return codes.toList()
    }

    /** Convenience overload for a single raw reply. */
    fun parseDtcs(raw: String, responseMode: String = "43"): List<String> =
        parseDtcs(messages(raw), responseMode)

    internal fun decodeDtc(hex: String): String? {
        val b1 = hex.substring(0, 2).toIntOrNull(16) ?: return null
        val b2 = hex.substring(2, 4).toIntOrNull(16) ?: return null
        val system = when ((b1 shr 6) and 0x03) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            else -> 'U'
        }
        val digit2 = (b1 shr 4) and 0x03
        val digit3 = b1 and 0x0F
        val digit4 = (b2 shr 4) and 0x0F
        val digit5 = b2 and 0x0F
        return "$system$digit2${digit3.toString(16)}${digit4.toString(16)}${digit5.toString(16)}".uppercase()
    }

    /** True when a Mode 04 reply acknowledges the clear request. */
    fun isClearAcknowledged(messages: List<String>): Boolean = messages.any { it.startsWith("44") }

    /**
     * Reassembles CAN replies captured with headers on (`AT H1`, spaces off). Each line is
     * `<header><PCI><data...>`; single frames carry their length in the PCI byte, while
     * multi-frame replies are a first frame (`1L LL`) followed by consecutive frames (`2N`),
     * keyed per responding module so interleaved replies from several modules stay separate.
     *
     * @param headerLength 3 for 11-bit CAN ids (e.g. `7E8`), 8 for 29-bit ids (e.g. `18DAF110`).
     */
    fun assembleCanMessages(lines: List<String>, headerLength: Int): List<CanMessage> {
        val messages = mutableListOf<CanMessage>()
        val partial = LinkedHashMap<String, Pair<Int, StringBuilder>>()

        for (line in lines) {
            if (!HEX_ONLY.matches(line) || line.length < headerLength + 2) continue
            val header = line.take(headerLength)
            val frame = line.drop(headerLength)
            val pci = frame.take(2).toIntOrNull(16) ?: continue
            when (pci shr 4) {
                0x0 -> {
                    val length = pci and 0x0F
                    messages += CanMessage(header, frame.drop(2).take(length * 2))
                }
                0x1 -> {
                    val length = ((pci and 0x0F) shl 8) or (frame.substring(2, 4).toIntOrNull(16) ?: 0)
                    partial[header] = length to StringBuilder(frame.drop(4))
                }
                0x2 -> {
                    val (length, builder) = partial[header] ?: continue
                    builder.append(frame.drop(2))
                    if (builder.length >= length * 2) {
                        messages += CanMessage(header, builder.take(length * 2).toString())
                        partial.remove(header)
                    }
                }
            }
        }
        // Keep whatever arrived for messages whose last frame was lost.
        partial.forEach { (header, value) -> messages += CanMessage(header, value.second.toString()) }
        return messages
    }

    /** Decodes codes from headered CAN replies, remembering which module reported each one. */
    fun parseDtcsWithSources(messages: List<CanMessage>, responseMode: String): DtcReadResult {
        val codes = LinkedHashSet<String>()
        val sources = LinkedHashMap<String, MutableList<String>>()
        for (message in messages) {
            val moduleCodes = parseDtcs(listOf(message.payload), responseMode)
            val module = moduleName(message.header)
            moduleCodes.forEach { code ->
                codes += code
                val list = sources.getOrPut(code) { mutableListOf() }
                if (module !in list) list += module
            }
        }
        return DtcReadResult(codes.toList(), sources)
    }

    /** Human-readable name for the module behind a CAN response header. */
    fun moduleName(header: String): String {
        val address = when (header.length) {
            3 -> header.toIntOrNull(16)?.let { it - 0x7E8 }?.takeIf { it in 0..7 }
            8 -> header.takeLast(2).toIntOrNull(16)
            else -> null
        }
        return when {
            header.length == 3 && address == 0 -> "Engine (ECM)"
            header.length == 3 && address == 1 -> "Transmission (TCM)"
            header.length == 3 && address != null -> "Module ${address + 1}"
            header.length == 8 && address == 0x10 -> "Engine (ECM)"
            header.length == 8 && address == 0x18 -> "Transmission (TCM)"
            address != null -> "Module %02X".format(address)
            else -> "Module $header"
        }
    }

    // endregion

    // region Mode 02 (freeze frame)

    /** The DTC that caused freeze frame 0 to be stored (Mode 02 PID 02), or null if none. */
    fun parseFreezeFrameDtc(messages: List<String>): String? {
        val message = messages.firstOrNull { it.startsWith("420200") } ?: return null
        val hex = message.substring(6).take(4)
        if (hex.length < 4 || hex == "0000") return null
        return decodeDtc(hex)
    }

    /** Data bytes from a Mode 02 reply (`42 <pid> <frame> data...`). */
    fun freezeFrameData(messages: List<String>, pid: Int, minBytes: Int = 1): IntArray? {
        val prefix = "42" + pid.toHex() + "00"
        return messages.asSequence()
            .filter { it.startsWith(prefix) }
            .map { hexToBytes(it.substring(prefix.length)) }
            .firstOrNull { it.size >= minBytes }
    }

    // endregion

    // region Mode 01

    /** Returns the data bytes following `41 <pid>` from the first ECU that answered. */
    fun pidData(messages: List<String>, pid: Int, minBytes: Int = 1): IntArray? {
        val prefix = "41" + pid.toHex()
        return messages.asSequence()
            .filter { it.startsWith(prefix) }
            .map { hexToBytes(it.substring(prefix.length)) }
            .firstOrNull { it.size >= minBytes }
    }

    /**
     * Decodes a "PIDs supported" bitmap reply (Mode 01 PID 00/20/40/...), OR-ing answers
     * from every ECU together.
     */
    fun parseSupportedPids(messages: List<String>, basePid: Int): Set<Int> {
        val prefix = "41" + basePid.toHex()
        val supported = mutableSetOf<Int>()
        messages.filter { it.startsWith(prefix) }.forEach { message ->
            val bytes = hexToBytes(message.substring(prefix.length))
            if (bytes.size < 4) return@forEach
            for (byteIndex in 0 until 4) {
                for (bit in 0 until 8) {
                    if (bytes[byteIndex] and (0x80 shr bit) != 0) {
                        supported += basePid + byteIndex * 8 + bit + 1
                    }
                }
            }
        }
        return supported
    }

    fun parseMonitorStatus(messages: List<String>): MonitorStatus? {
        val data = pidData(messages, 0x01, minBytes = 4) ?: return null
        val (a, b, c, d) = data.toList()
        val isDiesel = b and 0x08 != 0
        val monitors = mutableListOf<ReadinessMonitor>()

        // Continuous monitors: availability in B bits 0-2, incomplete flag in B bits 4-6.
        listOf("Misfire", "Fuel System", "Components").forEachIndexed { i, name ->
            if (b and (1 shl i) != 0) monitors += ReadinessMonitor(name, complete = b and (1 shl (i + 4)) == 0)
        }

        val nonContinuous = if (isDiesel) {
            listOf("NMHC Catalyst", "NOx/SCR Aftertreatment", null, "Boost Pressure", null, "Exhaust Gas Sensor", "PM Filter", "EGR/VVT System")
        } else {
            listOf("Catalyst", "Heated Catalyst", "Evaporative System", "Secondary Air", "A/C Refrigerant", "Oxygen Sensor", "Oxygen Sensor Heater", "EGR/VVT System")
        }
        nonContinuous.forEachIndexed { bit, name ->
            if (name != null && c and (1 shl bit) != 0) {
                monitors += ReadinessMonitor(name, complete = d and (1 shl bit) == 0)
            }
        }

        return MonitorStatus(
            milOn = a and 0x80 != 0,
            storedDtcCount = a and 0x7F,
            isDiesel = isDiesel,
            monitors = monitors,
        )
    }

    /** Odometer (PID A6), in whole kilometers. */
    fun parseOdometerKm(messages: List<String>): Int? {
        val d = pidData(messages, 0xA6, minBytes = 4) ?: return null
        val raw = (d[0].toLong() shl 24) or (d[1].toLong() shl 16) or (d[2].toLong() shl 8) or d[3].toLong()
        return (raw / 10).toInt()
    }

    // endregion

    // region Mode 09

    /**
     * Decodes the VIN from a Mode 09 PID 02 reply, handling both the CAN layout (one
     * multi-frame message, `49 02 01` + 17 bytes) and the legacy layout (five
     * `49 02 <seq>` lines carrying 4 bytes each, the first zero-padded).
     */
    fun parseVin(messages: List<String>): String? {
        val vinMessages = messages.filter { it.startsWith("4902") }.map { hexToBytes(it.substring(4)) }
        if (vinMessages.isEmpty()) return null

        val payload: List<Int> = if (vinMessages.size > 1 && vinMessages.all { it.size == 5 }) {
            vinMessages.sortedBy { it[0] }.flatMap { it.drop(1) }
        } else {
            // Single (CAN) message: first byte is the number of data items.
            vinMessages.first().drop(1)
        }

        val vin = payload
            .filter { it != 0 }
            .map { it.toChar() }
            .filter { it.isLetterOrDigit() }
            .joinToString("")
            .uppercase()
        return vin.takeLast(17).takeIf { it.length == 17 }
    }

    // endregion

    // region Adapter commands

    /** Parses an `AT RV` reply such as `12.6V`. */
    fun parseAdapterVoltage(lines: List<String>): Double? =
        lines.firstNotNullOfOrNull { it.removeSuffix("V").toDoubleOrNull() }

    /** Parses an `AT DPN` reply such as `A6` (auto, protocol 6) into the protocol number. */
    fun parseProtocolNumber(lines: List<String>): Char? =
        lines.firstOrNull()?.removePrefix("A")?.firstOrNull()

    fun protocolName(number: Char?): String? = when (number) {
        '1' -> "SAE J1850 PWM"
        '2' -> "SAE J1850 VPW"
        '3' -> "ISO 9141-2"
        '4' -> "ISO 14230-4 KWP (5 baud init)"
        '5' -> "ISO 14230-4 KWP (fast init)"
        '6' -> "ISO 15765-4 CAN (11 bit, 500 kbaud)"
        '7' -> "ISO 15765-4 CAN (29 bit, 500 kbaud)"
        '8' -> "ISO 15765-4 CAN (11 bit, 250 kbaud)"
        '9' -> "ISO 15765-4 CAN (29 bit, 250 kbaud)"
        'A' -> "SAE J1939 CAN"
        else -> null
    }

    // endregion

    internal fun hexToBytes(hex: String): IntArray =
        IntArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: 0 }

    private fun Int.toHex(): String = "%02X".format(this)
}
