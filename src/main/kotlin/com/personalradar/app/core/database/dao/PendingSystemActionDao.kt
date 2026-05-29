package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import com.personalradar.app.core.database.entity.PendingSystemActionEntity

@Dao
interface PendingSystemActionDao {
    @Insert
    suspend fun insertPendingAction(action: PendingSystemActionEntity): Long

    @Update
    suspend fun updatePendingAction(action: PendingSystemActionEntity)

    @Query("""
        SELECT * FROM pending_system_actions
        WHERE status = 'PENDING'
           OR (status = 'FAILED' AND attemptCount < :maxAttempts)
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getRunnableActions(
        limit: Int,
        maxAttempts: Int
    ): List<PendingSystemActionEntity>

    @Query("""
        UPDATE pending_system_actions
        SET status = :status,
            updatedAt = :now,
            attemptCount = attemptCount + :attemptDelta,
            lastError = :lastError
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: Long,
        status: String,
        now: Long,
        attemptDelta: Int,
        lastError: String?
    )

    @Query("DELETE FROM pending_system_actions WHERE status IN ('DONE', 'CANCELLED') AND updatedAt < :before")
    suspend fun deleteCompletedBefore(before: Long)
}
