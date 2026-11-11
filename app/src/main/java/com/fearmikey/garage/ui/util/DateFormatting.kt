package com.fearmikey.garage.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats an epoch-millis timestamp (as stored on entities) for display in the app's current language,
 * e.g. "Jan 5, 2024" (English), "05.01.2024" (German), "5 янв. 2024 г." (Russian). The locale is read on
 * every call so an in-app language change takes effect immediately.
 */
fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

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
