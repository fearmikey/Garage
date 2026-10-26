package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.VehicleType

private val CAR_ONLY = setOf(VehicleType.CAR)
private val MOTORCYCLE_ONLY = setOf(VehicleType.MOTORCYCLE)

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
            taskName = "Engine oil change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 5_000,
            intervalMonths = 6,
            excludePureEv = true,
        ),
        MaintenanceRule(
            taskName = "Power steering fluid",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 50_000,
            intervalMonths = 60,
            excludePureEv = true,
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Tire Replacement",
            category = MaintenanceCategory.TIRES,
            intervalMiles = 50_000,
            intervalMonths = 48,
            isDeferrable = true,
        ),
        MaintenanceRule(
            taskName = "Rotation and Balance",
            category = MaintenanceCategory.TIRES,
            intervalMiles = 6_000,
            intervalMonths = 6,
            vehicleTypes = CAR_ONLY,
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
            isDeferrable = true,
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Engine air filter replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 12,
            isDeferrable = true,
            excludePureEv = true,
        ),
        MaintenanceRule(
            taskName = "Automatic transmission fluid service",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            intervalMonths = 36,
            excludePureEv = true,
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Coolant flush",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 60_000,
            intervalMonths = 60,
            excludePureEv = true,
        ),
        MaintenanceRule(
            taskName = "Spark plug replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 60_000,
            intervalMonths = 60,
            isDeferrable = true,
            excludePureEv = true,
        ),
        MaintenanceRule(
            taskName = "Battery load test / replacement",
            category = MaintenanceCategory.BATTERY,
            intervalMiles = 50_000,
            intervalMonths = 48,
            isDeferrable = true,
        ),
        MaintenanceRule(
            taskName = "Rust prevention",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 12,
            isDeferrable = true,
        ),
        MaintenanceRule(
            taskName = "Paint protection",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 12,
            isDeferrable = true,
        ),
        MaintenanceRule(
            taskName = "Serpentine belt replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 60_000,
            intervalMonths = 60,
            isDeferrable = true,
            excludePureEv = true,
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Wiper blade replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 10_000,
            intervalMonths = 6,
            isDeferrable = true,
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Brake pad replacement",
            category = MaintenanceCategory.BRAKES,
            intervalMiles = 40_000,
            intervalMonths = 48,
            isDeferrable = true,
        ),

        // Drivetrain-specific: applies to any 4WD/AWD vehicle unless a more
        // specific make/model rule below overrides it (see MaintenanceScheduleEngine
        // de-duplication by task name + specificity).
        MaintenanceRule(
            taskName = "Transfer case fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            drivetrains = setOf(Drivetrain.FOUR_WD, Drivetrain.AWD),
            vehicleTypes = CAR_ONLY,
            notes = "Generic 4WD/AWD interval; check your owner's manual for the exact figure.",
        ),

        // Motorcycle-only tasks.
        MaintenanceRule(
            taskName = "Drive chain clean & lube",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 600,
            intervalMonths = 2,
            vehicleTypes = MOTORCYCLE_ONLY,
            notes = "Chain-drive bikes only; also check slack. Ignore this for belt or shaft drive.",
        ),
        MaintenanceRule(
            taskName = "Chain & sprocket replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 48,
            isDeferrable = true,
            vehicleTypes = MOTORCYCLE_ONLY,
            notes = "Chain-drive bikes only. Replace chain and both sprockets together.",
        ),
        MaintenanceRule(
            taskName = "Valve clearance check",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 15_000,
            intervalMonths = 48,
            isDeferrable = true,
            excludePureEv = true,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),
        MaintenanceRule(
            taskName = "Fork oil change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 20_000,
            intervalMonths = 24,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),
        MaintenanceRule(
            taskName = "Clutch & throttle cable adjustment",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 6_000,
            intervalMonths = 12,
            vehicleTypes = MOTORCYCLE_ONLY,
            notes = "Lube and adjust free play on cable-operated clutches and throttles.",
        ),

        // Motorcycle overrides of generic tasks (shorter intervals than cars).
        MaintenanceRule(
            taskName = "Engine oil change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 4_000,
            intervalMonths = 12,
            excludePureEv = true,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),
        MaintenanceRule(
            taskName = "Tire Replacement",
            category = MaintenanceCategory.TIRES,
            intervalMiles = 10_000,
            intervalMonths = 60,
            isDeferrable = true,
            vehicleTypes = MOTORCYCLE_ONLY,
            notes = "Rear tires typically wear 1.5-2x faster than fronts; check tread and date codes.",
        ),
        MaintenanceRule(
            taskName = "Brake pad replacement",
            category = MaintenanceCategory.BRAKES,
            intervalMiles = 15_000,
            intervalMonths = 36,
            isDeferrable = true,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),
        MaintenanceRule(
            taskName = "Engine air filter replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 12_000,
            intervalMonths = 24,
            isDeferrable = true,
            excludePureEv = true,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),
        MaintenanceRule(
            taskName = "Spark plug replacement",
            category = MaintenanceCategory.OTHER,
            intervalMiles = 16_000,
            intervalMonths = 48,
            isDeferrable = true,
            excludePureEv = true,
            vehicleTypes = MOTORCYCLE_ONLY,
        ),

        // Make/model-specific overrides.
        MaintenanceRule(
            taskName = "Transfer case fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
            drivetrains = setOf(Drivetrain.FOUR_WD),
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Front differential fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
            drivetrains = setOf(Drivetrain.FOUR_WD),
            vehicleTypes = CAR_ONLY,
        ),
        MaintenanceRule(
            taskName = "Rear differential fluid change",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30_000,
            makeMatch = "Toyota",
            modelMatch = "Tacoma",
            vehicleTypes = CAR_ONLY,
        ),
    )
}
