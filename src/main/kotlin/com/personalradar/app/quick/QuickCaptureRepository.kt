package com.personalradar.app.quick

import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.AnalysisResultEntity
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Calendar
import java.util.Locale

class QuickCaptureRepository(
    private val database: AppDatabase
) {
    suspend fun addCapture(rawText: String): QuickCaptureResult {
        val cleanText = rawText.trim()
        require(cleanText.isNotBlank()) { "Текст захвата пустой" }

        val now = System.currentTimeMillis()
        val language = detectLanguage(cleanText)
        val dateSignal = parseDateSignal(cleanText, now)
        val mainIntent = detectIntent(cleanText, dateSignal)
        val summary = cleanText.take(120)
        val hasAction = hasActionSignal(cleanText)
        val hasRisk = hasRiskSignal(cleanText)
        val hasReminder = hasReminderSignal(cleanText) || dateSignal != null
        val cardTitle = buildCardTitle(cleanText, mainIntent)
        val whyText = buildWhyText(language, mainIntent, hasAction, hasRisk, hasReminder, dateSignal)

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
                parserVersion = "quick-parser-v0.3",
                analyzerVersion = "quick-analyzer-v0.3",
                isLatest = true,
                language = language,
                mainIntent = mainIntent,
                secondaryIntent = null,
                confidence = if (dateSignal != null) 0.72f else 0.62f,
                summary = summary,
                detectedDateText = dateSignal?.dateText,
                detectedTimeText = dateSignal?.timeText,
                normalizedDateTime = dateSignal?.timestampMillis,
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
                radarEngineVersion = "quick-radar-v0.3",
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
                confidence = if (dateSignal != null) 0.72f else 0.62f,
                status = "ACTIVE",
                dueAt = dateSignal?.timestampMillis,
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

    private fun detectIntent(text: String, dateSignal: DateSignal?): String {
        val lower = text.lowercase()
        return when {
            hasRiskSignal(lower) -> "RISK"
            hasReminderSignal(lower) || dateSignal != null -> "REMINDER"
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
        return listOf(
            "завтра", "сегодня", "послезавтра", "вечером", "утром", "днём", "днем", "через", "напомни",
            "tomorrow", "today", "remind", "morning", "evening"
        ).any { it in lower }
    }

    private fun buildCardTitle(text: String, intent: String): String {
        val prefix = when (intent) {
            "RISK" -> "Риск"
            "REMINDER" -> "Напоминание"
            "TASK" -> "Задача"
            else -> "Мысль"
        }
        return "$prefix: ${text.take(48)}"
    }

    private fun buildWhyText(
        language: String,
        intent: String,
        hasAction: Boolean,
        hasRisk: Boolean,
        hasReminder: Boolean,
        dateSignal: DateSignal?
    ): String {
        val signals = mutableListOf<String>()
        signals.add("язык: $language")
        signals.add("тип: ${humanIntent(intent)}")
        if (hasAction) signals.add("есть сигнал действия")
        if (hasRisk) signals.add("есть сигнал риска")
        if (hasReminder) signals.add("есть сигнал времени/напоминания")
        if (dateSignal != null) signals.add("когда: ${dateSignal.label}")
        return signals.joinToString("; ")
    }

    private fun humanIntent(intent: String): String {
        return when (intent) {
            "RISK" -> "риск"
            "REMINDER" -> "напоминание"
            "TASK" -> "задача"
            else -> "мысль"
        }
    }

    private fun parseDateSignal(text: String, nowMillis: Long): DateSignal? {
        val lower = text.lowercase(Locale.getDefault())
        val relative = parseRelativeDate(lower, nowMillis)
        if (relative != null) return relative

        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dateText = when {
            "послезавтра" in lower -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                "послезавтра"
            }
            "завтра" in lower || "tomorrow" in lower -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                "завтра"
            }
            "сегодня" in lower || "today" in lower -> "сегодня"
            else -> null
        } ?: return null

        val timeSignal = detectTimeOfDay(lower)
        applyTimeOfDay(calendar, timeSignal)
        return DateSignal(
            label = buildDateLabel(dateText, timeSignal.label),
            dateText = dateText,
            timeText = timeSignal.label,
            timestampMillis = calendar.timeInMillis
        )
    }

    private fun parseRelativeDate(text: String, nowMillis: Long): DateSignal? {
        val match = Regex("через\\s+(\\d+)\\s*(минут[уы]?|час(?:а|ов)?|дн(?:я|ей|ь)?)").find(text) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        val unit = match.groupValues[2]
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val unitLabel: String
        when {
            unit.startsWith("минут") -> {
                calendar.add(Calendar.MINUTE, amount)
                unitLabel = plural(amount, "минуту", "минуты", "минут")
            }
            unit.startsWith("час") -> {
                calendar.add(Calendar.HOUR_OF_DAY, amount)
                unitLabel = plural(amount, "час", "часа", "часов")
            }
            else -> {
                calendar.add(Calendar.DAY_OF_YEAR, amount)
                unitLabel = plural(amount, "день", "дня", "дней")
            }
        }
        val label = "через $amount $unitLabel"
        return DateSignal(
            label = label,
            dateText = label,
            timeText = null,
            timestampMillis = calendar.timeInMillis
        )
    }

    private fun detectTimeOfDay(text: String): TimeSignal {
        return when {
            "утром" in text || "morning" in text -> TimeSignal("утром", 9, 0)
            "днём" in text || "днем" in text -> TimeSignal("днём", 13, 0)
            "вечером" in text || "evening" in text -> TimeSignal("вечером", 19, 0)
            else -> TimeSignal(null, 9, 0)
        }
    }

    private fun applyTimeOfDay(calendar: Calendar, signal: TimeSignal) {
        calendar.set(Calendar.HOUR_OF_DAY, signal.hour)
        calendar.set(Calendar.MINUTE, signal.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun buildDateLabel(dateText: String, timeText: String?): String {
        return if (timeText == null) dateText else "$dateText $timeText"
    }

    private fun plural(number: Int, one: String, few: String, many: String): String {
        val n = number % 100
        val n1 = number % 10
        return if (n in 11..14) many else when (n1) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }
}

data class DateSignal(
    val label: String,
    val dateText: String?,
    val timeText: String?,
    val timestampMillis: Long
)

data class TimeSignal(
    val label: String?,
    val hour: Int,
    val minute: Int
)

data class QuickCaptureResult(
    val captureId: Long,
    val analysisId: Long,
    val cardId: Long,
    val title: String,
    val whyText: String
)
