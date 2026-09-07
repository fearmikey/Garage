package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory

/**
 * A small, hand-curated starter set of mileage- and time-based maintenance intervals.
 *
 * This is intentionally not exhaustive (there's no free, comprehensive "OEM
 * maintenance schedule" database) — it covers generic intervals that apply to
 * almost any vehicle, plus a handful of illustrative make/model/drivetrain
 * overrides. It's meant to be easy to extend over time rather than complete
 * on day one.
 *
 * Where a rule sets both [MaintenanceRule.intervalMiles] and
 * [MaintenanceRule.intervalMonths], the two combine as "whichever comes
 * first" (see [MaintenanceScheduleEngine]) -- this matters most for
 * low-mileage vehicles that would otherwise never trigger a mileage-only rule.
 */
object MaintenanceScheduleRules {
    val rules: List<MaintenanceRule> = listOf(
        // Generic rules: apply to any vehicle regardless of make/model/drivetrain.
        MaintenanceRule(
            taskName = "Oil & filter change",
            category = MaintenanceCategory.OIL_CHANGE,
            intervalMiles = 5_000,
            intervalMonths = 6,
        ),
        MaintenanceRule(
            taskName = "Tire rotation",
            category = MaintenanceCategory.TIRE_ROTATION,
            intervalMiles = 6_000,
            intervalMonths = 6,
        ),
        MaintenanceRule(
            taskName = "Brake fluid flush",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            intervalMonths = 24,
        ),
        MaintenanceRule(
            taskName = "Cabin air filter replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 12,
        ),
        MaintenanceRule(
            taskName = "Engine air filter replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 12,
        ),
        MaintenanceRule(
            taskName = "Automatic transmission fluid service",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            intervalMonths = 36,
        ),
        MaintenanceRule(
            taskName = "Coolant flush",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 60_000,
            intervalMonths = 60,
        ),
        MaintenanceRule(
            taskName = "Spark plug replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 60_000,
            intervalMonths = 60,
        ),
        MaintenanceRule(
            taskName = "Battery load test / replacement",
            category = MaintenanceCategory.BATTERY,
            intervalMiles = 50_000,
            intervalMonths = 48,
        ),

        // Drivetrain-specific: applies to any 4WD/AWD vehicle unless a more
        // specific make/model rule below overrides it (see MaintenanceScheduleEngine
        // de-duplication by task name + specificity).
        MaintenanceRule(
            taskName = "Transfer case fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            drivetrains = setOf(Drivetrain.FOUR_WD, Drivetrain.AWD),
            notes = "Generic 4WD/AWD interval; check your owner's manual for the exact figure.",
        ),

        // Make/model-specific overrides.
        MaintenanceRule(
            taskName = "Transfer case fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
            drivetrains = setOf(Drivetrain.FOUR_WD),
        ),
        MaintenanceRule(
            taskName = "Front differential fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
            drivetrains = setOf(Drivetrain.FOUR_WD),
        ),
        MaintenanceRule(
            taskName = "Rear differential fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
        ),
    )
}
