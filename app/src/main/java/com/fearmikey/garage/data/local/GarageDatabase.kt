package com.fearmikey.garage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle

/** The filename used on disk; referenced by BackupRepository during export/import. */
const val GARAGE_DATABASE_NAME = "garage.db"

@Database(
    entities = [Vehicle::class, MaintenanceRecord::class, Reminder::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
}
