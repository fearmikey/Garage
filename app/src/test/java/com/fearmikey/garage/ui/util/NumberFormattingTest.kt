package com.fearmikey.garage.ui.util

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
        // User has "12,345" and types '6' at the end -> input becomes "12,3456"
        assertEquals("123,456", formatMileageInput("12,3456"))

        // User has "12,345" and backspaces '5' -> input becomes "12,34"
        assertEquals("1,234", formatMileageInput("12,34"))

        // User pastes a formatted string
        assertEquals("15,230", formatMileageInput("15,230"))

        // User deletes all digits
        assertEquals("", formatMileageInput(","))
    }
}
