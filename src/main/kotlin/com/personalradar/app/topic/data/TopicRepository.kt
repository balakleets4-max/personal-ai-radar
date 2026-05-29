package com.personalradar.app.topic.data

import com.personalradar.app.analysis.model.ParsedEntityDraft
import com.personalradar.app.core.database.entity.TopicEntity

interface TopicRepository {
    suspend fun upsertTopicsFromEntitiesInTransaction(
        captureId: Long,
        entities: List<ParsedEntityDraft>,
        now: Long
    ): List<TopicEntity>
}
