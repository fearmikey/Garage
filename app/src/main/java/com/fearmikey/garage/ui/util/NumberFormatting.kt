package com.fearmikey.garage.ui.util

/** Formats a raw mileage value for display with thousands separators, e.g. "15,230". */
fun Int.toDisplayMileage(): String = "%,d".format(this)

/**
 * Formats a user-entered mileage string with thousands separators (commas) as they type.
 * e.g. "12345" -> "12,345".
 */
fun formatMileageInput(input: String): String {
    val digits = input.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    val parsed = digits.toLongOrNull() ?: return digits
    return "%,d".format(parsed)
}

