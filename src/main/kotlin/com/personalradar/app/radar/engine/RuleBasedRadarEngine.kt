package com.personalradar.app.radar.engine

import com.personalradar.app.analysis.model.AnalysisDraft
import com.personalradar.app.analysis.model.ParsedEntityDraft
import com.personalradar.app.core.database.entity.RadarCardEntity
import com.personalradar.app.core.database.entity.TopicEntity
import com.personalradar.app.core.model.IntentType
import com.personalradar.app.core.model.ParsedEntityType
import com.personalradar.app.core.model.RadarCardStatus
import com.personalradar.app.core.model.RadarCardType
import com.personalradar.app.core.utils.buildDedupeKey
import com.personalradar.app.core.utils.toSourceQuote
import com.personalradar.app.core.versions.AppVersions
import com.personalradar.app.radar.domain.RadarCardDraft

class RuleBasedRadarEngine : RadarEngine {
    override fun createCandidateCards(
        rawText: String,
        analysis: AnalysisDraft,
        entities: List<ParsedEntityDraft>,
        now: Long
    ): List<RadarCardDraft> {
        if (analysis.confidence < 0.40f) return emptyList()

        val action = entities.firstOrNull { it.type == ParsedEntityType.ACTION }?.normalizedValue
        val person = entities.firstOrNull { it.type == ParsedEntityType.PERSON }?.normalizedValue
        val topic = entities.firstOrNull { it.type == ParsedEntityType.TOPIC || it.type == ParsedEntityType.PROJECT_SIGNAL }?.normalizedValue
        val hasUncertainDate = entities.any { it.type == ParsedEntityType.UNCERTAIN_DATE }
        val hasIdea = entities.any { it.type == ParsedEntityType.IDEA_SIGNAL }

        val type = when {
            analysis.mainIntent == IntentType.REMINDER -> RadarCardType.REMINDER
            analysis.mainIntent == IntentType.IDEA && topic != null -> RadarCardType.PROJECT_SIGNAL
            analysis.mainIntent == IntentType.RISK -> RadarCardType.RISK
            analysis.mainIntent == IntentType.TASK && hasUncertainDate -> RadarCardType.TASK_HIDDEN
            analysis.mainIntent == IntentType.TASK && !hasIdea -> RadarCardType.TASK_EXPLICIT
            else -> return emptyList()
        }

        val title = analysis.summary.ifBlank { rawText.take(80) }
        val why = when (type) {
            RadarCardType.REMINDER -> "Ты написал сигнал напоминания. Подтверди, чтобы я создал напоминание."
            RadarCardType.TASK_HIDDEN -> "Ты написал мягкий срок вроде “потом”. Похоже, это задача без точного срока."
            RadarCardType.RISK -> "Ты написал фразу риска. Похоже, это может потеряться или сорваться."
            RadarCardType.PROJECT_SIGNAL -> "Это похоже на идею или сигнал для проекта."
            else -> "Я нашёл действие и признаки задачи."
        }
        val priority = when (type) {
            RadarCardType.REMINDER -> 4
            RadarCardType.RISK -> 4
            RadarCardType.TASK_HIDDEN -> 3
            RadarCardType.TASK_EXPLICIT -> 3
            else -> 2
        }
        val dedupe = if (action != null || topic != null) buildDedupeKey(type, action, person, topic) else null

        return listOf(
            RadarCardDraft(
                type = type,
                title = title,
                description = "Создано из Capture.",
                whyText = why,
                sourceQuote = rawText.toSourceQuote(),
                priority = priority.coerceIn(1, 5),
                confidence = analysis.confidence.coerceIn(0f, 1f),
                dueAt = analysis.normalizedDateTime,
                dedupeKey = dedupe
            )
        )
    }

    override fun finalizeCards(
        drafts: List<RadarCardDraft>,
        captureId: Long,
        analysisId: Long,
        topics: List<TopicEntity>,
        now: Long
    ): List<RadarCardEntity> {
        return drafts.take(2).map { draft ->
            RadarCardEntity(
                captureId = captureId,
                analysisId = analysisId,
                radarEngineVersion = AppVersions.RADAR_ENGINE_VERSION,
                type = draft.type,
                title = draft.title,
                description = draft.description,
                whyText = draft.whyText,
                sourceQuote = draft.sourceQuote,
                priority = draft.priority.coerceIn(1, 5),
                confidence = draft.confidence.coerceIn(0f, 1f),
                status = RadarCardStatus.ACTIVE,
                dueAt = draft.dueAt,
                createdAt = now,
                updatedAt = now,
                dedupeKey = draft.dedupeKey,
                hasReminder = false
            )
        }
    }
}
