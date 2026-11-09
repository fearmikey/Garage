package com.fearmikey.garage.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the v33 -> v34 auto-migration (mileage/odometer columns INTEGER -> REAL). Room
 * implements the type change by recreating tables, including `vehicles`, which every record
 * references with ON DELETE CASCADE, so this guards against the migration wiping child rows.
 *
 * The v33 schema statements below are copied from app/schemas/.../33.json.
 */
@RunWith(AndroidJUnit4::class)
class MileageRealMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test-33-34.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null).use { db ->
            V33_SCHEMA.forEach(db::execSQL)
            V33_DATA.forEach(db::execSQL)
            db.version = 33
        }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrationKeepsAllRowsAndConvertsMileageToReal() {
        val room = Room.databaseBuilder(context, GarageDatabase::class.java, dbName).build()
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(GARAGE_DATABASE_VERSION, db.version)

            for (table in listOf(
                "vehicles", "maintenance_records", "fuel_records", "charging_records",
                "custom_maintenance_rules", "trip_records", "vehicle_specs",
            )) {
                assertEquals("rows in $table", 1L, db.longFor("SELECT COUNT(*) FROM $table"))
            }

            assertEquals("real", db.stringFor("SELECT typeof(initialMileage) FROM vehicles"))
            assertEquals(2237.0, db.doubleFor("SELECT initialMileage FROM vehicles"), 0.0)
            assertEquals(2237.0, db.doubleFor("SELECT mileage FROM maintenance_records"), 0.0)
            assertEquals(500.0, db.doubleFor("SELECT deferredMiles FROM maintenance_records"), 0.0)
            assertEquals(2300.0, db.doubleFor("SELECT mileage FROM fuel_records"), 0.0)
            assertEquals(2400.0, db.doubleFor("SELECT mileage FROM charging_records"), 0.0)
            assertEquals(6214.0, db.doubleFor("SELECT intervalMiles FROM custom_maintenance_rules"), 0.0)

            // New rows keep fractional miles (e.g. 3599 km) exactly.
            db.execSQL("UPDATE vehicles SET initialMileage = ?", arrayOf<Any>(3599 / 1.609344))
            assertEquals(3599.0, db.doubleFor("SELECT initialMileage FROM vehicles") * 1.609344, 1e-9)

            assertEquals("foreign key violations", 0L, db.longFor("SELECT COUNT(*) FROM pragma_foreign_key_check"))
        } finally {
            room.close()
        }
    }

    private fun SupportSQLiteDatabase.longFor(sql: String): Long = query(sql).use { it.moveToFirst(); it.getLong(0) }
    private fun SupportSQLiteDatabase.doubleFor(sql: String): Double = query(sql).use { it.moveToFirst(); it.getDouble(0) }
    private fun SupportSQLiteDatabase.stringFor(sql: String): String = query(sql).use { it.moveToFirst(); it.getString(0) }

    private companion object {
        val V33_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vin` TEXT NOT NULL, `year` INTEGER, `make` TEXT NOT NULL, `model` TEXT NOT NULL, `trim` TEXT NOT NULL, `imageUri` TEXT, `drivetrain` TEXT NOT NULL DEFAULT 'UNKNOWN', `imageOffsetY` REAL NOT NULL DEFAULT 0.0, `imageUri2` TEXT, `imageOffsetY2` REAL NOT NULL DEFAULT 0.0, `imageUri3` TEXT, `imageOffsetY3` REAL NOT NULL DEFAULT 0.0, `purchasedNew` INTEGER NOT NULL DEFAULT 0, `initialMileage` INTEGER, `themeColor` INTEGER, `vehicleType` TEXT NOT NULL DEFAULT 'CAR', `hiddenTabs` TEXT NOT NULL DEFAULT '')",
            "CREATE TABLE IF NOT EXISTS `maintenance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `mileage` INTEGER NOT NULL, `description` TEXT NOT NULL, `cost` REAL NOT NULL, `category` TEXT NOT NULL, `taskName` TEXT, `receiptUri` TEXT, `isDeferred` INTEGER NOT NULL DEFAULT 0, `deferredMiles` INTEGER, `deferredMonths` INTEGER, `lubeLoggerId` INTEGER DEFAULT NULL, `lubeLoggerSyncHash` TEXT DEFAULT NULL, `lubeLoggerRecordType` TEXT DEFAULT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId` ON `maintenance_records` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `vehicle_specs` (`vehicleId` INTEGER NOT NULL, `engineCylinders` TEXT, `displacementL` TEXT, `engineHp` TEXT, `fuelType` TEXT, `fuelTypeSecondary` TEXT, `electrificationLevel` TEXT, `transmissionStyle` TEXT, `transmissionSpeeds` TEXT, `bodyClass` TEXT, `doors` TEXT, `gvwr` TEXT, `series` TEXT, `vehicleType` TEXT, `plantCity` TEXT, `plantState` TEXT, `plantCountry` TEXT, `manufacturer` TEXT, `trailerBrakedCapacity` TEXT, `trailerUnbrakedCapacity` TEXT, `gcwr` TEXT, `curbWeight` TEXT, `customSpecs` TEXT NOT NULL DEFAULT '[]', PRIMARY KEY(`vehicleId`), FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `fuel_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `mileage` INTEGER NOT NULL, `gallons` REAL NOT NULL, `totalCost` REAL NOT NULL, `pricePerGallon` REAL NOT NULL, `isFullTank` INTEGER NOT NULL, `lubeLoggerId` INTEGER DEFAULT NULL, `lubeLoggerSyncHash` TEXT DEFAULT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_fuel_records_vehicleId` ON `fuel_records` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `vehicle_parts_info` (`vehicleId` INTEGER NOT NULL, `oilViscosity` TEXT, `oilCapacity` TEXT, `oilFilterPartNumber` TEXT, `sparkPlugPartNumber` TEXT, `sparkPlugGap` TEXT, `engineAirFilterPartNumber` TEXT, `cabinAirFilterPartNumber` TEXT, `tireSizeFront` TEXT, `tireSizeRear` TEXT, `tirePsiFront` TEXT, `tirePsiRear` TEXT, `wiperBladeSizeDriver` TEXT, `wiperBladeSizePassenger` TEXT, `wiperBladeSizeRear` TEXT, PRIMARY KEY(`vehicleId`), FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `custom_maintenance_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `taskName` TEXT NOT NULL, `category` TEXT NOT NULL, `intervalMiles` INTEGER, `intervalMonths` INTEGER, `isDeferrable` INTEGER NOT NULL DEFAULT 0, `notes` TEXT, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_custom_maintenance_rules_vehicleId` ON `custom_maintenance_rules` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `modification_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `imageUri` TEXT, `date` INTEGER NOT NULL, `cost` REAL NOT NULL, `productUrl` TEXT NOT NULL DEFAULT '', `imageUri2` TEXT, `imageUri3` TEXT, `imageUri4` TEXT, `imageUri5` TEXT, `imageUri6` TEXT, `lubeLoggerId` INTEGER DEFAULT NULL, `lubeLoggerSyncHash` TEXT DEFAULT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_modification_records_vehicleId` ON `modification_records` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `vehicle_registration_insurance` (`vehicleId` INTEGER NOT NULL, `licensePlate` TEXT, `registrationState` TEXT, `registrationExpiration` INTEGER, `registrationFee` REAL, `registrationNotes` TEXT, `registrationImageUri` TEXT, `registrationImageUri2` TEXT, `inspectionExpiration` INTEGER, `inspectionDate` INTEGER, `inspectionResult` TEXT, `inspectionNotes` TEXT, `emissionsExpiration` INTEGER, `emissionsDate` INTEGER, `emissionsResult` TEXT, `emissionsNotes` TEXT, `inspectionStickerExpiration` INTEGER, `inspectionStickerNumber` TEXT, `inspectionStickerNotes` TEXT, `insuranceProvider` TEXT, `policyNumber` TEXT, `insuranceExpiration` INTEGER, `insurancePremium` REAL, `insuranceAgentContact` TEXT, `insuranceNotes` TEXT, `insuranceImageUri` TEXT, `tollPassParkingExpiration` INTEGER, `tollPassParkingAccount` TEXT, `tollPassParkingNotes` TEXT, `driversLicenseExpiration` INTEGER, `driversLicenseNumber` TEXT, `driversLicenseState` TEXT, `driversLicenseNotes` TEXT, `purchasePrice` REAL, `purchaseDate` INTEGER, `purchaseSeller` TEXT, `purchaseNotes` TEXT, `purchaseImageUri` TEXT, PRIMARY KEY(`vehicleId`), FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `charging_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `mileage` INTEGER NOT NULL, `kwhAdded` REAL NOT NULL, `chargerSpeed` TEXT NOT NULL, `batteryPercentStart` INTEGER NOT NULL, `batteryPercentEnd` INTEGER NOT NULL, `totalCost` REAL NOT NULL, `vendor` TEXT NOT NULL, `vendorType` TEXT NOT NULL, `estimatedRangeAt100` INTEGER, `lubeLoggerId` INTEGER DEFAULT NULL, `lubeLoggerSyncHash` TEXT DEFAULT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_charging_records_vehicleId` ON `charging_records` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `ignored_maintenance_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `taskName` TEXT NOT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_ignored_maintenance_rules_vehicleId_taskName` ON `ignored_maintenance_rules` (`vehicleId`, `taskName`)",
            "CREATE TABLE IF NOT EXISTS `recall_campaign_states` (`vehicleId` INTEGER NOT NULL, `campaignNumber` TEXT NOT NULL, `state` TEXT NOT NULL, PRIMARY KEY(`vehicleId`, `campaignNumber`), FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_recall_campaign_states_vehicleId` ON `recall_campaign_states` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `lubelogger_pending_deletes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `lubeLoggerId` INTEGER NOT NULL, `lubeLoggerVehicleId` INTEGER NOT NULL, FOREIGN KEY(`lubeLoggerVehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_lubelogger_pending_deletes_lubeLoggerVehicleId` ON `lubelogger_pending_deletes` (`lubeLoggerVehicleId`)",
            "CREATE TABLE IF NOT EXISTS `torque_specs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `component` TEXT NOT NULL, `torqueNm` REAL, `torqueFtLbs` REAL, `notes` TEXT NOT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_torque_specs_vehicleId` ON `torque_specs` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `vehicle_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_vehicle_notes_vehicleId` ON `vehicle_notes` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `trip_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `startOdometer` INTEGER NOT NULL, `endOdometer` INTEGER NOT NULL, `calculatedDistance` REAL NOT NULL, `category` TEXT NOT NULL, `reimbursementRate` REAL NOT NULL, `tollExpense` REAL NOT NULL, `parkingExpense` REAL NOT NULL, `notes` TEXT NOT NULL, `isReimbursed` INTEGER NOT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_trip_records_vehicleId` ON `trip_records` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS `custom_documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `title` TEXT NOT NULL, `referenceNumber` TEXT, `expiration` INTEGER, `cost` REAL, `notes` TEXT, `imageUri` TEXT, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_custom_documents_vehicleId` ON `custom_documents` (`vehicleId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9dcf24016ebcbf188849030cceab3b14')",
        )

        val V33_DATA = listOf(
            "INSERT INTO `vehicles` (`id`,`vin`,`make`,`model`,`trim`,`drivetrain`,`imageOffsetY`,`imageOffsetY2`,`imageOffsetY3`,`purchasedNew`,`initialMileage`,`vehicleType`,`hiddenTabs`) VALUES (1,'x','x','x','x','FWD',1.0,1.0,1.0,1,2237,'CAR','x')",
            "INSERT INTO `maintenance_records` (`id`,`vehicleId`,`date`,`mileage`,`description`,`cost`,`category`,`isDeferred`,`deferredMiles`) VALUES (10,1,1,2237,'x',1.0,'OTHER',1,500)",
            "INSERT INTO `fuel_records` (`id`,`vehicleId`,`date`,`mileage`,`gallons`,`totalCost`,`pricePerGallon`,`isFullTank`) VALUES (20,1,1,2300,1.0,1.0,1.0,1)",
            "INSERT INTO `charging_records` (`id`,`vehicleId`,`date`,`mileage`,`kwhAdded`,`chargerSpeed`,`batteryPercentStart`,`batteryPercentEnd`,`totalCost`,`vendor`,`vendorType`,`estimatedRangeAt100`) VALUES (30,1,1,2400,1.0,'x',1,1,1.0,'x','x',250)",
            "INSERT INTO `custom_maintenance_rules` (`id`,`vehicleId`,`taskName`,`category`,`intervalMiles`,`isDeferrable`) VALUES (40,1,'x','OTHER',6214,1)",
            "INSERT INTO `trip_records` (`id`,`vehicleId`,`startDate`,`endDate`,`startOdometer`,`endOdometer`,`calculatedDistance`,`category`,`reimbursementRate`,`tollExpense`,`parkingExpense`,`notes`,`isReimbursed`) VALUES (50,1,1,1,1,1,1.0,'x',1.0,1.0,1.0,'x',1)",
            "INSERT INTO `vehicle_specs` (`vehicleId`,`customSpecs`) VALUES (1,'[{\"label\":\"Chain\",\"value\":\"520\"}]')",
        )
    }
}
