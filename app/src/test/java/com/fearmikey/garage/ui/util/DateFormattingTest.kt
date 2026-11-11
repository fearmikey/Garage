package com.fearmikey.garage.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class DateFormattingTest {

    @Test
    fun `toUtcDatePickerMillis and fromUtcDatePickerMillis preserve selected date`() {
        val selectedDate = LocalDate.of(2025, 6, 14)
        val localMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val utcPickerMillis = localMillis.toUtcDatePickerMillis()

        // DatePicker UTC millis should represent June 14, 2025 at 00:00:00 UTC
        val dateInUtc = LocalDate.ofInstant(Instant.ofEpochMilli(utcPickerMillis), ZoneOffset.UTC)
        assertEquals(selectedDate, dateInUtc)

        val restoredLocalMillis = utcPickerMillis.fromUtcDatePickerMillis()
        assertEquals(
            selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())),
            restoredLocalMillis.toDisplayDate(),
        )
    }

    @Test
    fun `toDisplayDate follows the current locale`() {
        val millis = LocalDate.of(2026, 11, 10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("10.11.2026", millis.toDisplayDate())
            Locale.setDefault(Locale.US)
            assertEquals("Nov 10, 2026", millis.toDisplayDate())
        } finally {
            Locale.setDefault(original)
        }
    }
}
