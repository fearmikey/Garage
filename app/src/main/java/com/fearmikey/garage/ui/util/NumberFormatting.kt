package com.fearmikey.garage.ui.util

/** Formats a raw mileage value for display with thousands separators, e.g. "15,230". */
fun Int.toDisplayMileage(): String = "%,d".format(this)
