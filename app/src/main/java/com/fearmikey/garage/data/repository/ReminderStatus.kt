package com.fearmikey.garage.data.repository

/** How urgent a reminder or maintenance task currently is. */
enum class ReminderStatus {
    OK,
    UPCOMING,
    OVERDUE,
    COMPLETED
}
