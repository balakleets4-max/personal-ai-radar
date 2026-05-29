from pathlib import Path
root=Path('/mnt/data/personal_ai_radar_v014/src/main/kotlin/com/personalradar/app')
def write(rel, content):
    p=root/rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content.strip()+"\n", encoding='utf-8')
base='core/database/dao'
common_imports='''package com.personalradar.app.core.database.dao\n\nimport androidx.room.Dao\nimport androidx.room.Insert\nimport androidx.room.OnConflictStrategy\nimport androidx.room.Query\nimport androidx.room.Update\nimport kotlinx.coroutines.flow.Flow\n'''
write(f'{base}/CaptureDao.kt', common_imports + r'''
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
}
''')
write(f'{base}/AnalysisDao.kt', common_imports + r'''
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
''')
write(f'{base}/ParsedEntityDao.kt', common_imports + r'''
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
''')
write(f'{base}/RadarCardDao.kt', common_imports + r'''
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
}
''')
write(f'{base}/ActionDao.kt', common_imports + r'''
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
''')
write(f'{base}/ReminderDao.kt', common_imports + r'''
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
''')
write(f'{base}/TopicDao.kt', common_imports + r'''
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.CaptureTopicCrossRef
import com.personalradar.app.core.database.entity.TopicEntity

@Dao
interface TopicDao {
    @Insert
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Long): TopicEntity?

    @Query("SELECT * FROM topics WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getTopicByNormalizedName(normalizedName: String): TopicEntity?

    @Query("SELECT * FROM topics ORDER BY importanceScore DESC, seenCount DESC, lastSeenAt DESC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE seenCount >= :minSeenCount ORDER BY seenCount DESC, lastSeenAt DESC")
    suspend fun getRepeatingTopics(minSeenCount: Int): List<TopicEntity>

    @Query("UPDATE topics SET seenCount = seenCount + 1, lastSeenAt = :now, importanceScore = importanceScore + :importanceDelta WHERE id = :topicId")
    suspend fun bumpTopic(topicId: Long, now: Long, importanceDelta: Float)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CaptureTopicCrossRef)

    @Query("""
        SELECT topics.* FROM topics
        INNER JOIN capture_topic_cross_refs
        ON topics.id = capture_topic_cross_refs.topicId
        WHERE capture_topic_cross_refs.captureId = :captureId
    """)
    suspend fun getTopicsForCapture(captureId: Long): List<TopicEntity>

    @Query("""
        SELECT captures.* FROM captures
        INNER JOIN capture_topic_cross_refs
        ON captures.id = capture_topic_cross_refs.captureId
        WHERE capture_topic_cross_refs.topicId = :topicId
        AND captures.status != 'DELETED'
        ORDER BY captures.createdAt DESC
    """)
    fun observeCapturesForTopic(topicId: Long): Flow<List<CaptureEntity>>
}
''')
write(f'{base}/DeviceCapabilityDao.kt', common_imports + r'''
import com.personalradar.app.core.database.entity.DeviceCapabilityEntity

@Dao
interface DeviceCapabilityDao {
    @Insert
    suspend fun insertCapability(capability: DeviceCapabilityEntity): Long

    @Query("SELECT * FROM device_capabilities WHERE capability = :capability LIMIT 1")
    suspend fun getCapability(capability: String): DeviceCapabilityEntity?

    @Query("""
        UPDATE device_capabilities
        SET state = :state, checkedAt = :checkedAt, explanation = :explanation, canRequest = :canRequest
        WHERE capability = :capability
    """)
    suspend fun updateCapabilityState(capability: String, state: String, checkedAt: Long, explanation: String, canRequest: Boolean)

    @Query("SELECT * FROM device_capabilities ORDER BY category ASC, capability ASC")
    fun observeCapabilities(): Flow<List<DeviceCapabilityEntity>>

    @Query("SELECT state FROM device_capabilities WHERE capability = :capability LIMIT 1")
    suspend fun getCapabilityState(capability: String): String?
}
''')
write(f'{base}/MemoryFactDao.kt', common_imports + r'''
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
''')
write(f'{base}/ReflectionDao.kt', common_imports + r'''
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
''')
write(f'{base}/UserSettingsDao.kt', common_imports + r'''
import com.personalradar.app.core.database.entity.UserSettingsEntity

@Dao
interface UserSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: UserSettingsEntity)

    @Query("SELECT value FROM user_settings WHERE key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM user_settings ORDER BY key ASC")
    fun observeSettings(): Flow<List<UserSettingsEntity>>
}
''')
write(f'{base}/AppEventLogDao.kt', common_imports + r'''
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
''')
write(f'{base}/PendingSystemActionDao.kt', common_imports + r'''
import com.personalradar.app.core.database.entity.PendingSystemActionEntity

@Dao
interface PendingSystemActionDao {
    @Insert
    suspend fun insertPendingAction(action: PendingSystemActionEntity): Long

    @Update
    suspend fun updatePendingAction(action: PendingSystemActionEntity)

    @Query("""
        SELECT * FROM pending_system_actions
        WHERE status IN ('PENDING', 'FAILED')
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getPendingOrFailedActions(limit: Int): List<PendingSystemActionEntity>

    @Query("""
        UPDATE pending_system_actions
        SET status = :status, updatedAt = :now, attemptCount = attemptCount + :attemptDelta, lastError = :lastError
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: String, now: Long, attemptDelta: Int, lastError: String?)

    @Query("DELETE FROM pending_system_actions WHERE status IN ('DONE', 'CANCELLED') AND updatedAt < :before")
    suspend fun deleteCompletedBefore(before: Long)
}
''')
# AppDatabase
write('core/database/AppDatabase.kt', r'''
package com.personalradar.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personalradar.app.core.database.dao.*
import com.personalradar.app.core.database.entity.*

@Database(
    entities = [
        CaptureEntity::class,
        AnalysisResultEntity::class,
        ParsedEntityEntity::class,
        RadarCardEntity::class,
        ActionEntity::class,
        ReminderEntity::class,
        TopicEntity::class,
        CaptureTopicCrossRef::class,
        DeviceCapabilityEntity::class,
        MemoryFactEntity::class,
        ReflectionLogEntity::class,
        UserSettingsEntity::class,
        AppEventLogEntity::class,
        PendingSystemActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun parsedEntityDao(): ParsedEntityDao
    abstract fun radarCardDao(): RadarCardDao
    abstract fun actionDao(): ActionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun topicDao(): TopicDao
    abstract fun deviceCapabilityDao(): DeviceCapabilityDao
    abstract fun memoryFactDao(): MemoryFactDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun appEventLogDao(): AppEventLogDao
    abstract fun pendingSystemActionDao(): PendingSystemActionDao
}
''')
