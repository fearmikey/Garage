package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.Vehicle

/**
 * A single "do this every N miles and/or every N months" maintenance interval rule.
 *
 * [makeMatch]/[modelMatch]/[drivetrains] narrow which vehicles a rule applies
 * to; leaving them `null` means "any". This lets [MaintenanceScheduleRules]
 * mix generic rules that apply to every vehicle (e.g. oil changes) with
 * narrow overrides that only make sense for specific configurations (e.g.
 * transfer case fluid, which only exists on 4WD/AWD vehicles).
 *
 * [intervalMiles] and [intervalMonths] can both be set (e.g. "every 6 months
 * or 5,000 miles, whichever comes first" -- crucial for low-mileage
 * vehicles), but at least one of them must be non-null.
 */
data class MaintenanceRule(
    val taskName: String,
    val category: MaintenanceCategory,
    val intervalMiles: Int? = null,
    val intervalMonths: Int? = null,
    val makeMatch: String? = null,
    val modelMatch: String? = null,
    val drivetrains: Set<Drivetrain>? = null,
    val notes: String? = null,
    /**
     * True for a user-defined rule (see [com.fearmikey.garage.data.local.entity.CustomMaintenanceRule])
     * rather than one of the built-in [MaintenanceScheduleRules]. Custom rules always take
     * precedence over a built-in rule sharing the same [taskName], regardless of specificity,
     * since a user explicitly configuring a task's interval is a stronger signal than any
     * generic or make/model-matched built-in rule.
     */
    val isCustom: Boolean = false,
) {
    init {
        require(intervalMiles != null || intervalMonths != null) {
            "MaintenanceRule '$taskName' must set intervalMiles and/or intervalMonths."
        }
    }

    /** How many matchers this rule pins down; used to prefer specific rules over generic ones. */
    val specificity: Int
        get() = listOfNotNull(makeMatch, modelMatch, drivetrains).size

    fun matches(vehicle: Vehicle): Boolean =
        ((makeMatch == null) || vehicle.make.equals(makeMatch, ignoreCase = true)) &&
            ((modelMatch == null) || vehicle.model.contains(modelMatch, ignoreCase = true)) &&
            ((drivetrains == null) || (vehicle.drivetrain in drivetrains))
}
