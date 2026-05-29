package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.AnalysisResultEntity

@Dao
interface AnalysisDao {
    @Insert
    suspend fun insertAnalysisResult(analysis: AnalysisResultEntity): Long

    @Update
    suspend fun updateAnalysisResult(analysis: AnalysisResultEntity)

    @Query("UPDATE analysis_results SET isLatest = 0 WHERE captureId = :captureId")
    suspend fun markAllAnalysisAsNotLatest(captureId: Long)

    @Query("""
        SELECT * FROM analysis_results
        WHERE captureId = :captureId
        AND isLatest = 1
        ORDER BY analyzedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestAnalysisForCapture(captureId: Long): AnalysisResultEntity?

    @Query("SELECT * FROM analysis_results WHERE id = :id LIMIT 1")
    suspend fun getAnalysisById(id: Long): AnalysisResultEntity?

    @Query("SELECT * FROM analysis_results WHERE mainIntent = :intent ORDER BY analyzedAt DESC")
    fun observeAnalysisByIntent(intent: String): Flow<List<AnalysisResultEntity>>
}
