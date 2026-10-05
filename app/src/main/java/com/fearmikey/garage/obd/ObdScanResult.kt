package com.fearmikey.garage.obd

/** Everything gathered during a single diagnostic scan. Every field is optional. */
data class ObdScanResult(
    val storedDtcs: List<String> = emptyList(),
    val pendingDtcs: List<String> = emptyList(),
    val permanentDtcs: List<String> = emptyList(),
    val monitorStatus: MonitorStatus? = null,
    val odometerKm: Int? = null,
    val distanceSinceClearedKm: Int? = null,
    val vin: String? = null,
    val readings: List<ObdReading> = emptyList(),
    val adapterVoltage: Double? = null,
    val protocolName: String? = null,
    /** Module(s) that reported each code, when known (CAN vehicles only). */
    val dtcSources: Map<String, List<String>> = emptyMap(),
    val freezeFrame: FreezeFrame? = null,
    /** Number of individual requests that timed out or failed; the rest of the scan still ran. */
    val failedRequests: Int = 0,
) {
    val hasAnyCodes: Boolean
        get() = storedDtcs.isNotEmpty() || pendingDtcs.isNotEmpty() || permanentDtcs.isNotEmpty()
}

/** Builds the human-readable text saved to the maintenance timeline for a scan. */
object ObdScanSummary {

    const val SCAN_TASK_NAME = "OBD2 Scan"
    const val CLEAR_TASK_NAME = "OBD2 Codes Cleared"

    fun describe(result: ObdScanResult): String = buildString {
        append("OBD2 scan")
        result.monitorStatus?.let { append(if (it.milOn) " — check engine light ON" else " — check engine light off") }
        append(".")

        if (!result.hasAnyCodes) {
            append(" No trouble codes.")
        } else {
            appendCodes("Stored", result.storedDtcs)
            appendCodes("Pending", result.pendingDtcs)
            appendCodes("Permanent", result.permanentDtcs)
        }

        result.monitorStatus?.takeIf { it.monitors.isNotEmpty() }?.let { status ->
            append(" Readiness: ${status.completeCount}/${status.monitors.size} monitors complete")
            val incomplete = status.monitors.filterNot { it.complete }.map { it.name }
            if (incomplete.isNotEmpty()) append(" (not ready: ${incomplete.joinToString()})")
            append(".")
        }
    }

    /** Readiness information recovered from a saved scan's timeline description. */
    data class SavedReadiness(
        val milOn: Boolean?,
        val complete: Int,
        val total: Int,
        val notReady: List<String>,
    ) {
        /** Most emissions programs allow at most one incomplete monitor (two for 1996–2000 vehicles). */
        val likelyReady: Boolean get() = milOn != true && (total - complete) <= 1
    }

    private val READINESS_REGEX = Regex("""Readiness: (\d+)/(\d+) monitors complete(?: \(not ready: ([^)]*)\))?""")

    /**
     * Parses the readiness section written by [describe]. Returns null for records that
     * didn't include readiness (older scans or vehicles that don't report it).
     */
    fun parseReadiness(description: String): SavedReadiness? {
        val match = READINESS_REGEX.find(description) ?: return null
        val milOn = when {
            description.contains("check engine light ON") -> true
            description.contains("check engine light off") -> false
            else -> null
        }
        return SavedReadiness(
            milOn = milOn,
            complete = match.groupValues[1].toInt(),
            total = match.groupValues[2].toInt(),
            notReady = match.groupValues[3].split(", ").filter { it.isNotBlank() },
        )
    }

    fun describeClear(clearedCodes: List<String>): String =
        if (clearedCodes.isEmpty()) {
            "OBD2 trouble codes cleared."
        } else {
            "OBD2 trouble codes cleared: ${clearedCodes.joinToString()}."
        }

    private fun StringBuilder.appendCodes(label: String, codes: List<String>) {
        if (codes.isEmpty()) return
        append(" $label: ")
        append(codes.joinToString { code -> "$code (${DtcDescriptions.describe(code).title})" })
        append(".")
    }
}
