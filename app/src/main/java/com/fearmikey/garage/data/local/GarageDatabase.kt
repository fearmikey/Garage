package com.fearmikey.garage.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.fearmikey.garage.data.local.dao.CustomMaintenanceRuleDao
import com.fearmikey.garage.data.local.dao.FuelDao
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ModificationDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehiclePartsDao
import com.fearmikey.garage.data.local.dao.VehicleRegistrationDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import com.fearmikey.garage.data.local.entity.CustomMaintenanceRule
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleRegistrationInsurance
import com.fearmikey.garage.data.local.entity.VehicleSpecs

/** The filename used on disk; referenced by BackupRepository during export/import. */
const val GARAGE_DATABASE_NAME = "garage.db"

/**
 * The current Room schema version, kept as a standalone constant so it can be
 * compared against a backup's on-disk schema version before restoring it.
 */
const val GARAGE_DATABASE_VERSION = 16

@DeleteTable.Entries(value = [DeleteTable(tableName = "reminders")])
class DeleteRemindersTableSpec : AutoMigrationSpec

@Database(
    entities = [
        Vehicle::class, MaintenanceRecord::class, VehicleSpecs::class, FuelRecord::class,
        VehiclePartsInfo::class, CustomMaintenanceRule::class, ModificationRecord::class, VehicleRegistrationInsurance::class,
    ],
    version = GARAGE_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        // Additive: receiptUri added to maintenance_records table.
        AutoMigration(from = 11, to = 12),
        // Removed reminders table.
        AutoMigration(from = 12, to = 13, spec = DeleteRemindersTableSpec::class),
        // Additive: productUrl added to modification_records table.
        AutoMigration(from = 13, to = 14),
        // Additive: imageUri2, imageOffsetY2, imageUri3, imageOffsetY3 added to vehicles table.
        AutoMigration(from = 14, to = 15),
        // Additive: imageUri2..6 added to modification_records table.
        AutoMigration(from = 15, to = 16),
    ],
)
@TypeConverters(Converters::class)
abstract class GarageDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun vehicleSpecsDao(): VehicleSpecsDao
    abstract fun fuelDao(): FuelDao
    abstract fun vehiclePartsDao(): VehiclePartsDao
    abstract fun customMaintenanceRuleDao(): CustomMaintenanceRuleDao
    abstract fun modificationDao(): ModificationDao
    abstract fun vehicleRegistrationDao(): VehicleRegistrationDao
}
