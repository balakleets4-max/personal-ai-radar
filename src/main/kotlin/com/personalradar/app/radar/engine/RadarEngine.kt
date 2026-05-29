package com.personalradar.app.radar.engine

import com.personalradar.app.analysis.model.AnalysisDraft
import com.personalradar.app.analysis.model.ParsedEntityDraft
import com.personalradar.app.core.database.entity.RadarCardEntity
import com.personalradar.app.core.database.entity.TopicEntity
import com.personalradar.app.radar.domain.RadarCardDraft

interface RadarEngine {
    fun createCandidateCards(
        rawText: String,
        analysis: AnalysisDraft,
        entities: List<ParsedEntityDraft>,
        now: Long
    ): List<RadarCardDraft>

    fun finalizeCards(
        drafts: List<RadarCardDraft>,
        captureId: Long,
        analysisId: Long,
        topics: List<TopicEntity>,
        now: Long
    ): List<RadarCardEntity>
}
