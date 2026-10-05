package com.fearmikey.garage.obd

/**
 * Offline descriptions for generic (SAE J2012 "0"-series) diagnostic trouble codes.
 *
 * Exact titles live in the bundled `obd/dtc_generic.tsv` resource so the list can grow
 * without touching code. Codes that aren't listed fall back to a description of the
 * subsystem encoded in the code's structure, so every code always gets *some* helpful
 * context without any network access.
 */
object DtcDescriptions {

    private const val RESOURCE_PATH = "/obd/dtc_generic.tsv"

    data class Description(
        val title: String,
        /** True when [title] is an exact definition rather than a subsystem-level fallback. */
        val isExact: Boolean,
    )

    /** Number of codes with exact titles (exposed for tests/diagnostics). */
    val exactCount: Int get() = exact.size

    fun describe(code: String): Description {
        val normalized = code.trim().uppercase()
        exact[normalized]?.let { return Description(it, isExact = true) }
        return Description(fallback(normalized), isExact = false)
    }

    private fun fallback(code: String): String {
        if (code.length != 5) return "Unknown code"
        val system = code[0]
        val isManufacturer = code[1] == '1' || (system == 'P' && code[1] == '3') || (system != 'P' && code[1] == '2')
        val prefix = if (isManufacturer) "Manufacturer-specific" else "Generic"
        val area = when (system) {
            'P' -> when (code[2]) {
                '0' -> "fuel/air metering & auxiliary emissions"
                '1', '2' -> "fuel and air metering"
                '3' -> "ignition system or misfire"
                '4' -> "auxiliary emission controls"
                '5' -> "vehicle speed, idle control & auxiliary inputs"
                '6' -> "computer & output circuits"
                '7', '8', '9' -> "transmission"
                'A', 'B', 'C' -> "hybrid / electric propulsion"
                else -> "powertrain"
            }
            'B' -> "body system (airbags, seats, lighting, comfort)"
            'C' -> "chassis system (ABS, steering, suspension)"
            'U' -> "network / module communication"
            else -> return "Unknown code"
        }
        return "$prefix $area fault"
    }

    /** Loaded lazily on first lookup; an unreadable resource simply means fallbacks only. */
    private val exact: Map<String, String> by lazy {
        runCatching {
            DtcDescriptions::class.java.getResourceAsStream(RESOURCE_PATH)?.bufferedReader(Charsets.UTF_8)?.use(::parse)
        }.getOrNull().orEmpty()
    }

    internal fun parse(reader: java.io.Reader): Map<String, String> =
        reader.readLines()
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val code = line.substring(0, tab).trim().uppercase()
                val title = line.substring(tab + 1).trim()
                if (code.length == 5 && title.isNotEmpty()) code to title else null
            }
            .toMap()
}
