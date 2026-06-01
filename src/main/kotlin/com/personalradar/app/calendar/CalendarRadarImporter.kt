package com.personalradar.app.calendar

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.AnalysisResultEntity
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class CalendarRadarImporter(
    private val database: AppDatabase
) {
    suspend fun importEvents(
        events: List<CalendarSourceEvent>,
        syncStartedAt: Long = System.currentTimeMillis(),
        syncDaysAhead: Int = DEFAULT_SYNC_DAYS_AHEAD,
        syncLimit: Int = DEFAULT_SYNC_LIMIT
    ): CalendarImportResult {
        val now = syncStartedAt
        val radarDao = database.radarCardDao()
        val createdCardIds = mutableListOf<Long>()
        val updatedCardIds = mutableListOf<Long>()
        val archivedDuplicateCardIds = mutableListOf<Long>()
        var created = 0
        var alreadyKnown = 0

        events.deduplicatedSourceEvents().forEach { event ->
            val dedupeKey = event.radarDedupeKey()
            val existingCard = findBestExistingCard(event)
            if (existingCard != null) {
                val reminderDueAt = event.reminderDueAt()
                val refreshedCard = existingCard.copy(
                    title = event.cleanTitle(),
                    description = buildDescription(event),
                    whyText = buildWhyText(event),
                    sourceQuote = event.toRadarCaptureText().take(180),
                    priority = event.controlMode.priority,
                    dueAt = reminderDueAt,
                    updatedAt = now,
                    dedupeKey = dedupeKey,
                    hasReminder = true
                )
                val changed = existingCard.title != refreshedCard.title ||
                    existingCard.description != refreshedCard.description ||
                    existingCard.whyText != refreshedCard.whyText ||
                    existingCard.priority != refreshedCard.priority ||
                    existingCard.dueAt != refreshedCard.dueAt ||
                    existingCard.dedupeKey != refreshedCard.dedupeKey
                if (changed) {
                    radarDao.updateRadarCard(refreshedCard)
                    updatedCardIds.add(existingCard.id)
                } else {
                    radarDao.bumpDuplicateHitCount(existingCard.id, now)
                    alreadyKnown++
                }
                archivedDuplicateCardIds.addAll(archiveDuplicateCardsForEvent(event, existingCard.id, now))
                return@forEach
            }

            val language = detectLanguage(event.title + " " + event.description)
            val reminderDueAt = event.reminderDueAt()

            val captureId = database.captureDao().insertCapture(
                CaptureEntity(
                    rawText = event.toRadarCaptureText(),
                    createdAt = now,
                    updatedAt = now,
                    source = "calendar_provider",
                    language = language,
                    status = "ACTIVE"
                )
            )

            val analysisId = database.analysisDao().insertAnalysisResult(
                AnalysisResultEntity(
                    captureId = captureId,
                    analyzedAt = now,
                    parserVersion = "calendar-source-v0.1",
                    analyzerVersion = "calendar-radar-importer-v0.1",
                    isLatest = true,
                    language = language,
                    mainIntent = "CALENDAR",
                    secondaryIntent = null,
                    confidence = 0.92f,
                    summary = event.cleanTitle().take(120),
                    detectedDateText = event.displayWhenText(),
                    detectedTimeText = if (event.allDay) "09:00" else event.displayClockText(),
                    normalizedDateTime = reminderDueAt,
                    hasAction = false,
                    hasRisk = false,
                    hasPerson = false,
                    hasReminderSignal = true,
                    explanation = buildWhyText(event)
                )
            )

            val cardId = radarDao.insertRadarCard(
                RadarCardEntity(
                    captureId = captureId,
                    analysisId = analysisId,
                    radarEngineVersion = "calendar-radar-v0.1",
                    type = "CALENDAR",
                    title = event.cleanTitle(),
                    description = buildDescription(event),
                    whyText = buildWhyText(event),
                    sourceQuote = event.toRadarCaptureText().take(180),
                    priority = event.controlMode.priority,
                    confidence = 0.92f,
                    status = "ACTIVE",
                    dueAt = reminderDueAt,
                    createdAt = now,
                    updatedAt = now,
                    dedupeKey = dedupeKey,
                    hasReminder = true
                )
            )

            archivedDuplicateCardIds.addAll(archiveDuplicateCardsForEvent(event, cardId, now))
            createdCardIds.add(cardId)
            created++
        }

        val archivedMissingCardIds = archiveMissingVisibleCalendarCards(
            events = events,
            now = now,
            syncDaysAhead = syncDaysAhead,
            syncLimit = syncLimit
        )
        val allArchivedIds = (archivedMissingCardIds + archivedDuplicateCardIds).distinct()

        return CalendarImportResult(
            total = events.size,
            created = created,
            alreadyKnown = alreadyKnown,
            activeCount = events.count { it.controlMode == CalendarControlMode.ACTIVE },
            mediumCount = events.count { it.controlMode == CalendarControlMode.MEDIUM },
            weakCount = events.count { it.controlMode == CalendarControlMode.WEAK },
            backgroundCount = events.count { it.controlMode == CalendarControlMode.BACKGROUND },
            createdCardIds = createdCardIds,
            updatedCardIds = updatedCardIds,
            archivedMissingCardIds = allArchivedIds
        )
    }

    private fun List<CalendarSourceEvent>.deduplicatedSourceEvents(): List<CalendarSourceEvent> {
        val seen = linkedSetOf<String>()
        return filter { event -> seen.add(event.semanticMergeKey()) }
    }

    private suspend fun findBestExistingCard(event: CalendarSourceEvent): RadarCardEntity? {
        val radarDao = database.radarCardDao()
        val dedupeKey = event.radarDedupeKey()
        val legacySourceKey = event.stableKey()
        val identityPattern = event.sourceIdentityPrefix() + "%"
        val calendarPattern = event.calendarPattern()
        val dueAt = event.reminderDueAt() ?: event.beginMillis
        return radarDao.findNonArchivedCardByDedupeKey(dedupeKey)
            ?: radarDao.findNonArchivedCardByDedupeKey(legacySourceKey)
            ?: radarDao.findVisibleCalendarCardByIdentityPattern(identityPattern)
            ?: radarDao.getVisibleCalendarCardsByTitleAndDueAt(
                title = event.cleanTitle(),
                fromMillis = dueAt - SAME_EVENT_TIME_TOLERANCE_MS,
                toMillis = dueAt + SAME_EVENT_TIME_TOLERANCE_MS
            ).firstOrNull()
            ?: radarDao.getVisibleCalendarCardsByCalendarAndDueAt(
                calendarPattern = calendarPattern,
                fromMillis = dueAt - LEGACY_MATCH_TOLERANCE_MS,
                toMillis = dueAt + LEGACY_MATCH_TOLERANCE_MS
            ).firstOrNull()
            ?: radarDao.getVisibleCalendarCardsByCalendarAndTitle(
                calendarPattern = calendarPattern,
                title = event.cleanTitle()
            ).firstOrNull()
    }

    private suspend fun archiveDuplicateCardsForEvent(event: CalendarSourceEvent, keepCardId: Long, now: Long): List<Long> {
        val radarDao = database.radarCardDao()
        val dueAt = event.reminderDueAt() ?: event.beginMillis
        val identityMatches = radarDao.getVisibleCalendarCardsByIdentityPattern(event.sourceIdentityPrefix() + "%")
        val sameTitleTimeMatches = radarDao.getVisibleCalendarCardsByTitleAndDueAt(
            title = event.cleanTitle(),
            fromMillis = dueAt - SAME_EVENT_TIME_TOLERANCE_MS,
            toMillis = dueAt + SAME_EVENT_TIME_TOLERANCE_MS
        )
        val sameTimeMatches = radarDao.getVisibleCalendarCardsByCalendarAndDueAt(
            calendarPattern = event.calendarPattern(),
            fromMillis = dueAt - LEGACY_MATCH_TOLERANCE_MS,
            toMillis = dueAt + LEGACY_MATCH_TOLERANCE_MS
        )
        val duplicateIds = (identityMatches + sameTitleTimeMatches + sameTimeMatches)
            .map { it.id }
            .filter { it != keepCardId }
            .distinct()
        if (duplicateIds.isNotEmpty()) radarDao.archiveCards(duplicateIds, now)
        return duplicateIds
    }

    private suspend fun archiveMissingVisibleCalendarCards(
        events: List<CalendarSourceEvent>,
        now: Long,
        syncDaysAhead: Int,
        syncLimit: Int
    ): List<Long> {
        // If the provider result is truncated, archiving would be unsafe: some existing events may simply be beyond the limit.
        if (events.size >= syncLimit) return emptyList()

        val syncWindowStart = now - CALENDAR_SYNC_WINDOW_TOLERANCE_MS
        val syncWindowEnd = now + TimeUnit.DAYS.toMillis(syncDaysAhead.toLong().coerceAtLeast(1L)) + CALENDAR_SYNC_WINDOW_TOLERANCE_MS
        val sourceKeys = events.flatMap { event -> listOf(event.stableKey(), event.radarDedupeKey()) }.toSet()
        val sourcePrefixes = events.map { event -> event.sourceIdentityPrefix() }
        val semanticKeys = events.map { event -> event.semanticMergeKey() }.toSet()
        val visibleCards = database.radarCardDao().getVisibleCalendarCardsInWindow(
            fromMillis = syncWindowStart,
            toMillis = syncWindowEnd
        )
        val missingCards = visibleCards.filter { card ->
            val key = card.dedupeKey.orEmpty()
            key !in sourceKeys && sourcePrefixes.none { prefix -> key.startsWith(prefix) } && card.semanticMergeKey() !in semanticKeys
        }
        if (missingCards.isEmpty()) return emptyList()
        val missingCardIds = missingCards.map { it.id }
        database.radarCardDao().archiveCards(missingCardIds, now)
        return missingCardIds
    }

    private fun buildDescription(event: CalendarSourceEvent): String {
        val parts = mutableListOf<String>()
        parts.add("Событие из календаря.")
        if (event.allDay) parts.add("Время: весь день.")
        if (event.location.isNotBlank()) parts.add("Место: ${event.location}.")
        if (event.description.isNotBlank()) parts.add("Описание: ${event.description.take(120)}.")
        return parts.joinToString(" ")
    }

    private fun buildWhyText(event: CalendarSourceEvent): String {
        return "Событие найдено в календаре; когда: ${event.displayWhenText()}; контроль: ${event.controlMode.label}"
    }

    private fun detectLanguage(text: String): String {
        val hasCyrillic = text.any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        return when {
            hasCyrillic && hasLatin -> "MIXED"
            hasCyrillic -> "RU"
            hasLatin -> "EN"
            else -> "UNKNOWN"
        }
    }

    companion object {
        private const val DEFAULT_SYNC_DAYS_AHEAD = 14
        private const val DEFAULT_SYNC_LIMIT = 60
        private const val CALENDAR_SYNC_WINDOW_TOLERANCE_MS = 5L * 60L * 1000L
        private const val LEGACY_MATCH_TOLERANCE_MS = 2L * 60L * 60L * 1000L
        private const val SAME_EVENT_TIME_TOLERANCE_MS = 10L * 60L * 1000L
    }
}

