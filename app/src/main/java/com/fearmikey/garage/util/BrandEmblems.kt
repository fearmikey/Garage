package com.fearmikey.garage.util

import androidx.annotation.DrawableRes
import com.fearmikey.garage.R

object BrandEmblems {

    /**
     * Normalizes a make string (lowercase, alphanumeric only) and resolves it to a bundled brand emblem resource ID.
     * Returns null if no emblem is available for the given make.
     */
    @DrawableRes
    fun forMake(make: String): Int? {
        val normalized = make.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (normalized.isBlank()) return null

        return when (normalized) {
            "acura" -> R.drawable.brand_acura
            "astonmartin", "aston" -> R.drawable.brand_astonmartin
            "audi" -> R.drawable.brand_audi
            "autozone" -> R.drawable.brand_autozone
            "bentley" -> R.drawable.brand_bentley
            "bmw" -> R.drawable.brand_bmw
            "bosch" -> R.drawable.brand_bosch
            "bugatti" -> R.drawable.brand_bugatti
            "cadillac" -> R.drawable.brand_cadillac
            "caterpillar", "cat" -> R.drawable.brand_caterpillar
            "chevrolet", "chevy" -> R.drawable.brand_chevrolet
            "chrysler" -> R.drawable.brand_chrysler
            "citroen" -> R.drawable.brand_citroen
            "dacia" -> R.drawable.brand_dacia
            "daf" -> R.drawable.brand_daf
            "dsautomobiles", "ds" -> R.drawable.brand_dsautomobiles
            "ducati" -> R.drawable.brand_ducati
            "ferrari" -> R.drawable.brand_ferrari
            "fiat" -> R.drawable.brand_fiat
            "ford" -> R.drawable.brand_ford
            "generalmotors", "gm" -> R.drawable.brand_generalmotors
            "honda" -> R.drawable.brand_honda
            "husqvarna" -> R.drawable.brand_husqvarna
            "hyundai" -> R.drawable.brand_hyundai
            "infiniti" -> R.drawable.brand_infiniti
            "iveco" -> R.drawable.brand_iveco
            "jcb" -> R.drawable.brand_jcb
            "jeep" -> R.drawable.brand_jeep
            "johndeere", "deere" -> R.drawable.brand_johndeere
            "kia" -> R.drawable.brand_kia
            "koenigsegg" -> R.drawable.brand_koenigsegg
            "ktm" -> R.drawable.brand_ktm
            "lamborghini", "lambo" -> R.drawable.brand_lamborghini
            "lucid", "lucidmotors" -> R.drawable.brand_lucid
            "mahindra" -> R.drawable.brand_mahindra
            "man" -> R.drawable.brand_man
            "maserati" -> R.drawable.brand_maserati
            "mazda" -> R.drawable.brand_mazda
            "mclaren" -> R.drawable.brand_mclaren
            "mercedes", "mercedesbenz", "benz", "mercedesamg" -> R.drawable.brand_mercedes
            "mg", "mgmotor" -> R.drawable.brand_mg
            "mini", "minicooper" -> R.drawable.brand_mini
            "mitsubishi" -> R.drawable.brand_mitsubishi
            "nissan" -> R.drawable.brand_nissan
            "opel" -> R.drawable.brand_opel
            "peugeot" -> R.drawable.brand_peugeot
            "polestar" -> R.drawable.brand_polestar
            "porsche" -> R.drawable.brand_porsche
            "ram", "ramtrucks" -> R.drawable.brand_ram
            "renault" -> R.drawable.brand_renault
            "rollsroyce", "rolls" -> R.drawable.brand_rollsroyce
            "saturn" -> R.drawable.brand_saturn
            "scania" -> R.drawable.brand_scania
            "seat" -> R.drawable.brand_seat
            "shell" -> R.drawable.brand_shell
            "skoda" -> R.drawable.brand_skoda
            "smart" -> R.drawable.brand_smart
            "subaru" -> R.drawable.brand_subaru
            "suzuki" -> R.drawable.brand_suzuki
            "tata", "tatamotors" -> R.drawable.brand_tata
            "tesla" -> R.drawable.brand_tesla
            "toyota" -> R.drawable.brand_toyota
            "vauxhall" -> R.drawable.brand_vauxhall
            "vespa" -> R.drawable.brand_vespa
            "volkswagen", "vw" -> R.drawable.brand_volkswagen
            "volvo" -> R.drawable.brand_volvo
            else -> null
        }
    }
}
