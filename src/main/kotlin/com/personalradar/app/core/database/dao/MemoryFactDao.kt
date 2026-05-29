package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.MemoryFactEntity

@Dao
interface MemoryFactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryFact(fact: MemoryFactEntity)

    @Query("SELECT * FROM memory_facts WHERE type = :type AND key = :key LIMIT 1")
    suspend fun getMemoryFact(type: String, key: String): MemoryFactEntity?

    @Query("SELECT * FROM memory_facts WHERE type = :type ORDER BY confidence DESC, evidenceCount DESC")
    suspend fun getMemoryFactsByType(type: String): List<MemoryFactEntity>

    @Query("""
        SELECT * FROM memory_facts
        WHERE type = 'SUPPRESSION'
        AND key = :key
        AND (expiresAt IS NULL OR expiresAt > :now)
        LIMIT 1
    """)
    suspend fun getActiveSuppression(key: String, now: Long): MemoryFactEntity?

    @Query("DELETE FROM memory_facts WHERE type = 'SUPPRESSION' AND expiresAt IS NOT NULL AND expiresAt <= :now")
    suspend fun deleteExpiredSuppressions(now: Long)
}
