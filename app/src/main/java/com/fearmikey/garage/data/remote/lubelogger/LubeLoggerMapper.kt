package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun FuelRecord.toLubeLoggerDto(lubeLoggerVehicleId: Int): LubeLoggerGasRecordDto {
    return LubeLoggerGasRecordDto(
        vehicleId = lubeLoggerVehicleId,
        date = dateFormatter.format(Date(this.date)),
        mileage = this.mileage,
        gallons = this.gallons,
        cost = this.totalCost,
        isFillToFull = this.isFullTank,
        missedFuelUp = false // We don't explicitly track missed fuel ups right now
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
        date = dateFormatter.format(Date(this.date)),
        mileage = this.mileage,
        description = fullDescription,
        cost = this.cost
    )
}
