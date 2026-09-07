package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.Vehicle

/**
 * A single "do this every N miles" maintenance interval rule.
 *
 * [makeMatch]/[modelMatch]/[drivetrains] narrow which vehicles a rule applies
 * to; leaving them `null` means "any". This lets [MaintenanceScheduleRules]
 * mix generic rules that apply to every vehicle (e.g. oil changes) with
 * narrow overrides that only make sense for specific configurations (e.g.
 * transfer case fluid, which only exists on 4WD/AWD vehicles).
 */
data class MaintenanceRule(
    val taskName: String,
    val category: MaintenanceCategory,
    val intervalMiles: Int,
    val makeMatch: String? = null,
    val modelMatch: String? = null,
    val drivetrains: Set<Drivetrain>? = null,
    val notes: String? = null,
) {
    /** How many matchers this rule pins down; used to prefer specific rules over generic ones. */
    val specificity: Int
        get() = listOfNotNull(makeMatch, modelMatch, drivetrains).size

    fun matches(vehicle: Vehicle): Boolean =
        ((makeMatch == null) || vehicle.make.equals(makeMatch, ignoreCase = true)) &&
            ((modelMatch == null) || vehicle.model.contains(modelMatch, ignoreCase = true)) &&
            ((drivetrains == null) || (vehicle.drivetrain in drivetrains))
}
