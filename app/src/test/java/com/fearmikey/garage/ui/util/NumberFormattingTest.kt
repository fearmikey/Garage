package com.fearmikey.garage.ui.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormattingTest {

    @Test
    fun `toDisplayMileage formats integers with thousands separators`() {
        assertEquals("0", 0.toDisplayMileage())
        assertEquals("123", 123.toDisplayMileage())
        assertEquals("1,234", 1234.toDisplayMileage())
        assertEquals("15,230", 15230.toDisplayMileage())
        assertEquals("100,000", 100000.toDisplayMileage())
        assertEquals("1,234,567", 1234567.toDisplayMileage())
    }

    @Test
    fun `formatMileageInput auto-adds commas as user types`() {
        assertEquals("", formatMileageInput(""))
        assertEquals("1", formatMileageInput("1"))
        assertEquals("12", formatMileageInput("12"))
        assertEquals("123", formatMileageInput("123"))
        assertEquals("1,234", formatMileageInput("1234"))
        assertEquals("12,345", formatMileageInput("12345"))
        assertEquals("123,456", formatMileageInput("123456"))
        assertEquals("1,234,567", formatMileageInput("1234567"))
    }

    @Test
    fun `formatMileageInput handles pre-existing commas and backspacing`() {
        assertEquals("123,456", formatMileageInput("12,3456"))
        assertEquals("1,234", formatMileageInput("12,34"))
        assertEquals("15,230", formatMileageInput("15,230"))
        assertEquals("", formatMileageInput(","))
    }

    @Test
    fun `sanitizeMileageInput extracts raw digits and handles leading zeros`() {
        assertEquals("", sanitizeMileageInput(""))
        assertEquals("", sanitizeMileageInput("abc"))
        assertEquals("0", sanitizeMileageInput("0"))
        assertEquals("0", sanitizeMileageInput("000"))
        assertEquals("5", sanitizeMileageInput("05"))
        assertEquals("123", sanitizeMileageInput("0123"))
        assertEquals("15230", sanitizeMileageInput("15,230"))
        assertEquals("123456", sanitizeMileageInput("123,456"))
        assertEquals("121", sanitizeMileageInput("120,5"))
        assertEquals("121", sanitizeMileageInput("120.5"))
    }

    @Test
    fun `parseToDoubleOrNull accepts both comma and dot as decimal separators`() {
        assertEquals(12.5, "12.5".parseToDoubleOrNull()!!, 0.001)
        assertEquals(12.5, "12,5".parseToDoubleOrNull()!!, 0.001)
        assertEquals(12.50, "12,50".parseToDoubleOrNull()!!, 0.001)
        assertEquals(0.95, "0,95".parseToDoubleOrNull()!!, 0.001)
        assertEquals(0.5, ",5".parseToDoubleOrNull()!!, 0.001)
        assertEquals(1234.56, "1.234,56".parseToDoubleOrNull()!!, 0.001)
        assertEquals(1234.56, "1,234.56".parseToDoubleOrNull()!!, 0.001)
        assertEquals(null, "".parseToDoubleOrNull())
        assertEquals(null, "abc".parseToDoubleOrNull())
    }

    @Test
    fun `parseToIntOrNull handles integers, thousands separators and decimal entries`() {
        assertEquals(120500, "120500".parseToIntOrNull()!!)
        assertEquals(120500, "120,500".parseToIntOrNull()!!)
        assertEquals(120500, "120.500".parseToIntOrNull()!!)
        assertEquals(120500, "120 500".parseToIntOrNull()!!)
        assertEquals(121, "120,5".parseToIntOrNull()!!)
        assertEquals(121, "120.5".parseToIntOrNull()!!)
        assertEquals(null, "".parseToIntOrNull())
        assertEquals(null, "abc".parseToIntOrNull())
    }

    @Test
    fun `ThousandsSeparatorVisualTransformation transforms and maps offsets correctly`() {
        val transformation = ThousandsSeparatorVisualTransformation()

        // 1. Empty string
        val emptyResult = transformation.filter(AnnotatedString(""))
        assertEquals("", emptyResult.text.text)
        assertEquals(0, emptyResult.offsetMapping.originalToTransformed(0))
        assertEquals(0, emptyResult.offsetMapping.transformedToOriginal(0))

        // 2. Three digits ("123") -> "123" (no commas)
        val shortResult = transformation.filter(AnnotatedString("123"))
        assertEquals("123", shortResult.text.text)
        assertEquals(0, shortResult.offsetMapping.originalToTransformed(0))
        assertEquals(1, shortResult.offsetMapping.originalToTransformed(1))
        assertEquals(2, shortResult.offsetMapping.originalToTransformed(2))
        assertEquals(3, shortResult.offsetMapping.originalToTransformed(3))
        assertEquals(0, shortResult.offsetMapping.transformedToOriginal(0))
        assertEquals(1, shortResult.offsetMapping.transformedToOriginal(1))

        // 3. Four digits ("1234") -> "1,234" (one comma)
        val fourDigitResult = transformation.filter(AnnotatedString("1234"))
        assertEquals("1,234", fourDigitResult.text.text)
        // originalToTransformed:
        // 0 -> 0 (before '1')
        // 1 -> 2 (after '1,')
        // 2 -> 3 (before '3')
        // 3 -> 4 (before '4')
        // 4 -> 5 (end)
        assertEquals(0, fourDigitResult.offsetMapping.originalToTransformed(0))
        assertEquals(2, fourDigitResult.offsetMapping.originalToTransformed(1))
        assertEquals(3, fourDigitResult.offsetMapping.originalToTransformed(2))
        assertEquals(4, fourDigitResult.offsetMapping.originalToTransformed(3))
        assertEquals(5, fourDigitResult.offsetMapping.originalToTransformed(4))

        // transformedToOriginal:
        // 0 ("") -> 0
        // 1 ("1") -> 1
        // 2 ("1,") -> 1
        // 3 ("1,2") -> 2
        // 4 ("1,23") -> 3
        // 5 ("1,234") -> 4
        assertEquals(0, fourDigitResult.offsetMapping.transformedToOriginal(0))
        assertEquals(1, fourDigitResult.offsetMapping.transformedToOriginal(1))
        assertEquals(1, fourDigitResult.offsetMapping.transformedToOriginal(2))
        assertEquals(2, fourDigitResult.offsetMapping.transformedToOriginal(3))
        assertEquals(3, fourDigitResult.offsetMapping.transformedToOriginal(4))
        assertEquals(4, fourDigitResult.offsetMapping.transformedToOriginal(5))

        // 4. Seven digits ("1234567") -> "1,234,567" (two commas)
        val sevenDigitResult = transformation.filter(AnnotatedString("1234567"))
        assertEquals("1,234,567", sevenDigitResult.text.text)
        assertEquals(0, sevenDigitResult.offsetMapping.originalToTransformed(0))
        assertEquals(2, sevenDigitResult.offsetMapping.originalToTransformed(1))
        assertEquals(6, sevenDigitResult.offsetMapping.originalToTransformed(4))
        assertEquals(9, sevenDigitResult.offsetMapping.originalToTransformed(7))

        assertEquals(0, sevenDigitResult.offsetMapping.transformedToOriginal(0))
        assertEquals(1, sevenDigitResult.offsetMapping.transformedToOriginal(1))
        assertEquals(1, sevenDigitResult.offsetMapping.transformedToOriginal(2))
        assertEquals(4, sevenDigitResult.offsetMapping.transformedToOriginal(6))
        assertEquals(7, sevenDigitResult.offsetMapping.transformedToOriginal(9))
    }
}
