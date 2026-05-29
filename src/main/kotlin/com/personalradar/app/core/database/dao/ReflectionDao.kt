package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.ReflectionLogEntity

@Dao
interface ReflectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReflectionLog(log: ReflectionLogEntity)

    @Query("SELECT * FROM reflection_logs WHERE date = :date LIMIT 1")
    suspend fun getReflectionLogByDate(date: String): ReflectionLogEntity?

    @Query("SELECT * FROM reflection_logs ORDER BY date DESC")
    fun observeReflectionLogs(): Flow<List<ReflectionLogEntity>>

    @Query("SELECT * FROM reflection_logs ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentReflectionLogs(limit: Int): List<ReflectionLogEntity>
}
