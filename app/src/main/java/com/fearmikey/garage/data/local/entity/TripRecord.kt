package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TripCategory {
    BUSINESS, PERSONAL, COMMUTE, MEDICAL, CHARITY, OTHER
}

@Entity(
    tableName = "trip_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class TripRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val startDate: Long,
    val endDate: Long,
    val startOdometer: Int,
    val endOdometer: Int,
    val calculatedDistance: Double,
    val category: TripCategory,
    val reimbursementRate: Double,
    val tollExpense: Double,
    val parkingExpense: Double,
    val notes: String,
    val isReimbursed: Boolean
)
