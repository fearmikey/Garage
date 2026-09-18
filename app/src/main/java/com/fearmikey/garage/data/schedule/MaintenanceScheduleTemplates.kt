package com.fearmikey.garage.data.schedule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.ui.graphics.vector.ImageVector
import com.fearmikey.garage.data.local.entity.MaintenanceCategory

data class MaintenanceTemplateRule(
    val taskName: String,
    val category: MaintenanceCategory,
    val intervalMiles: Int?,
    val intervalMonths: Int?,
    val notes: String? = null,
)

data class MaintenanceTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val rules: List<MaintenanceTemplateRule>,
)

object MaintenanceScheduleTemplates {
    val templates: List<MaintenanceTemplate> = listOf(
        MaintenanceTemplate(
            id = "heavy_duty",
            title = "Heavy Duty & Towing",
            description = "Shortened fluid change intervals and severe-duty checks for trucks, towing rigs, or off-road vehicles.",
            icon = Icons.Outlined.LocalShipping,
            rules = listOf(
                MaintenanceTemplateRule(
                    taskName = "Severe Duty Engine Oil & Filter",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 3_500,
                    intervalMonths = 4,
                    notes = "Severe duty interval for frequent towing or heavy loads.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Transfer Case Fluid (Severe)",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 20_000,
                    intervalMonths = 24,
                    notes = "Recommended for frequent 4WD usage or water crossings.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Front & Rear Differential Fluid",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 20_000,
                    intervalMonths = 24,
                    notes = "Severe duty interval for towing and heavy loads.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Chassis Greasing & Ball Joints",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 10_000,
                    intervalMonths = 12,
                    notes = "Lube grease fittings, drive shaft U-joints, and suspension joints.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Heavy Duty Brake & Suspension Inspection",
                    category = MaintenanceCategory.BRAKES,
                    intervalMiles = 12_000,
                    intervalMonths = 12,
                    notes = "Inspect brake pad thickness, rotors, trailer brake controller, and leaf springs.",
                ),
            ),
        ),
        MaintenanceTemplate(
            id = "daily_commuter",
            title = "Daily Commuter",
            description = "Standard balanced maintenance intervals for daily drivers, highway commuters, and family vehicles.",
            icon = Icons.Outlined.DirectionsCar,
            rules = listOf(
                MaintenanceTemplateRule(
                    taskName = "Full Synthetic Oil & Filter",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 7_500,
                    intervalMonths = 6,
                    notes = "Standard full synthetic oil change interval.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Tire Rotation & Pressure Check",
                    category = MaintenanceCategory.TIRES,
                    intervalMiles = 7_500,
                    intervalMonths = 6,
                    notes = "Ensure even tread wear and optimal fuel efficiency.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Cabin & Engine Air Filters",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 15_000,
                    intervalMonths = 12,
                    notes = "Replace both air filters annually for HVAC & engine efficiency.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Multi-Point Inspection",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 15_000,
                    intervalMonths = 12,
                    notes = "Check belts, hoses, wiper blades, lights, and fluid levels.",
                ),
            ),
        ),
        MaintenanceTemplate(
            id = "hybrid_ev",
            title = "Hybrid & Electric Vehicle (EV)",
            description = "Optimized for hybrid and battery electric vehicles with emphasis on cabin filters, brake slides, and cooling systems.",
            icon = Icons.Outlined.ElectricCar,
            rules = listOf(
                MaintenanceTemplateRule(
                    taskName = "Cabin Air Filter & HV Battery Duct Check",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 15_000,
                    intervalMonths = 12,
                    notes = "Clean/replace cabin air filter and check high-voltage battery cooling fan intake.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Brake Slide Lubrication & Fluid Check",
                    category = MaintenanceCategory.BRAKES,
                    intervalMiles = 20_000,
                    intervalMonths = 24,
                    notes = "Regenerative braking reduces pad wear, but slide pins need regular lubrication to prevent seizing.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Tire Rotation & Alignment Inspection",
                    category = MaintenanceCategory.TIRES,
                    intervalMiles = 5_000,
                    intervalMonths = 6,
                    notes = "Instant EV motor torque causes faster tire wear.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Inverter & Battery Coolant Flush",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 50_000,
                    intervalMonths = 48,
                    notes = "Inspect and flush high-voltage component cooling system.",
                ),
            ),
        ),
        MaintenanceTemplate(
            id = "classic_high_mileage",
            title = "High Mileage & Classic Car",
            description = "Frequent inspections and short intervals for older vehicles, high-mileage engines, or vintage classics.",
            icon = Icons.Outlined.History,
            rules = listOf(
                MaintenanceTemplateRule(
                    taskName = "High-Zinc / High-Mileage Oil Change",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 3_000,
                    intervalMonths = 4,
                    notes = "Protects flat-tappet camshafts or high-mileage engine seals.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Cooling System & Hose Inspection",
                    category = MaintenanceCategory.FLUIDS,
                    intervalMiles = 12_000,
                    intervalMonths = 12,
                    notes = "Inspect radiator hoses, thermostat, heater core, and coolant condition.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Ignition, Spark Plugs & Distributer",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 15_000,
                    intervalMonths = 12,
                    notes = "Inspect spark plugs, ignition wires, distributor cap, and rotor/points.",
                ),
                MaintenanceTemplateRule(
                    taskName = "V-Belts & Tensioner Check",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 10_000,
                    intervalMonths = 12,
                    notes = "Check drive belt tension and inspect for cracking or glazing.",
                ),
                MaintenanceTemplateRule(
                    taskName = "Chassis & Steering Greasing",
                    category = MaintenanceCategory.OTHER,
                    intervalMiles = 3_000,
                    intervalMonths = 6,
                    notes = "Grease tie rod ends, ball joints, pitman arm, and suspension bushings.",
                ),
            ),
        ),
    )
}
