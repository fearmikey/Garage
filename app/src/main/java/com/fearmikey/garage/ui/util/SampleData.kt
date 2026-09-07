package com.fearmikey.garage.ui.util

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.schedule.MaintenanceScheduleEngine
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

    private val now = System.currentTimeMillis()
    private val day = TimeUnit.DAYS.toMillis(1)

    val tacomaMaintenanceRecords = listOf(
        MaintenanceRecord(
            id = 1,
            vehicleId = tacoma.id,
            date = now - (10 * day),
            mileage = 15230,
            description = "Full synthetic oil change + filter",
            cost = 89.99,
            category = MaintenanceCategory.OIL_CHANGE,
            taskName = "Oil & filter change",
        ),
        MaintenanceRecord(
            id = 2,
            vehicleId = tacoma.id,
            date = now - (95 * day),
            mileage = 12100,
            description = "Rotated tires, checked tread depth",
            cost = 40.0,
            category = MaintenanceCategory.TIRE_ROTATION,
            taskName = "Tire rotation",
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
            taskName = "Oil change",
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

    val tacomaMaintenanceSuggestions = MaintenanceScheduleEngine.suggestionsFor(
        vehicle = tacoma,
        latestMileage = TACOMA_LATEST_MILEAGE,
        records = tacomaMaintenanceRecords,
    )
}
