package com.fearmikey.garage.ui.util

import kotlin.math.roundToInt

enum class UnitSystem(
    val distanceUnit: String,
    val volumeUnit: String,
    val fuelEconomyUnit: String,
    val pricePerVolumeUnit: String,
) {
    IMPERIAL(
        distanceUnit = "mi",
        volumeUnit = "gal",
        fuelEconomyUnit = "mpg",
        pricePerVolumeUnit = "$/gal",
    ),
    METRIC(
        distanceUnit = "km",
        volumeUnit = "L",
        fuelEconomyUnit = "L/100km",
        pricePerVolumeUnit = "$/L",
    );

    companion object {
        fun fromString(value: String?): UnitSystem =
            if (value.equals("metric", ignoreCase = true)) METRIC else IMPERIAL
    }
}

object UnitConverter {
    const val KM_PER_MILE = 1.609344
    const val LITERS_PER_GALLON = 3.785411784
    const val MPG_TO_L100KM_FACTOR = 235.214583

    // Distance conversions
    fun milesToKm(miles: Int): Int = (miles * KM_PER_MILE).roundToInt()
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE
    fun kmToMiles(km: Int): Int = (km / KM_PER_MILE).roundToInt()
    fun kmToMiles(km: Double): Double = km / KM_PER_MILE

    /** Canonical mileage (stored in miles) -> Display mileage integer based on selected [UnitSystem]. */
    fun displayDistanceValue(miles: Int, unitSystem: UnitSystem): Int = when (unitSystem) {
        UnitSystem.IMPERIAL -> miles
        UnitSystem.METRIC -> milesToKm(miles)
    }

    /** Display mileage input (user-entered in miles or km) -> Canonical miles integer for DB. */
    fun canonicalMilesFromInput(input: Int, unitSystem: UnitSystem): Int = when (unitSystem) {
        UnitSystem.IMPERIAL -> input
        UnitSystem.METRIC -> kmToMiles(input)
    }

    /** Formats a canonical mileage (miles) with thousands separators and unit label, e.g. "15,230 mi" or "24,510 km". */
    fun formatDistance(miles: Int, unitSystem: UnitSystem): String {
        val value = displayDistanceValue(miles, unitSystem)
        return "${value.toDisplayMileage()} ${unitSystem.distanceUnit}"
    }

    // Volume conversions
    fun gallonsToLiters(gallons: Double): Double = gallons * LITERS_PER_GALLON
    fun litersToGallons(liters: Double): Double = liters / LITERS_PER_GALLON

    /** Canonical volume (gallons) -> Display volume value based on [UnitSystem]. */
    fun displayVolumeValue(gallons: Double, unitSystem: UnitSystem): Double = when (unitSystem) {
        UnitSystem.IMPERIAL -> gallons
        UnitSystem.METRIC -> gallonsToLiters(gallons)
    }

    /** Display volume input (user-entered in gallons or liters) -> Canonical gallons for DB. */
    fun canonicalGallonsFromInput(input: Double, unitSystem: UnitSystem): Double = when (unitSystem) {
        UnitSystem.IMPERIAL -> input
        UnitSystem.METRIC -> litersToGallons(input)
    }

    /** Formats canonical volume (gallons) for display, e.g. "12.500 gal" or "47.318 L". */
    fun formatVolume(gallons: Double, unitSystem: UnitSystem): String {
        val value = displayVolumeValue(gallons, unitSystem)
        return "%.3f %s".format(value, unitSystem.volumeUnit)
    }

    /** Formats price per unit volume, given pricePerGallon. E.g. "$3.500/gal" or "€0.925/L". */
    fun formatPricePerVolume(pricePerGallon: Double, unitSystem: UnitSystem, currencySymbol: String = "$"): String = when (unitSystem) {
        UnitSystem.IMPERIAL -> "%s%.3f/gal".format(currencySymbol, pricePerGallon)
        UnitSystem.METRIC -> "%s%.3f/L".format(currencySymbol, pricePerGallon / LITERS_PER_GALLON)
    }

    // Fuel economy conversions
    /** Converts MPG to L/100km, or null if MPG <= 0. */
    fun mpgToLitersPer100Km(mpg: Double): Double? =
        if (mpg > 0.0) MPG_TO_L100KM_FACTOR / mpg else null

    /** Formats average/calculated fuel economy (given MPG) according to [UnitSystem]. */
    fun formatFuelEconomy(mpg: Double?, unitSystem: UnitSystem): String {
        if (mpg == null || mpg <= 0.0) return "—"
        return when (unitSystem) {
            UnitSystem.IMPERIAL -> "%.1f mpg".format(mpg)
            UnitSystem.METRIC -> {
                val l100km = mpgToLitersPer100Km(mpg)
                if (l100km != null) "%.1f L/100km".format(l100km) else "—"
            }
        }
    }

    /** Formats segment fuel economy text, e.g. "28.5 mpg since last full tank" or "8.3 L/100km since last full tank". */
    fun formatSegmentFuelEconomy(mpg: Double, unitSystem: UnitSystem): String = when (unitSystem) {
        UnitSystem.IMPERIAL -> "%.1f mpg since last full tank".format(mpg)
        UnitSystem.METRIC -> {
            val l100km = mpgToLitersPer100Km(mpg)
            if (l100km != null) "%.1f L/100km since last full tank".format(l100km)
            else "—"
        }
    }

