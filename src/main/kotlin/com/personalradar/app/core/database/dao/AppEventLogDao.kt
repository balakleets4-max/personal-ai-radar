package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.AppEventLogEntity

@Dao
interface AppEventLogDao {
    @Insert
    suspend fun insertEvent(event: AppEventLogEntity): Long

    @Query("SELECT * FROM app_event_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<AppEventLogEntity>

    @Query("SELECT * FROM app_event_logs WHERE level = :level ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getEventsByLevel(level: String, limit: Int): List<AppEventLogEntity>

    @Query("DELETE FROM app_event_logs WHERE createdAt < :before")
    suspend fun deleteOldEvents(before: Long)
}
