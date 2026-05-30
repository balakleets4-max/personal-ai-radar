package com.personalradar.app.quick

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.AnalysisResultEntity
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.RadarCardEntity

class QuickCaptureRepository(
    private val database: AppDatabase
) {
    suspend fun addCapture(rawText: String): QuickCaptureResult {
        val cleanText = rawText.trim()
        require(cleanText.isNotBlank()) { "Capture text is empty" }

        val now = System.currentTimeMillis()
        val language = detectLanguage(cleanText)
        val mainIntent = detectIntent(cleanText)
        val summary = cleanText.take(120)
        val hasAction = hasActionSignal(cleanText)
        val hasRisk = hasRiskSignal(cleanText)
        val hasReminder = hasReminderSignal(cleanText)
        val cardTitle = buildCardTitle(cleanText, mainIntent)
        val whyText = buildWhyText(language, mainIntent, hasAction, hasRisk, hasReminder)

        val captureId = database.captureDao().insertCapture(
            CaptureEntity(
                rawText = cleanText,
                createdAt = now,
                updatedAt = now,
                source = "manual_text",
                language = language,
                status = "ACTIVE"
            )
        )

        val analysisId = database.analysisDao().insertAnalysisResult(
            AnalysisResultEntity(
                captureId = captureId,
                analyzedAt = now,
                parserVersion = "quick-v0.1",
                analyzerVersion = "quick-v0.1",
                isLatest = true,
                language = language,
                mainIntent = mainIntent,
                secondaryIntent = null,
                confidence = 0.62f,
                summary = summary,
                detectedDateText = null,
                detectedTimeText = null,
                normalizedDateTime = null,
                hasAction = hasAction,
                hasRisk = hasRisk,
                hasPerson = false,
                hasReminderSignal = hasReminder,
                explanation = whyText
            )
        )

        val cardId = database.radarCardDao().insertRadarCard(
            RadarCardEntity(
                captureId = captureId,
                analysisId = analysisId,
                radarEngineVersion = "quick-radar-v0.1",
                type = mainIntent,
                title = cardTitle,
                description = summary,
                whyText = whyText,
                sourceQuote = cleanText.take(180),
                priority = when {
                    hasRisk -> 5
                    hasReminder || hasAction -> 4
                    else -> 3
                },
                confidence = 0.62f,
                status = "ACTIVE",
                dueAt = null,
                createdAt = now,
                updatedAt = now,
                dedupeKey = "quick:${cleanText.lowercase().take(80)}",
                hasReminder = hasReminder
            )
        )

        return QuickCaptureResult(captureId, analysisId, cardId, cardTitle, whyText)
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

    private fun detectIntent(text: String): String {
        val lower = text.lowercase()
        return when {
            hasRiskSignal(lower) -> "RISK"
            hasReminderSignal(lower) -> "REMINDER"
            hasActionSignal(lower) -> "TASK"
            else -> "THOUGHT"
        }
    }

    private fun hasActionSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf("надо", "нужно", "сделать", "проверить", "позвонить", "купить", "отправить", "do", "check", "call", "send", "buy").any { it in lower }
    }

    private fun hasRiskSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf("риск", "опасно", "проблем", "ошибка", "сломается", "risk", "danger", "problem", "error", "fail").any { it in lower }
    }

    private fun hasReminderSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf("завтра", "сегодня", "вечером", "утром", "напомни", "tomorrow", "today", "remind", "morning", "evening").any { it in lower }
    }

    private fun buildCardTitle(text: String, intent: String): String {
        val prefix = when (intent) {
            "RISK" -> "Risk"
            "REMINDER" -> "Reminder"
            "TASK" -> "Task"
            else -> "Thought"
        }
        return "$prefix: ${text.take(48)}"
    }

    private fun buildWhyText(
        language: String,
        intent: String,
        hasAction: Boolean,
        hasRisk: Boolean,
        hasReminder: Boolean
    ): String {
        val signals = mutableListOf<String>()
        signals.add("language=$language")
        signals.add("intent=$intent")
        if (hasAction) signals.add("action signal")
        if (hasRisk) signals.add("risk signal")
        if (hasReminder) signals.add("time/reminder signal")
        return "Why I see this: ${signals.joinToString()}"
    }
}

data class QuickCaptureResult(
    val captureId: Long,
    val analysisId: Long,
    val cardId: Long,
    val title: String,
    val whyText: String
)
