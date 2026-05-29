package com.personalradar.app.capture.usecase

import com.personalradar.app.analysis.analyzer.AnalysisEngine
import com.personalradar.app.analysis.language.LanguageDetector
import com.personalradar.app.analysis.model.AnalysisDraft
import com.personalradar.app.analysis.model.ParserResult
import com.personalradar.app.analysis.parser.ParserEngine
import com.personalradar.app.capture.domain.*
import com.personalradar.app.core.database.dao.*
import com.personalradar.app.core.database.entity.*
import com.personalradar.app.core.model.*
import com.personalradar.app.core.time.TimeProvider
import com.personalradar.app.core.transaction.TransactionRunner
import com.personalradar.app.core.utils.TextLimits
import com.personalradar.app.core.versions.AppVersions
import com.personalradar.app.radar.engine.RadarEngine
import com.personalradar.app.topic.data.TopicRepository

class AddCaptureUseCase(
    private val languageDetector: LanguageDetector,
    private val parserEngine: ParserEngine,
    private val analysisEngine: AnalysisEngine,
    private val radarEngine: RadarEngine,
    private val captureDao: CaptureDao,
    private val analysisDao: AnalysisDao,
    private val parsedEntityDao: ParsedEntityDao,
    private val radarCardDao: RadarCardDao,
    private val appEventLogDao: AppEventLogDao,
    private val topicRepository: TopicRepository,
    private val transactionRunner: TransactionRunner,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(input: AddCaptureInput): AddCaptureResult {
        val cleanText = input.rawText.trim()
        if (cleanText.isBlank()) throw AddCaptureError.EmptyText
        if (cleanText.length > TextLimits.MAX_CAPTURE_LENGTH) throw AddCaptureError.TextTooLong(TextLimits.MAX_CAPTURE_LENGTH)

        val now = timeProvider.nowMillis()
        val warnings = mutableListOf<AddCaptureWarning>()
        val language = languageDetector.detect(cleanText)
        if (language == LanguageCode.UNKNOWN) warnings.add(AddCaptureWarning.UnknownLanguage)

        lateinit var parserResult: ParserResult
        lateinit var analysisDraft: AnalysisDraft
        var parserFailed: Throwable? = null
        try {
            parserResult = parserEngine.parse(cleanText, language, now)
            analysisDraft = analysisEngine.analyze(cleanText, language, parserResult, now)
        } catch (t: Throwable) {
            parserFailed = t
            warnings.add(AddCaptureWarning.ParserFailedButCaptureSaved(t.message ?: "Unknown parser error"))
            parserResult = ParserResult(language = language, entities = emptyList())
            analysisDraft = AnalysisDraft(
                mainIntent = IntentType.UNKNOWN,
                secondaryIntent = null,
                confidence = 0f,
                summary = cleanText.take(80),
                detectedDateText = null,
                detectedTimeText = null,
                normalizedDateTime = null,
                hasAction = false,
                hasRisk = false,
                hasPerson = false,
                hasReminderSignal = false,
                explanation = "Parser failed; capture saved without reliable analysis."
            )
        }

        if (analysisDraft.confidence < 0.40f) warnings.add(AddCaptureWarning.LowConfidence)

        val cardDrafts = if (parserFailed == null) {
            radarEngine.createCandidateCards(cleanText, analysisDraft, parserResult.entities, now)
        } else {
            emptyList()
        }
        if (cardDrafts.isEmpty()) warnings.add(AddCaptureWarning.NoRadarCardCreated)

        val saveResult = transactionRunner.runInTransaction {
            val captureId = captureDao.insertCapture(
                CaptureEntity(
                    rawText = cleanText,
                    createdAt = now,
                    updatedAt = now,
                    source = input.source,
                    language = language,
                    status = CaptureStatus.ACTIVE
                )
            )

            analysisDao.markAllAnalysisAsNotLatest(captureId)
            val analysisId = analysisDao.insertAnalysisResult(
                AnalysisResultEntity(
                    captureId = captureId,
                    analyzedAt = now,
                    parserVersion = AppVersions.PARSER_VERSION,
                    analyzerVersion = AppVersions.ANALYZER_VERSION,
                    isLatest = true,
                    language = language,
                    mainIntent = analysisDraft.mainIntent,
                    secondaryIntent = analysisDraft.secondaryIntent,
                    confidence = analysisDraft.confidence.coerceIn(0f, 1f),
                    summary = analysisDraft.summary,
                    detectedDateText = analysisDraft.detectedDateText,
                    detectedTimeText = analysisDraft.detectedTimeText,
                    normalizedDateTime = analysisDraft.normalizedDateTime,
                    hasAction = analysisDraft.hasAction,
                    hasRisk = analysisDraft.hasRisk,
                    hasPerson = analysisDraft.hasPerson,
                    hasReminderSignal = analysisDraft.hasReminderSignal,
                    explanation = analysisDraft.explanation
                )
            )

            parsedEntityDao.insertParsedEntities(
                parserResult.entities.map { e ->
                    ParsedEntityEntity(
                        captureId = captureId,
                        analysisId = analysisId,
                        type = e.type,
                        rawValue = e.rawValue,
                        normalizedValue = e.normalizedValue,
                        startIndex = e.startIndex,
                        endIndex = e.endIndex,
                        confidence = e.confidence.coerceIn(0f, 1f)
                    )
                }
            )

            if (parserFailed != null) {
                appEventLogDao.insertEvent(
                    AppEventLogEntity(
                        createdAt = now,
                        level = AppEventLevel.WARNING,
                        category = AppEventCategory.PARSER,
                        message = "Parser failed for capture; raw text omitted for privacy.",
                        relatedCaptureId = captureId
                    )
                )
            }

            val topics = topicRepository.upsertTopicsFromEntitiesInTransaction(captureId, parserResult.entities, now)
            val finalCards = radarEngine.finalizeCards(cardDrafts, captureId, analysisId, topics, now)
            val created = mutableListOf<Long>()
            val updated = mutableListOf<Long>()

            for (card in finalCards) {
                val existing = card.dedupeKey?.let { radarCardDao.findActiveCardByDedupeKey(it) }
                if (existing != null) {
                    radarCardDao.bumpDuplicateHitCount(existing.id, now)
                    updated.add(existing.id)
                } else {
                    created.add(radarCardDao.insertRadarCard(card))
                }
            }
            TransactionSaveResult(captureId, analysisId, created, updated)
        }

        val finalWarnings = warnings.toMutableList()
        if (saveResult.updatedCardIds.isNotEmpty()) finalWarnings.add(AddCaptureWarning.DuplicateCardUpdated)

        return AddCaptureResult(
            captureId = saveResult.captureId,
            analysisId = saveResult.analysisId,
            createdCardIds = saveResult.createdCardIds,
            updatedCardIds = saveResult.updatedCardIds,
            summary = analysisDraft.summary,
            detectedLanguage = language,
            mainIntent = analysisDraft.mainIntent,
            warnings = finalWarnings.distinctBy { it.toString() }
        )
    }

    private data class TransactionSaveResult(
        val captureId: Long,
        val analysisId: Long,
        val createdCardIds: List<Long>,
        val updatedCardIds: List<Long>
    )
}
