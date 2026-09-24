package com.fearmikey.garage.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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

/**
 * Sanitizes raw mileage input to contain only digits, stripping leading zeros when followed by other digits.
 * e.g. "05" -> "5", "0" -> "0", "" -> "".
 */
fun sanitizeMileageInput(input: String): String {
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


