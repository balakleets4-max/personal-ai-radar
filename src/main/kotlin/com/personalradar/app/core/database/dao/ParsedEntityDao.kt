package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.ParsedEntityEntity

@Dao
interface ParsedEntityDao {
    @Insert
    suspend fun insertParsedEntity(entity: ParsedEntityEntity): Long

    @Insert
    suspend fun insertParsedEntities(entities: List<ParsedEntityEntity>)

    @Query("SELECT * FROM parsed_entities WHERE analysisId = :analysisId ORDER BY startIndex ASC")
    suspend fun getEntitiesForAnalysis(analysisId: Long): List<ParsedEntityEntity>

    @Query("SELECT * FROM parsed_entities WHERE captureId = :captureId ORDER BY startIndex ASC")
    suspend fun getEntitiesForCapture(captureId: Long): List<ParsedEntityEntity>

    @Query("SELECT * FROM parsed_entities WHERE type = :type ORDER BY confidence DESC")
    suspend fun getEntitiesByType(type: String): List<ParsedEntityEntity>

    @Query("SELECT * FROM parsed_entities WHERE normalizedValue = :normalizedValue ORDER BY confidence DESC")
    suspend fun getEntitiesByNormalizedValue(normalizedValue: String): List<ParsedEntityEntity>
}
