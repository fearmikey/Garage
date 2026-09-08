package com.fearmikey.garage.ui.util

import com.fearmikey.garage.data.fuel.FuelEconomyCalculator
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
import com.fearmikey.garage.data.schedule.toMaintenanceRule
import java.util.concurrent.TimeUnit

/**
 * Static sample data used only by `@Preview` composables, so screens render
 * meaningfully in the IDE/Preview tooling without needing a real device or
 * database. Never referenced from production code paths.
 */
object SampleData {
    val tacoma = Vehicle(
        id = 1,
        vin = "3TYCZ5AN0PT000001",
        year = 2023,
        make = "Toyota",
        model = "Tacoma",
        trim = "TRD Off-Road",
        imageUri = null,
        drivetrain = Drivetrain.FOUR_WD,
    )

    val civic = Vehicle(
        id = 2,
        vin = "2HGFE2F59PH000002",
        year = 2021,
        make = "Honda",
        model = "Civic",
        trim = "Sport",
        imageUri = null,
    )

    val vehicles = listOf(tacoma, civic)

    val tacomaSpecs = VehicleSpecs(
        vehicleId = tacoma.id,
        engineCylinders = "6",
        displacementL = "3.5",
        engineHp = "278",
        fuelType = "Gasoline",
        transmissionStyle = "Automatic",
        transmissionSpeeds = "6",
        bodyClass = "Pickup",
        doors = "4",
        gvwr = "Class 2E: 6,001 - 7,000 lb",
        series = "TRD Off-Road",
        vehicleType = "Truck",
        plantCity = "San Antonio",
        plantState = "Texas",
        plantCountry = "United States",
        manufacturer = "Toyota Motor Manufacturing, Texas, Inc.",
    )

    val tacomaPartsInfo = VehiclePartsInfo(
        vehicleId = tacoma.id,
        oilViscosity = "0W-20",
        oilCapacity = "6.2 qt",
        oilFilterPartNumber = "Toyota 04152-YZZA1",
        sparkPlugPartNumber = "Denso SK20HR11",
        sparkPlugGap = "0.043 in",
        tireSizeFront = "265/70R16",
        tireSizeRear = "265/70R16",
        tirePsiFront = "32 psi",
        tirePsiRear = "32 psi",
        wiperBladeSizeDriver = "26 in",
        wiperBladeSizePassenger = "20 in",
        wiperBladeSizeRear = "12 in",
    )

    private val now = System.currentTimeMillis()
    private val day = TimeUnit.DAYS.toMillis(1)

    val tacomaMaintenanceRecords = listOf(
        MaintenanceRecord(
            id = 1,
            vehicleId = tacoma.id,
            date = now - (10 * day),
            mileage = 15230,
            description = "Full synthetic engine oil change + filter",
            cost = 89.99,
            category = MaintenanceCategory.FLUIDS,
            taskName = "Engine oil change",
        ),
        MaintenanceRecord(
            id = 2,
            vehicleId = tacoma.id,
            date = now - (95 * day),
            mileage = 12100,
            description = "Rotated tires, checked tread depth",
            cost = 40.0,
            category = MaintenanceCategory.TIRES,
            taskName = "Rotation and Balance",
        ),
        MaintenanceRecord(
            id = 3,
            vehicleId = tacoma.id,
            date = now - (200 * day),
            mileage = 8000,
            description = "Replaced front brake pads",
            cost = 210.50,
            category = MaintenanceCategory.BRAKES,
        ),
    )

    val tacomaReminders = listOf(
        Reminder(
            id = 1,
            vehicleId = tacoma.id,
            taskName = "Engine oil change",
            dueDate = now + (20 * day),
            dueMileage = 18000,
            isCompleted = false,
        ),
        Reminder(
            id = 2,
            vehicleId = tacoma.id,
            taskName = "Registration renewal",
            dueDate = now - (2 * day),
            dueMileage = null,
            isCompleted = false,
        ),
        Reminder(
            id = 3,
            vehicleId = tacoma.id,
            taskName = "Cabin air filter",
            dueDate = now + (120 * day),
            dueMileage = 20000,
            isCompleted = false,
        ),
    )

    const val TACOMA_LATEST_MILEAGE = 15230

    val tacomaCustomRules = listOf(
        CustomMaintenanceRule(
            id = 1,
            vehicleId = tacoma.id,
            taskName = "Diff fluid change (locker)",
            category = MaintenanceCategory.FLUIDS,
            intervalMiles = 30000,
            intervalMonths = null,
            notes = "Aftermarket locker manufacturer recommendation",
        ),
        CustomMaintenanceRule(
            id = 2,
            vehicleId = tacoma.id,
            taskName = "Track day brake inspection",
            category = MaintenanceCategory.BRAKES,
            intervalMiles = null,
            intervalMonths = 3,
        ),
    )

    val tacomaMaintenanceSuggestions = MaintenanceScheduleEngine.suggestionsFor(
        vehicle = tacoma,
        latestMileage = TACOMA_LATEST_MILEAGE,
        records = tacomaMaintenanceRecords,
        customRules = tacomaCustomRules.map { it.toMaintenanceRule() },
    )

    val tacomaRecalls = listOf(
        VehicleRecall(
            campaignNumber = "23V123000",
            component = "FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP",
            summary = "Toyota is recalling certain vehicles. The low-pressure fuel pump may fail.",
            consequence = "An inoperative fuel pump can cause the engine to stall, increasing crash risk.",
            remedy = "Dealers will replace the fuel pump free of charge.",
            reportedDate = "05/12/2023",
            parkIt = false,
            parkOutside = false,
        ),
        VehicleRecall(
            campaignNumber = "22V456000",
            component = "ELECTRICAL SYSTEM:12V/24V/48V BATTERY",
            summary = "Toyota is recalling certain vehicles. The battery cable may chafe against a bracket.",
            consequence = "Chafing could cause a short circuit, increasing the risk of a fire.",
            remedy = "Dealers will inspect and, if necessary, repair the wire harness free of charge.",
            reportedDate = "18/07/2022",
            parkIt = false,
            parkOutside = true,
        ),
    )

    val tacomaFuelRecords = listOf(
        FuelRecord(
            id = 1,
            vehicleId = tacoma.id,
            date = now - (2 * day),
            mileage = 15230,
            gallons = 17.2,
            totalCost = 62.48,
            pricePerGallon = 62.48 / 17.2,
            isFullTank = true,
        ),
        FuelRecord(
            id = 2,
            vehicleId = tacoma.id,
            date = now - (40 * day),
            mileage = 14890,
            gallons = 9.4,
            totalCost = 34.03,
            pricePerGallon = 34.03 / 9.4,
            isFullTank = false,
        ),
        FuelRecord(
            id = 3,
            vehicleId = tacoma.id,
            date = now - (48 * day),
            mileage = 14610,
            gallons = 16.8,
            totalCost = 60.79,
            pricePerGallon = 60.79 / 16.8,
            isFullTank = true,
        ),
        FuelRecord(
            id = 4,
            vehicleId = tacoma.id,
            date = now - (78 * day),
            mileage = 14180,
            gallons = 17.0,
            totalCost = 61.63,
            pricePerGallon = 61.63 / 17.0,
            isFullTank = true,
        ),
    )

    private val tacomaFuelEconomyEntries = FuelEconomyCalculator.entriesFor(tacomaFuelRecords)

    val tacomaFuelMpgByRecordId = tacomaFuelEconomyEntries.associate { it.record.id to it.mpg }

    val tacomaAverageMpg = FuelEconomyCalculator.averageMpg(tacomaFuelEconomyEntries)
}
