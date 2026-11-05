package com.fearmikey.garage.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.data.schedule.MaintenanceScheduleRules
import com.fearmikey.garage.data.schedule.MaintenanceScheduleTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class MaintenanceCategorySplitSpecTest {

    private val split = MaintenanceCategorySplitSpec.TASK_CATEGORY_SPLIT

    /** Every built-in rule/template task (case-insensitive) -> its current category. */
    private val currentCategories: Map<String, MaintenanceCategory> =
        (MaintenanceScheduleRules.rules.map { it.taskName to it.category } +
            MaintenanceScheduleTemplates.templates.flatMap { t -> t.rules.map { it.taskName to it.category } })
            .associate { (task, category) -> task.lowercase() to category }

    @Test
    fun `migration targets only categories that were split from OTHER`() {
        split.keys.forEach { name ->
            val category = MaintenanceCategory.valueOf(name)
            assertTrue("$name is not a split-from-OTHER category", category in MaintenanceCategory.SPLIT_FROM_OTHER)
        }
    }

    @Test
    fun `migration mapping agrees with current rules and templates`() {
        split.forEach { (category, tasks) ->
            tasks.forEach { task ->
                assertEquals("Mapping for '$task'", category, currentCategories[task.lowercase()]?.name)
            }
        }
    }

    @Test
    fun `every built-in task in a split category is covered by the migration`() {
        val migrated = split.values.flatten().map { it.lowercase() }.toSet()
        currentCategories
            .filterValues { it in MaintenanceCategory.SPLIT_FROM_OTHER }
            .keys
            .forEach { task -> assertTrue("'$task' would stay under OTHER for old records", task in migrated) }
    }

    @Test
    fun `onPostMigrate updates OTHER rows in records and custom rules`() {
        val statements = mutableListOf<Pair<String, List<Any?>>>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args?.size == 2) {
                @Suppress("UNCHECKED_CAST")
                statements += (args[0] as String) to (args[1] as Array<Any?>).toList()
            }
            null
        } as SupportSQLiteDatabase

        MaintenanceCategorySplitSpec().onPostMigrate(db)

        assertEquals(split.size * 2, statements.size)
        assertTrue(statements.all { (sql, _) -> "category = 'OTHER'" in sql })
        assertEquals(setOf("maintenance_records", "custom_maintenance_rules"),
            statements.map { (sql, _) -> sql.substringAfter("UPDATE ").substringBefore(" ") }.toSet())

        val filters = statements.first { (sql, args) -> "maintenance_records" in sql && args.first() == "FILTERS" }
        assertTrue("engine air filter replacement" in filters.second)
        // One bind arg per placeholder: the category plus each task name.
        assertEquals(filters.second.size, filters.first.count { it == '?' })
    }
}
