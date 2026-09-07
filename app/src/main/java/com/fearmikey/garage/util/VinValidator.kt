package com.fearmikey.garage.util

/**
 * Shared VIN format validation, used both by the manual entry field (for live
 * feedback) and the camera scanner (to decide whether a barcode/OCR candidate
 * is actually a VIN worth accepting).
 *
 * A VIN is always 17 characters, drawn from the Latin alphabet and digits,
 * *excluding* the letters I, O and Q (too easily confused with 1 and 0).
 */
object VinValidator {
    const val VIN_LENGTH = 17

    private val VIN_REGEX = Regex("^[A-HJ-NPR-Z0-9]{$VIN_LENGTH}$")

    /** A regex suitable for extracting a VIN-shaped token out of a larger block of OCR text. */
    val VIN_TOKEN_REGEX = Regex("[A-HJ-NPR-Z0-9]{$VIN_LENGTH}")

    fun isValidVin(candidate: String): Boolean = VIN_REGEX.matches(candidate.uppercase())

    /** Extracts the first VIN-shaped token from free-form OCR text, if any. */
    fun findVinInText(text: String): String? =
        VIN_TOKEN_REGEX.find(text.uppercase())?.value
}
