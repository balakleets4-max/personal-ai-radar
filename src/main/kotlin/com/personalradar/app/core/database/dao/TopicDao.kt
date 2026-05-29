package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Query("""
        SELECT * FROM topics
        WHERE lastSeenAt BETWEEN :from AND :to
        ORDER BY importanceScore DESC, seenCount DESC, lastSeenAt DESC
        LIMIT :limit
    """)
    suspend fun getTopicsSeenBetween(from: Long, to: Long, limit: Int): List<TopicEntity>
}