    // EV Charging efficiency conversions
    /** Formats Wh/mi (or Wh/km) according to [UnitSystem]. */
    fun formatWhPerMi(whPerMi: Double?, unitSystem: UnitSystem): String {
        if (whPerMi == null || whPerMi <= 0.0) return "—"
        return when (unitSystem) {
            UnitSystem.IMPERIAL -> "%.0f Wh/mi".format(whPerMi)
            UnitSystem.METRIC -> "%.0f Wh/km".format(whPerMi / KM_PER_MILE)
        }
    }

    /** Formats kWh/100km (or kWh/100mi) according to [UnitSystem]. */
    fun formatKwhPer100Km(whPerMi: Double?, unitSystem: UnitSystem): String {
        if (whPerMi == null || whPerMi <= 0.0) return "—"
        return when (unitSystem) {
            UnitSystem.IMPERIAL -> {
                val kwh100mi = (whPerMi * 100.0) / 1000.0
                "%.1f kWh/100mi".format(kwh100mi)
            }
            UnitSystem.METRIC -> {
                val whPerKm = whPerMi / KM_PER_MILE
                val kwh100km = (whPerKm * 100.0) / 1000.0
                "%.1f kWh/100km".format(kwh100km)
            }
        }
    }

    /** Formats MPGe (Miles per Gallon Equivalent). */
    fun formatMpge(mpge: Double?): String {
        if (mpge == null || mpge <= 0.0) return "—"
        return "%.1f MPGe".format(mpge)
    }

    // Vehicle Parts & Fluids Spec Conversions
    /** Formats oil capacity according to [UnitSystem] (e.g. "5.2 qts" <-> "4.9 L"). */
    fun formatOilCapacity(capacityStr: String?, unitSystem: UnitSystem): String? {
        if (capacityStr.isNullOrBlank()) return null
        if (capacityStr.contains("N/A", ignoreCase = true)) return capacityStr
        val numMatch = Regex("""\d+(?:[.,]\d+)?""").find(capacityStr)?.value?.parseToDoubleOrNull() ?: return capacityStr
        val hasLiters = capacityStr.contains("L", ignoreCase = true) && !capacityStr.contains("gal", ignoreCase = true)
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                if (hasLiters) "%.1f L".format(numMatch)
                else "%.1f L".format(numMatch * 0.946353)
            }
            UnitSystem.IMPERIAL -> {
                if (hasLiters) "%.1f qts".format(numMatch / 0.946353)
                else "%.1f qts".format(numMatch)
            }
        }
    }

    /** Formats tire pressure according to [UnitSystem] (e.g. "35 PSI" <-> "2.4 bar"). */
    fun formatTirePressure(psiStr: String?, unitSystem: UnitSystem): String? {
        if (psiStr.isNullOrBlank()) return null
        if (psiStr.contains("N/A", ignoreCase = true)) return psiStr
        val numMatch = Regex("""\d+(?:[.,]\d+)?""").find(psiStr)?.value?.parseToDoubleOrNull() ?: return psiStr
        val hasBar = psiStr.contains("bar", ignoreCase = true)
        val hasKpa = psiStr.contains("kPa", ignoreCase = true)
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                if (hasBar) "%.1f bar".format(numMatch)
                else if (hasKpa) "${numMatch.roundToInt()} kPa"
                else "%.1f bar".format(numMatch * 0.0689476)
            }
            UnitSystem.IMPERIAL -> {
                if (hasBar) "${(numMatch / 0.0689476).roundToInt()} PSI"
                else if (hasKpa) "${(numMatch * 0.1450377).roundToInt()} PSI"
                else "${numMatch.roundToInt()} PSI"
            }
        }
    }

    /** Formats wiper blade sizes according to [UnitSystem] (e.g. "26 in" <-> "650 mm"). */
    fun formatWiperSize(sizeStr: String?, unitSystem: UnitSystem): String? {
        if (sizeStr.isNullOrBlank()) return null
        if (sizeStr.contains("N/A", ignoreCase = true)) return sizeStr
        val numMatch = Regex("""\d+(?:[.,]\d+)?""").find(sizeStr)?.value?.parseToDoubleOrNull() ?: return sizeStr
        val hasMm = sizeStr.contains("mm", ignoreCase = true)
        val hasCm = sizeStr.contains("cm", ignoreCase = true)
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                if (hasMm) "${numMatch.roundToInt()} mm"
                else if (hasCm) "${(numMatch * 10).roundToInt()} mm"
                else "${(numMatch * 25.4).roundToInt()} mm"
            }
            UnitSystem.IMPERIAL -> {
                if (hasMm) "${(numMatch / 25.4).roundToInt()} in"
                else if (hasCm) "${(numMatch * 10 / 25.4).roundToInt()} in"
                else "${numMatch.roundToInt()} in"
            }
        }
    }

    /** Formats spark plug gap according to [UnitSystem] (e.g. "0.040 in" <-> "1.0 mm"). */
    fun formatSparkPlugGap(gapStr: String?, unitSystem: UnitSystem): String? {
        if (gapStr.isNullOrBlank()) return null
        if (gapStr.contains("N/A", ignoreCase = true)) return gapStr
        val numMatch = Regex("""\d+(?:[.,]\d+)?""").find(gapStr)?.value?.parseToDoubleOrNull() ?: return gapStr
        val hasMm = gapStr.contains("mm", ignoreCase = true)
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                if (hasMm) "%.1f mm".format(numMatch)
                else "%.1f mm".format(numMatch * 25.4)
            }
            UnitSystem.IMPERIAL -> {
                if (hasMm) "%.3f in".format(numMatch / 25.4)
                else "%.3f in".format(numMatch)
            }
        }
    }
}
