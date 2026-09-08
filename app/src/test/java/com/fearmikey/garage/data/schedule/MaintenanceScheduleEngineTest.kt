package com.fearmikey.garage.data.schedule

import com.fearmikey.garage.data.local.entity.Drivetrain
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.repository.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceScheduleEngineTest {

    private val fwdCivic = Vehicle(
        id = 1,
        make = "Honda",
        model = "Civic",
        drivetrain = Drivetrain.FWD,
    )

    private val fourWdTacoma = Vehicle(
        id = 2,
        make = "Toyota",
        model = "Tacoma",
        drivetrain = Drivetrain.FOUR_WD,
    )

    private val fourWdF150 = Vehicle(
        id = 3,
        make = "Ford",
        model = "F-150",
        drivetrain = Drivetrain.FOUR_WD,
    )

    @Test
    fun `generic rules apply to any vehicle`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 0, records = emptyList())

        assertTrue(suggestions.any { it.rule.taskName == "Engine oil change" })
        assertTrue(suggestions.any { it.rule.taskName == "Rotation and Balance" })
    }

    @Test
    fun `drivetrain-only rules are excluded for a vehicle without that drivetrain`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 0, records = emptyList())

        assertTrue(suggestions.none { it.rule.taskName == "Transfer case fluid change" })
    }

    @Test
    fun `make and model specific rules only apply to matching vehicles`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fourWdTacoma, latestMileage = 0, records = emptyList())

        assertTrue(suggestions.any { it.rule.taskName == "Front differential fluid change" })
        assertTrue(suggestions.any { it.rule.taskName == "Rear differential fluid change" })
    }

    @Test
    fun `front differential rule does not apply to a non-Tacoma 4WD truck`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fourWdF150, latestMileage = 0, records = emptyList())

        assertTrue(suggestions.none { it.rule.taskName == "Front differential fluid change" })
    }

    @Test
    fun `most specific rule wins when a generic and make-specific rule share a task name`() {
        val tacomaSuggestions = MaintenanceScheduleEngine.suggestionsFor(fourWdTacoma, latestMileage = 0, records = emptyList())
        val f150Suggestions = MaintenanceScheduleEngine.suggestionsFor(fourWdF150, latestMileage = 0, records = emptyList())

        // Both get exactly one "Transfer case fluid change" suggestion (no duplicates)...
        assertEquals(1, tacomaSuggestions.count { it.rule.taskName == "Transfer case fluid change" })
        assertEquals(1, f150Suggestions.count { it.rule.taskName == "Transfer case fluid change" })

        // ...but the Tacoma gets the make/model-specific rule, and the F-150 the generic fallback.
        val tacomaRule = tacomaSuggestions.first { it.rule.taskName == "Transfer case fluid change" }.rule
        val f150Rule = f150Suggestions.first { it.rule.taskName == "Transfer case fluid change" }.rule
        assertEquals("Toyota", tacomaRule.makeMatch)
        assertNull(f150Rule.makeMatch)
    }

    @Test
    fun `baseline mileage comes from the most recent matching service record`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 20_000,
                description = "Engine oil change",
                cost = 50.0,
                category = MaintenanceCategory.FLUIDS,
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 20_200, records = records)
        val oilChange = suggestions.first { it.rule.taskName == "Engine oil change" }

        assertEquals(20_000, oilChange.lastServiceMileage)
        assertEquals(25_000, oilChange.nextDueMileage)
    }

    @Test
    fun `suggestion is upcoming when within the upcoming mileage window`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 20_000,
                description = "Engine oil change",
                cost = 50.0,
                category = MaintenanceCategory.FLUIDS,
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 24_700, records = records)
        val oilChange = suggestions.first { it.rule.taskName == "Engine oil change" }

        assertEquals(25_000, oilChange.nextDueMileage)
        assertEquals(ReminderStatus.UPCOMING, oilChange.status)
    }

    @Test
    fun `next due mileage rolls forward past multiple missed intervals for a never-serviced task`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 47_000, records = emptyList())
        val oilChange = suggestions.first { it.rule.taskName == "Engine oil change" }

        assertNull(oilChange.lastServiceMileage)
        // Interval is 5,000mi: 5k, 10k, ..., 45k are all <= 47k, so the next unpassed multiple is 50k.
        assertEquals(50_000, oilChange.nextDueMileage)
        assertEquals(ReminderStatus.OK, oilChange.status)
    }

    @Test
    fun `status is OK when latest mileage is unknown`() {
        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = null, records = emptyList())
        val oilChange = suggestions.first { it.rule.taskName == "Engine oil change" }

        assertEquals(5_000, oilChange.nextDueMileage)
        assertEquals(ReminderStatus.OK, oilChange.status)
    }

    @Test
    fun `logging one task-tagged fluids record does not satisfy a different fluids task`() {
        // Regression test for a real bug report: logging a brake fluid flush (category FLUIDS)
        // must not be mistaken for a coolant flush, ATF service, etc. just because they share a
        // category -- each needs its own taskName to be tracked independently.
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fourWdTacoma.id,
                date = 0,
                mileage = 20_000,
                description = "Brake fluid flush",
                cost = 100.0,
                category = MaintenanceCategory.FLUIDS,
                taskName = "Brake fluid flush",
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fourWdTacoma, latestMileage = 20_100, records = records)

        val brakeFluid = suggestions.first { it.rule.taskName == "Brake fluid flush" }
        assertEquals(20_000, brakeFluid.lastServiceMileage)

        val coolant = suggestions.first { it.rule.taskName == "Coolant flush" }
        assertNull(coolant.lastServiceMileage)

        val atf = suggestions.first { it.rule.taskName == "Automatic transmission fluid service" }
        assertNull(atf.lastServiceMileage)
    }

    @Test
    fun `a taskName match is honored even for records whose free-text description differs`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 30_000,
                description = "Flushed the coolant system at Joe's Garage",
                cost = 120.0,
                category = MaintenanceCategory.FLUIDS,
                taskName = "Coolant flush",
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 30_100, records = records)
        val coolant = suggestions.first { it.rule.taskName == "Coolant flush" }

        assertEquals(30_000, coolant.lastServiceMileage)
    }

    @Test
    fun `untagged custom other record matches matching keywords but not unrelated tasks`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 15_000,
                description = "Replaced cabin air filter",
                cost = 30.0,
                category = MaintenanceCategory.OTHER,
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 15_100, records = records)

        val cabinFilter = suggestions.first { it.rule.taskName == "Cabin air filter replacement" }
        val engineFilter = suggestions.first { it.rule.taskName == "Engine air filter replacement" }
        val sparkPlugs = suggestions.first { it.rule.taskName == "Spark plug replacement" }

        assertEquals(15_000, cabinFilter.lastServiceMileage)
        assertNull(engineFilter.lastServiceMileage)
        assertNull(sparkPlugs.lastServiceMileage)
    }

    @Test
    fun `custom other entry like daytime running lights does not satisfy spark plug replacement`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 63_285,
                description = "Replacing daytime running lights",
                cost = 25.0,
                category = MaintenanceCategory.OTHER,
                taskName = null,
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 63_300, records = records)

        val sparkPlugs = suggestions.first { it.rule.taskName == "Spark plug replacement" }
        assertNull(sparkPlugs.lastServiceMileage)

        val cabinFilter = suggestions.first { it.rule.taskName == "Cabin air filter replacement" }
        assertNull(cabinFilter.lastServiceMileage)
    }

    @Test
    fun `logging rotation satisfies rotation and balance rule`() {
        val records = listOf(
            MaintenanceRecord(
                vehicleId = fwdCivic.id,
                date = 0,
                mileage = 12_000,
                description = "Tire rotation",
                cost = 40.0,
                category = MaintenanceCategory.TIRES,
                taskName = "Rotation",
            )
        )

        val suggestions = MaintenanceScheduleEngine.suggestionsFor(fwdCivic, latestMileage = 12_100, records = records)

        val rotationAndBalance = suggestions.first { it.rule.taskName == "Rotation and Balance" }

        assertEquals(12_000, rotationAndBalance.lastServiceMileage)
        assertEquals(18_000, rotationAndBalance.nextDueMileage)
        assertEquals(ReminderStatus.OK, rotationAndBalance.status)
    }
}
