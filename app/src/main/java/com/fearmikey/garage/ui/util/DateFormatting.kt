package com.fearmikey.garage.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Formats an epoch-millis timestamp (as stored on entities) for display, e.g. "Jan 5, 2024". */
fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(DISPLAY_FORMATTER)

/**
 * Converts a local epoch-millis timestamp to UTC epoch-millis at start of day in UTC,
 * suitable for initializing Compose's [androidx.compose.material3.rememberDatePickerState].
 */
fun Long.toUtcDatePickerMillis(): Long {
    val localDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

/**
 * Converts UTC epoch-millis returned by Compose's [androidx.compose.material3.DatePickerState.selectedDateMillis]
 * back to a local epoch-millis timestamp at start of day in the system default timezone.
 */
fun Long.fromUtcDatePickerMillis(): Long {
    val selectedUtcDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    return selectedUtcDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
