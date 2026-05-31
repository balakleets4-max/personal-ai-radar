package com.personalradar.app.calendar

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.AnalysisResultEntity
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Calendar
import java.util.Locale

class CalendarRadarImporter(
    private val database: AppDatabase
) {
    suspend fun importEvents(events: List<CalendarSourceEvent>): CalendarImportResult {
        val now = System.currentTimeMillis()
        val radarDao = database.radarCardDao()
        val createdCardIds = mutableListOf<Long>()
        var created = 0
        var alreadyKnown = 0

        events.forEach { event ->
            val dedupeKey = event.stableKey()
            val existingCard = radarDao.findLatestCardByDedupeKey(dedupeKey)
            if (existingCard != null) {
                radarDao.bumpDuplicateHitCount(existingCard.id, now)
                alreadyKnown++
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

            createdCardIds.add(cardId)
            created++
        }

        return CalendarImportResult(
            total = events.size,
            created = created,
            alreadyKnown = alreadyKnown,
            activeCount = events.count { it.controlMode == CalendarControlMode.ACTIVE },
            mediumCount = events.count { it.controlMode == CalendarControlMode.MEDIUM },
            weakCount = events.count { it.controlMode == CalendarControlMode.WEAK },
            backgroundCount = events.count { it.controlMode == CalendarControlMode.BACKGROUND },
            createdCardIds = createdCardIds
        )
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
}

data class CalendarImportResult(
    val total: Int,
    val created: Int,
    val alreadyKnown: Int,
    val activeCount: Int,
    val mediumCount: Int,
    val weakCount: Int,
    val backgroundCount: Int,
    val createdCardIds: List<Long>
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
