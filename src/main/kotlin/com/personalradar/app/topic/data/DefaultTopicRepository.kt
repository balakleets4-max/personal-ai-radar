package com.personalradar.app.topic.data

import com.personalradar.app.analysis.model.ParsedEntityDraft
import com.personalradar.app.core.database.dao.TopicDao
import com.personalradar.app.core.database.entity.CaptureTopicCrossRef
import com.personalradar.app.core.database.entity.TopicEntity
import com.personalradar.app.core.model.ParsedEntityType
import com.personalradar.app.core.model.TopicType
import com.personalradar.app.core.utils.normalizeKeyPart

class DefaultTopicRepository(
    private val topicDao: TopicDao
) : TopicRepository {
    override suspend fun upsertTopicsFromEntitiesInTransaction(
        captureId: Long,
        entities: List<ParsedEntityDraft>,
        now: Long
    ): List<TopicEntity> {
        val topicEntities = entities
            .filter { it.type == ParsedEntityType.TOPIC || it.type == ParsedEntityType.PROJECT_SIGNAL || it.type == ParsedEntityType.PERSON }
            .mapNotNull { entity ->
                val name = entity.normalizedValue ?: entity.rawValue
                val normalized = name.normalizeKeyPart()
                if (normalized == "_") null else entity to Pair(name, normalized)
            }
            .distinctBy { it.second.second }

        val result = mutableListOf<TopicEntity>()
        for ((entity, pair) in topicEntities) {
            val (name, normalized) = pair
            val existing = topicDao.getTopicByNormalizedName(normalized)
            val topicId = if (existing == null) {
                topicDao.insertTopic(
                    TopicEntity(
                        name = name,
                        normalizedName = normalized,
                        type = if (entity.type == ParsedEntityType.PERSON) TopicType.PERSON else TopicType.UNKNOWN,
                        createdAt = now,
                        lastSeenAt = now,
                        seenCount = 1,
                        importanceScore = 0.1f
                    )
                )
            } else {
                topicDao.bumpTopic(existing.id, now, 0.03f)
                existing.id
            }
            topicDao.insertCrossRef(CaptureTopicCrossRef(captureId, topicId, entity.confidence.coerceIn(0f, 1f)))
            topicDao.getTopicById(topicId)?.let { result.add(it) }
        }
        return result
    }
}
