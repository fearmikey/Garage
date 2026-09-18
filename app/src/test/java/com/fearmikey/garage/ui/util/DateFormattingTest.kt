package com.fearmikey.garage.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
        assertEquals(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), restoredLocalMillis.toDisplayDate())
    }
}
