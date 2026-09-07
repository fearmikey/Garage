package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.fearmikey.garage.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY isCompleted ASC, dueDate ASC")
    fun getRemindersForVehicle(vehicleId: Long): Flow<List<Reminder>>

    /** Used by the daily WorkManager check across all vehicles. */
    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    suspend fun getIncompleteReminders(): List<Reminder>

    @Upsert
    suspend fun upsert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}
