package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.ReminderEntity

@Dao
interface ReminderDao {
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'SCHEDULED' ORDER BY scheduledAt ASC")
    fun observeScheduledReminders(): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders
        WHERE status = 'SCHEDULED'
        AND scheduledAt BETWEEN :dayStart AND :dayEnd
        ORDER BY scheduledAt ASC
    """)
    fun observeTodayReminders(dayStart: Long, dayEnd: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'SCHEDULED' AND scheduledAt < :now ORDER BY scheduledAt ASC")
    fun observeOverdueReminders(now: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'SCHEDULED' AND scheduledAt <= :now ORDER BY scheduledAt ASC")
    suspend fun getDueReminders(now: Long): List<ReminderEntity>

    @Query("UPDATE reminders SET status = 'FIRED', firedAt = :now, updatedAt = :now WHERE id = :reminderId")
    suspend fun markFired(reminderId: Long, now: Long)

    @Query("UPDATE reminders SET status = 'MISSED', updatedAt = :now WHERE id = :reminderId")
    suspend fun markMissed(reminderId: Long, now: Long)

    @Query("UPDATE reminders SET status = 'CANCELLED', cancelledAt = :now, updatedAt = :now WHERE id = :reminderId")
    suspend fun cancelReminder(reminderId: Long, now: Long)

    @Query("UPDATE reminders SET scheduledAt = :newScheduledAt, status = 'SCHEDULED', updatedAt = :now WHERE id = :reminderId")
    suspend fun rescheduleReminder(reminderId: Long, newScheduledAt: Long, now: Long)

    @Query("UPDATE reminders SET schedulerState = :schedulerState, updatedAt = :now WHERE id = :reminderId")
    suspend fun updateSchedulerState(reminderId: Long, schedulerState: String, now: Long)

    @Query("SELECT * FROM reminders WHERE radarCardId = :cardId ORDER BY createdAt DESC")
    suspend fun getRemindersForCard(cardId: Long): List<ReminderEntity>
}
