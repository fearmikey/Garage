package com.fearmikey.garage.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Reminder
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
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
const val GARAGE_DATABASE_VERSION = 7

@Database(
    entities = [
        Vehicle::class, MaintenanceRecord::class, Reminder::class, VehicleSpecs::class, FuelRecord::class,
        VehiclePartsInfo::class, CustomMaintenanceRule::class,
    ],
    version = GARAGE_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        // Purely additive: two new tables (vehicle_parts_info,
        // custom_maintenance_rules), no changes to existing tables.
        AutoMigration(from = 5, to = 6),
        // Additive: imageOffsetY added to vehicles table.
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(Converters::class)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun vehicleSpecsDao(): VehicleSpecsDao
    abstract fun fuelDao(): FuelDao
    abstract fun vehiclePartsDao(): VehiclePartsDao
    abstract fun customMaintenanceRuleDao(): CustomMaintenanceRuleDao
}
