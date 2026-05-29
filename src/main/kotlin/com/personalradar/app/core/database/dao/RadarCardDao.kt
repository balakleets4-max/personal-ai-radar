package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.RadarCardEntity

@Dao
interface RadarCardDao {
    @Insert
    suspend fun insertRadarCard(card: RadarCardEntity): Long

    @Insert
    suspend fun insertRadarCards(cards: List<RadarCardEntity>)

    @Update
    suspend fun updateRadarCard(card: RadarCardEntity)

    @Query("SELECT * FROM radar_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): RadarCardEntity?

    @Query("""
        SELECT * FROM radar_cards
        WHERE status = 'ACTIVE'
        ORDER BY priority DESC,
                 CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END,
                 dueAt ASC,
                 createdAt DESC
    """)
    fun observeActiveCards(): Flow<List<RadarCardEntity>>

    @Query("""
        SELECT * FROM radar_cards
        WHERE status = 'ACTIVE'
        AND priority >= 4
        ORDER BY priority DESC,
                 CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END,
                 dueAt ASC,
                 createdAt DESC
        LIMIT :limit
    """)
    fun observeImportantCards(limit: Int = 5): Flow<List<RadarCardEntity>>

    @Query("""
        SELECT * FROM radar_cards
        WHERE status = 'ACTIVE'
        AND dueAt BETWEEN :dayStart AND :dayEnd
        ORDER BY priority DESC, dueAt ASC, createdAt DESC
    """)
    fun observeTodayCards(dayStart: Long, dayEnd: Long): Flow<List<RadarCardEntity>>

    @Query("""
        SELECT * FROM radar_cards
        WHERE status = 'ACTIVE'
        AND type = :type
        ORDER BY priority DESC, createdAt DESC
        LIMIT :limit
    """)
    fun observeCardsByType(type: String, limit: Int): Flow<List<RadarCardEntity>>

    @Query("SELECT * FROM radar_cards WHERE status = 'SNOOZED' AND snoozedUntil <= :now")
    suspend fun getDueSnoozedCards(now: Long): List<RadarCardEntity>

    @Query("""
        UPDATE radar_cards
        SET status = 'ACTIVE', snoozedUntil = NULL, lastShownAt = :now,
            shownCount = shownCount + 1, updatedAt = :now
        WHERE id IN (:cardIds)
    """)
    suspend fun reactivateSnoozedCards(cardIds: List<Long>, now: Long)

    @Query("UPDATE radar_cards SET status = 'DONE', completedAt = :now, updatedAt = :now WHERE id = :cardId")
    suspend fun markDone(cardId: Long, now: Long)

    @Query("UPDATE radar_cards SET status = 'HIDDEN', hiddenAt = :now, updatedAt = :now WHERE id = :cardId")
    suspend fun hideCard(cardId: Long, now: Long)

    @Query("UPDATE radar_cards SET status = 'SNOOZED', snoozedUntil = :until, updatedAt = :now WHERE id = :cardId")
    suspend fun snoozeCard(cardId: Long, until: Long, now: Long)

    @Query("UPDATE radar_cards SET dueAt = :dueAt, updatedAt = :now WHERE id = :cardId")
    suspend fun updateDueDate(cardId: Long, dueAt: Long?, now: Long)

    @Query("UPDATE radar_cards SET hasReminder = :hasReminder, updatedAt = :now WHERE id = :cardId")
    suspend fun setHasReminder(cardId: Long, hasReminder: Boolean, now: Long)

    @Query("UPDATE radar_cards SET type = :newType, updatedAt = :now WHERE id = :cardId")
    suspend fun updateCardType(cardId: Long, newType: String, now: Long)

    @Query("""
        SELECT * FROM radar_cards
        WHERE dedupeKey = :dedupeKey
        AND status IN ('ACTIVE', 'SNOOZED')
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    suspend fun findActiveCardByDedupeKey(dedupeKey: String): RadarCardEntity?

    @Query("UPDATE radar_cards SET duplicateHitCount = duplicateHitCount + 1, updatedAt = :now WHERE id = :cardId")
    suspend fun bumpDuplicateHitCount(cardId: Long, now: Long)

    @Query("UPDATE radar_cards SET shownCount = shownCount + 1, lastShownAt = :now, updatedAt = :now WHERE id = :cardId")
    suspend fun markShown(cardId: Long, now: Long)

    @Query("UPDATE radar_cards SET status = 'ARCHIVED', updatedAt = :now WHERE id = :cardId")
    suspend fun archiveCard(cardId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM radar_cards WHERE createdAt BETWEEN :from AND :to")
    suspend fun countCardsCreatedBetween(from: Long, to: Long): Int
}
