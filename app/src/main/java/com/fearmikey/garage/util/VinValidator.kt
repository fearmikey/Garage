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

    /**
     * Cleans up manually typed / pasted VIN text: uppercases, drops whitespace, dashes and
     * any other separators (VINs printed on European registration documents are often
     * grouped with spaces), and applies the standard VIN transcription rule that the
     * never-valid letters I, O and Q are really the digits 1, 0 and 0.
     */
    fun normalize(raw: String): String = buildString {
        var chars = raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
        // Code 39 VIN barcodes (US door-jamb labels) often carry a leading "I" import marker.
        if (chars.length == VIN_LENGTH + 1 && chars.first() == 'I') chars = chars.drop(1)
        for (c in chars) {
            when (c) {
                'I' -> append('1')
                'O', 'Q' -> append('0')
                in 'A'..'Z', in '0'..'9' -> append(c)
                else -> Unit // separators, whitespace, punctuation
            }
        }
    }

    /**
     * Extracts a VIN from scanned barcode/QR contents. Returns null when the code doesn't
     * contain one: e.g. QR codes on European registration documents or stickers often
     * encode a short document/serial number instead of the VIN.
     */
    fun extractVinFromScan(contents: String): String? {
        // Prefer an exact 17-char token bounded by non-VIN chars, so we don't slice a VIN
        // out of a longer alphanumeric run.
        Regex("(?<![A-Z0-9])[A-HJ-NPR-Z0-9]{$VIN_LENGTH}(?![A-Z0-9])")
            .find(contents.uppercase())?.value?.let { return it }
        // Fall back to the whole payload being a VIN with separators / an import marker.
        return normalize(contents).takeIf { isValidVin(it) }
    }

    /** Extracts the first VIN-shaped token from free-form OCR text, if any. */
    fun findVinInText(text: String): String? =
        VIN_TOKEN_REGEX.find(text.uppercase())?.value
}
