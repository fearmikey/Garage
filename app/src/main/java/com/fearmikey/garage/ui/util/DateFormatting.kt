package com.fearmikey.garage.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Formats an epoch-millis timestamp (as stored on entities) for display, e.g. "Jan 5, 2024". */
fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(DISPLAY_FORMATTER)
