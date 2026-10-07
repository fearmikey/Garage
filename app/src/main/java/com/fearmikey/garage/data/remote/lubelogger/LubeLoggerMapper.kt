package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.ui.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun FuelRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerGasRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage
    val convertedGallons = if (isMetric) UnitConverter.gallonsToLiters(this.gallons) else this.gallons

    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = dateFormatter.format(Date(this.date)),
        odometer = convertedOdometer.toString(),
        fuelConsumed = "%.3f".format(Locale.US, convertedGallons),
        cost = "%.2f".format(Locale.US, this.totalCost),
        isFillToFull = this.isFullTank.toString(),
        missedFuelUp = "false", // We don't explicitly track missed fuel ups right now
    )
}

fun MaintenanceRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerServiceRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    val fullDescription = buildString {
        append(description)
        if (taskName != null && taskName != "Odometer Check-in") {
            append(" ($taskName)")
        }
        if (isDeferred) {
            append(" [Deferred]")
        }
    }
    
    return LubeLoggerServiceRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = dateFormatter.format(Date(this.date)),
        odometer = convertedOdometer.toString(),
        description = fullDescription,
        cost = "%.2f".format(Locale.US, this.cost),
    )
}

fun MaintenanceRecord.toLubeLoggerOdometerDto(lubeLoggerVehicleId: Int, lubeLoggerUnitSystem: String = "imperial"): LubeLoggerOdometerRecordDto {
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val convertedOdometer = if (isMetric) UnitConverter.milesToKm(this.mileage) else this.mileage

    return LubeLoggerOdometerRecordDto(
        vehicleId = lubeLoggerVehicleId.toString(),
        id = this.lubeLoggerId?.toString() ?: "0",
        date = dateFormatter.format(Date(this.date)),
        odometer = convertedOdometer.toString(),
        notes = this.description.takeIf { it != "Odometer check-in" } ?: ""
    )
}

fun LubeLoggerGasRecordDto.toFuelRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial"): FuelRecord? {
    val recordId = id ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val parsedDate = parseDateToEpochMillis(date)
    
    val rawMileage = mileage ?: odometer?.toIntOrNull() ?: 0
    val rawGallons = gallons ?: fuelConsumed?.toDoubleOrNull() ?: 0.0
    val parsedCost = cost?.toDoubleOrNull() ?: 0.0
    val parsedIsFillToFull = isFillToFull?.toBooleanStrictOrNull() ?: true

    val parsedMileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage
    val parsedGallons = if (isMetric) UnitConverter.litersToGallons(rawGallons) else rawGallons
    val ppg = if (parsedGallons > 0.0) parsedCost / parsedGallons else 0.0

    return FuelRecord(
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        gallons = parsedGallons,
        totalCost = parsedCost,
        pricePerGallon = ppg,
        isFullTank = parsedIsFillToFull,
        lubeLoggerId = recordId,
    )
}

fun LubeLoggerServiceRecordDto.toMaintenanceRecord(localVehicleId: Long, lubeLoggerUnitSystem: String = "imperial"): MaintenanceRecord? {
    val recordId = id ?: return null
    val isMetric = lubeLoggerUnitSystem.equals("metric", ignoreCase = true)
    val parsedDate = parseDateToEpochMillis(date)
    
    val rawMileage = mileage ?: odometer?.toIntOrNull() ?: 0
    val parsedCost = cost?.toDoubleOrNull() ?: 0.0

    val parsedMileage = if (isMetric) UnitConverter.kmToMiles(rawMileage) else rawMileage

    return MaintenanceRecord(
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        description = description,
        cost = parsedCost,
        category = com.fearmikey.garage.data.local.entity.MaintenanceCategory.OTHER,
        lubeLoggerId = recordId,
    )
}

private fun parseDateToEpochMillis(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
