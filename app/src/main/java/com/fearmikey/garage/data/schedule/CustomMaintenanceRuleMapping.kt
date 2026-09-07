package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule

fun CustomMaintenanceRule.toMaintenanceRule(): MaintenanceRule =
    MaintenanceRule(
        taskName = taskName,
        category = category,
        intervalMiles = intervalMiles,
        intervalMonths = intervalMonths,
        notes = notes,
        isCustom = true,
    )
