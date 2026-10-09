package com.fearmikey.garage.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.roundToInt

/** Formats a raw mileage value for display with thousands separators, e.g. "15,230". */
fun Int.toDisplayMileage(): String = "%,d".format(this)

/**
 * Parses a string to a [Double] accepting both dot ('.') and comma (',') as decimal separators,
 * as well as handling strings with mixed separators (thousands vs decimal).
 */
fun String.parseToDoubleOrNull(): Double? {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return null

    val normalized = if (trimmed.contains(',') && trimmed.contains('.')) {
        val lastComma = trimmed.lastIndexOf(',')
        val lastDot = trimmed.lastIndexOf('.')
        if (lastComma > lastDot) {
            // EU style: 1.234,56 -> 1234.56
            trimmed.replace(".", "").replace(',', '.')
        } else {
            // US style: 1,234.56 -> 1234.56
            trimmed.replace(",", "")
        }
    } else {
        // Single separator (comma or dot) or none
        trimmed.replace(',', '.')
    }

    return normalized.toDoubleOrNull()
}

/**
 * Parses a string representing an integer (like mileage or odometer reading) accepting
 * numbers with thousands separators (commas, dots, spaces) or decimal numbers (e.g. 120,5 km -> 121).
 */
fun String.parseToIntOrNull(): Int? {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return null

    // Direct integer parse
    trimmed.toIntOrNull()?.let { return it }

    val hasComma = trimmed.contains(',')
    val hasDot = trimmed.contains('.')

    if (hasComma && !hasDot) {
        val parts = trimmed.split(',')
        if ((parts.size == 2) && (parts[1].length in 1..2)) {
            trimmed.replace(',', '.').toDoubleOrNull()?.let { return it.roundToInt() }
        }
    } else if (hasDot && !hasComma) {
        val parts = trimmed.split('.')
        if ((parts.size == 2) && (parts[1].length in 1..2)) {
            trimmed.toDoubleOrNull()?.let { return it.roundToInt() }
        }
    }

    return trimmed.filter { it.isDigit() }.toIntOrNull()
}

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

/**
 * Sanitizes raw mileage input to contain only digits, stripping leading zeros when followed by other digits,
 * while supporting decimal km/miles input (e.g. "120,5" -> "121").
 */
fun sanitizeMileageInput(input: String): String {
    val intValue = input.parseToIntOrNull()
    if (intValue != null) return intValue.toString()
    val digits = input.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    val trimmed = digits.dropWhile { it == '0' }
    return trimmed.ifEmpty { "0" }
}

/**
 * A [VisualTransformation] that formats numeric input (digits) with thousands separators (commas)
 * without altering the underlying raw digit string or shifting cursor position.
 */
class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = formatMileageInput(originalText)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, originalText.length)
                var digitCount = 0
                var transformedIndex = 0
                while ((transformedIndex < formattedText.length) && (digitCount < clampedOffset)) {
                    if (formattedText[transformedIndex].isDigit()) {
                        digitCount++
                    }
                    transformedIndex++
                }
                while ((transformedIndex < formattedText.length) && !formattedText[transformedIndex].isDigit()) {
                    transformedIndex++
                }
                return transformedIndex
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, formattedText.length)
                var digitCount = 0
                for (i in 0 until clampedOffset) {
                    if (formattedText[i].isDigit()) {
                        digitCount++
                    }
                }
                return digitCount.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}



