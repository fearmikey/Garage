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
        missedFuelUp = "false" // We don't explicitly track missed fuel ups right now
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
