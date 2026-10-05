package com.fearmikey.garage.obd

/** How Garage talks to an OBD2 adapter. */
enum class ObdAdapterType(val displayName: String, val requiresBluetooth: Boolean) {
    /** Bluetooth Classic (SPP/RFCOMM) – the common ELM327 clones and most "paired" adapters. */
    CLASSIC("Bluetooth", true),

    /** Bluetooth Low Energy – Vgate iCar Pro BLE, OBDLink CX, Veepeak BLE+, etc. Not paired in system settings. */
    BLE("Bluetooth LE", true),

    /** Wi-Fi ELM327 adapters that expose a raw TCP socket (typically 192.168.0.10:35000). */
    WIFI("Wi-Fi", false);

    companion object {
        fun fromName(name: String?): ObdAdapterType = entries.firstOrNull { it.name == name } ?: CLASSIC
    }
}

/**
 * A saved/selected adapter.
 *
 * @param address MAC address for Bluetooth adapters, or `host:port` for Wi-Fi adapters.
 */
data class ObdAdapterConfig(
    val type: ObdAdapterType,
    val address: String,
    val name: String,
) {
    /** Host and port for [ObdAdapterType.WIFI] adapters. */
    val wifiHostPort: Pair<String, Int>?
        get() {
            if (type != ObdAdapterType.WIFI) return null
            val host = address.substringBeforeLast(':', address).trim()
            val port = address.substringAfterLast(':', "").toIntOrNull() ?: DEFAULT_WIFI_PORT
            return if (host.isBlank()) null else host to port
        }

    companion object {
        const val DEFAULT_WIFI_HOST = "192.168.0.10"
        const val DEFAULT_WIFI_PORT = 35000

        fun wifi(host: String, port: Int, name: String = "Wi-Fi OBD2 Adapter") =
            ObdAdapterConfig(ObdAdapterType.WIFI, "${host.trim()}:$port", name)
    }
}
