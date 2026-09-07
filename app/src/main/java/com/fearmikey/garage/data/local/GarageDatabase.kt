package com.fearmikey.garage.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehicleSpecs

/** The filename used on disk; referenced by BackupRepository during export/import. */
const val GARAGE_DATABASE_NAME = "garage.db"

/**
 * The current Room schema version, kept as a standalone constant (rather than
 * only living inside the [Database] annotation below) so it can be compared
 * against a backup's on-disk schema version before restoring it (see
 * `BackupRepository.importBackup`). Without that check, restoring a backup
 * taken with a newer app build (higher schema version) onto an older
 * installed build downgrades the live database, which Room cannot open --
 * crashing the app on every launch with no way to recover other than
 * clearing app data again.
 */
const val GARAGE_DATABASE_VERSION = 4

@Database(
    entities = [Vehicle::class, MaintenanceRecord::class, Reminder::class, VehicleSpecs::class],
    version = GARAGE_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun vehicleSpecsDao(): VehicleSpecsDao
}