data class CalendarImportResult(
    val total: Int,
    val created: Int,
    val alreadyKnown: Int,
    val activeCount: Int,
    val mediumCount: Int,
    val weakCount: Int,
    val backgroundCount: Int,
    val createdCardIds: List<Long>,
    val updatedCardIds: List<Long> = emptyList(),
    val archivedMissingCardIds: List<Long> = emptyList()
)

private fun CalendarSourceEvent.cleanTitle(): String {
    return title
        .trim()
        .replace(Regex("\\s+"), " ")
        .ifBlank { "Событие календаря" }
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun CalendarSourceEvent.reminderDueAt(): Long? {
    if (beginMillis <= 0L) return null
    if (!allDay) return beginMillis

    return Calendar.getInstance().apply {
        timeInMillis = beginMillis
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun CalendarSourceEvent.radarDedupeKey(): String {
    return sourceIdentityPrefix()
}

private fun CalendarSourceEvent.sourceIdentityPrefix(): String {
    return "calendar:$calendarId:$eventId:"
}

private fun CalendarSourceEvent.calendarPattern(): String {
    return "calendar:$calendarId:%"
}

private fun CalendarSourceEvent.semanticMergeKey(): String {
    return "calendar-semantic:${cleanTitle().lowercase(Locale.getDefault())}:${reminderDueAt() ?: beginMillis}"
}

private fun RadarCardEntity.semanticMergeKey(): String {
    return "calendar-semantic:${title.trim().lowercase(Locale.getDefault())}:${dueAt ?: 0L}"
}
