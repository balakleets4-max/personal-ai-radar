package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.CaptureEntity

@Dao
interface CaptureDao {
    @Insert
    suspend fun insertCapture(capture: CaptureEntity): Long

    @Update
    suspend fun updateCapture(capture: CaptureEntity)

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getCaptureById(id: Long): CaptureEntity?

    @Query("SELECT * FROM captures WHERE status != 'DELETED' ORDER BY createdAt DESC")
    fun observeAllCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActiveCaptures(): Flow<List<CaptureEntity>>

    @Query("""
        SELECT * FROM captures
        WHERE createdAt BETWEEN :from AND :to
        AND status != 'DELETED'
        ORDER BY createdAt DESC
    """)
    fun observeCapturesBetween(from: Long, to: Long): Flow<List<CaptureEntity>>

    @Query("UPDATE captures SET status = 'ARCHIVED', updatedAt = :now WHERE id = :captureId")
    suspend fun archiveCapture(captureId: Long, now: Long)

    @Query("UPDATE captures SET status = 'DELETED', updatedAt = :now WHERE id = :captureId")
    suspend fun softDeleteCapture(captureId: Long, now: Long)

    @Query("UPDATE captures SET isFavorite = :isFavorite, updatedAt = :now WHERE id = :captureId")
    suspend fun setFavorite(captureId: Long, isFavorite: Boolean, now: Long)

    @Query("SELECT COUNT(*) FROM captures WHERE createdAt BETWEEN :from AND :to AND status != 'DELETED'")
    suspend fun countCapturesBetween(from: Long, to: Long): Int
}
