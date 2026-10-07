package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
// LubeLogger might return ISO dates or partial strings.
// A more robust format parser may be required depending on server region config,
// but for exporting, "yyyy-MM-dd" is acceptable.

fun FuelRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int): LubeLoggerGasRecordDto {
    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        id = this.lubeLoggerId,
        date = dateFormatter.format(Date(this.date)),
        odometer = this.mileage.toString(),
        fuelConsumed = this.gallons.toString(),
        cost = this.totalCost.toString(),
        isFillToFull = this.isFullTank.toString(),
        missedFuelUp = "false", // We don't explicitly track missed fuel ups right now
    )
}

fun MaintenanceRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int): LubeLoggerServiceRecordDto {
    val fullDescription = buildString {
        append(description)
        if (taskName != null) {
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
        odometer = this.mileage.toString(),
        description = fullDescription,
        cost = this.cost.toString()
    )
}

fun LubeLoggerGasRecordDto.toFuelRecord(localVehicleId: Long): FuelRecord? {
    val recordId = id ?: return null
    val parsedDate = parseDateToEpochMillis(date)
    val parsedMileage = mileage ?: odometer?.toIntOrNull() ?: 0
    val parsedGallons = gallons ?: fuelConsumed?.toDoubleOrNull() ?: 0.0
    val parsedCost = cost?.toDoubleOrNull() ?: 0.0
    val parsedIsFillToFull = isFillToFull?.toBooleanStrictOrNull() ?: true
    val ppg = if (parsedGallons > 0.0) parsedCost / parsedGallons else 0.0

    return FuelRecord(
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        gallons = parsedGallons,
        totalCost = parsedCost,
        pricePerGallon = ppg,
        isFullTank = parsedIsFillToFull,
        lubeLoggerId = recordId
    )
}

fun LubeLoggerServiceRecordDto.toMaintenanceRecord(localVehicleId: Long): MaintenanceRecord? {
    val recordId = id ?: return null
    val parsedDate = parseDateToEpochMillis(date)
    val parsedMileage = mileage ?: odometer?.toIntOrNull() ?: 0
    val parsedCost = cost?.toDoubleOrNull() ?: 0.0

    return MaintenanceRecord(
        vehicleId = localVehicleId,
        date = parsedDate,
        mileage = parsedMileage,
        description = description,
        cost = parsedCost,
        category = com.fearmikey.garage.data.local.entity.MaintenanceCategory.OTHER,
        lubeLoggerId = recordId
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
