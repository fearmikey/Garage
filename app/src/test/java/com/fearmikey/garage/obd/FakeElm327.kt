package com.fearmikey.garage.obd

import java.io.InputStream
import java.io.OutputStream

/**
 * In-memory ELM327 emulator. Each command written to [output] (terminated by `\r`) is looked
 * up in [responses] (or [headerResponses] while headers are on via `ATH1`) and the reply,
 * followed by the `>` prompt, becomes readable from [input]. Commands listed in
 * [silentCommands] never get a reply, to simulate a stalled adapter.
 */
class FakeElm327(
    responses: Map<String, String> = emptyMap(),
    headerResponses: Map<String, String> = emptyMap(),
    silentCommands: Set<String> = emptySet(),
) {
    val responses: MutableMap<String, String> = responses.toMutableMap()
    val headerResponses: MutableMap<String, String> = headerResponses.toMutableMap()
    val silentCommands: MutableSet<String> = silentCommands.toMutableSet()
    val commands = mutableListOf<String>()

    /** When true, every read fails as if the link dropped. */
    @Volatile
    var broken = false

    var headersOn = false
        private set

    private val pending = ArrayDeque<Byte>()
    private val current = StringBuilder()

    val input: InputStream = object : InputStream() {
        override fun read(): Int = synchronized(pending) { pending.removeFirstOrNull()?.toInt()?.and(0xFF) ?: -1 }
        override fun available(): Int {
            if (broken) throw java.io.IOException("Link dropped")
            return synchronized(pending) { pending.size }
        }
    }

    val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            if (broken) throw java.io.IOException("Link dropped")
            val c = b.toChar()
            if (c == '\r') {
                handle(current.toString())
                current.clear()
            } else {
                current.append(c)
            }
        }
    }

    /** Queues bytes as if they arrived late from a previous command. */
    fun injectStale(text: String) = synchronized(pending) { text.toByteArray().forEach(pending::addLast) }

    private fun handle(command: String) {
        commands += command
        when (command) {
            "ATH1" -> headersOn = true
            "ATH0", "ATZ" -> headersOn = false
        }
        if (command in silentCommands) return
        val reply = (if (headersOn) headerResponses[command] else null)
            ?: responses[command]
            ?: when {
                command.startsWith("AT") -> "OK"
                else -> "NO DATA"
            }
        synchronized(pending) { "$reply\r\r>".toByteArray().forEach(pending::addLast) }
    }

    companion object {
        /** A typical 2010s CAN gasoline car with one stored and one pending code. */
        fun canCar(): FakeElm327 = FakeElm327(
            responses = mapOf(
                "ATZ" to "\r\rELM327 v1.5",
                "ATRV" to "12.6V",
                "ATDPN" to "A6",
                // PIDs 01-20: 01,04,05,06,07,0B-10,11,1C,1F,20 supported
                "0100" to "SEARCHING...\r41 00 9E 3F 80 13",
                // PIDs 21-40: 2F, 31, 33, 40
                "0120" to "41 20 00 02 A0 01",
                // PIDs 41-60: 42, 46
                "0140" to "41 40 44 00 00 00",
                // MIL on, 1 code; spark ignition; misfire/fuel/components available & complete;
                // catalyst (bit0), evap (bit2), O2 (bit5), O2 heater (bit6) available; evap incomplete.
                "0101" to "41 01 81 07 65 04",
                "03" to "43 01 04 20",
                "07" to "47 01 01 71",
                "0A" to "4A 00",
                "0105" to "41 05 7B",
                "010C" to "41 0C 0C 1C",
                "010D" to "41 0D 32",
                "012F" to "41 2F 80",
                "0131" to "41 31 01 F4",
                "0142" to "41 42 36 B0",
                "0902" to "014\r0: 49 02 01 31 48 47\r1: 43 4D 38 32 36 33 33\r2: 41 30 30 34 33 35 32",
                "04" to "44",
                // Freeze frame for P0420
                "020200" to "42 02 00 04 20",
                "020C00" to "42 0C 00 26 48",
                "020D00" to "42 0D 00 58",
                "020500" to "42 05 00 83",
                "020700" to "42 07 00 8A",
            ),
            headerResponses = mapOf(
                // 11-bit CAN with headers: ECM (7E8) reports P0420, TCM (7E9) reports P0700.
                "03" to "7E8 04 43 01 04 20\r7E9 04 43 01 07 00",
                "07" to "7E8 04 47 01 01 71\r7E9 02 47 00",
                "0A" to "7E8 02 4A 00",
            ),
        )
    }
}
