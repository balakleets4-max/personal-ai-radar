package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.ActionEntity

@Dao
interface ActionDao {
    @Insert
    suspend fun insertAction(action: ActionEntity): Long

    @Query("SELECT * FROM actions WHERE radarCardId = :cardId ORDER BY createdAt DESC")
    suspend fun getActionsForCard(cardId: Long): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE captureId = :captureId ORDER BY createdAt DESC")
    suspend fun getActionsForCapture(captureId: Long): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getActionsByType(type: String): List<ActionEntity>

    @Query("SELECT COUNT(*) FROM actions WHERE type = :type AND createdAt BETWEEN :from AND :to")
    suspend fun countActionsByTypeBetween(type: String, from: Long, to: Long): Int
}
