package com.personalradar.app.quick

import com.personalradar.app.ai.AiAnalysisResult
import com.personalradar.app.ai.AiSettingsStore
import com.personalradar.app.ai.YandexAiClient
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.core.database.entity.AnalysisResultEntity
import com.personalradar.app.core.database.entity.CaptureEntity
import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Calendar
import java.util.Locale

class QuickCaptureRepository(
    private val database: AppDatabase,
    private val aiSettingsStore: AiSettingsStore? = null,
    private val yandexAiClient: YandexAiClient? = null
) {
    suspend fun addCapture(rawText: String): QuickCaptureResult {
        val cleanText = rawText.trim()
        require(cleanText.isNotBlank()) { "Текст захвата пустой" }

        val now = System.currentTimeMillis()
        val language = detectLanguage(cleanText)
        val localDateSignal = parseDateSignal(cleanText, now)
        val cloudResult = tryCloudAnalysis(cleanText)
        val cloudDateSignal = cloudResult?.dueText?.takeIf { it.isNotBlank() }?.let { parseDateSignal(it, now) }
        val dateSignal = cloudDateSignal ?: localDateSignal
        val offlineActionText = OfflineTextPolisher.polishAction(cleanText, dateSignal).ifBlank { extractActionText(cleanText) }
        val cloudActionText = cloudResult?.action
            ?.takeIf { it.isNotBlank() }
            ?.let { OfflineTextPolisher.polishAction(it, dateSignal) }
            ?.takeIf { it.isNotBlank() }
        val actionText = if (cloudActionText != null && !isWeakCloudAction(cloudActionText, offlineActionText)) cloudActionText else offlineActionText
        val mainIntent = normalizeIntent(cloudResult?.type) ?: detectIntent(cleanText, dateSignal)
        val summary = actionText.ifBlank { cleanText }.take(120)
        val hasAction = hasActionSignal(cleanText) || actionText.isNotBlank()
        val hasRisk = hasRiskSignal(cleanText) || mainIntent == "RISK"
        val hasReminder = hasReminderSignal(cleanText) || dateSignal != null || mainIntent == "REMINDER"
        val cardTitle = buildCardTitle(summary, mainIntent)
        val whyText = buildWhyText(language, mainIntent, hasAction, hasRisk, hasReminder, dateSignal, cloudResult)
        val notificationText = OfflineTextPolisher.buildOfflineNotification(actionText)

        val captureId = database.captureDao().insertCapture(
            CaptureEntity(
                rawText = cleanText,
                createdAt = now,
                updatedAt = now,
                source = if (cloudResult != null) "manual_or_shared_text_cloud_ai" else "manual_or_shared_text_offline_polished",
                language = language,
                status = "ACTIVE"
            )
        )

        val analysisId = database.analysisDao().insertAnalysisResult(
            AnalysisResultEntity(
                captureId = captureId,
                analyzedAt = now,
                parserVersion = if (cloudResult != null) "yandex-ai-context-v0.1+datetime-v0.1" else "context-parser-v0.7-offline-polish",
                analyzerVersion = if (cloudResult != null) "cloud-ai-analyzer-v0.1+local-safety-polish" else "offline-polisher-v0.1",
                isLatest = true,
                language = language,
                mainIntent = mainIntent,
                secondaryIntent = null,
                confidence = if (cloudResult != null) 0.84f else if (dateSignal != null) 0.78f else 0.64f,
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
                radarEngineVersion = if (cloudResult != null) "ai-radar-v0.2-local-safety" else "quick-radar-v0.7-offline-polish",
                type = mainIntent,
                title = cardTitle,
                description = notificationText,
                whyText = whyText,
                sourceQuote = cleanText.take(180),
                priority = cloudResult?.importance ?: when {
                    hasRisk -> 5
                    hasReminder || hasAction -> 4
                    else -> 3
                },
                confidence = if (cloudResult != null) 0.84f else if (dateSignal != null) 0.78f else 0.64f,
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

    private fun tryCloudAnalysis(text: String): AiAnalysisResult? {
        val settings = aiSettingsStore?.getSettings()
        if (settings == null) {
            yandexAiClient?.analyzeText(text, com.personalradar.app.ai.AiSettings(false, "Yandex AI", "", "", true, true, false))
            return null
        }
        return yandexAiClient?.analyzeText(text, settings)
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

    private fun normalizeIntent(type: String?): String? {
        return when (type?.lowercase()?.trim()) {
            "reminder" -> "REMINDER"
            "task" -> "TASK"
            "risk" -> "RISK"
            "thought" -> "THOUGHT"
            else -> null
        }
    }

    private fun hasActionSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "надо", "нужно", "сделать", "проверить", "позвонить", "купить", "отправить", "забрать", "принести", "выпить", "оплатить",
            "отметить", "праздновать", "отмечаю", "do", "check", "call", "send", "buy", "pay", "bring"
        ).any { it in lower }
    }

    private fun isWeakCloudAction(cloudAction: String, offlineAction: String): Boolean {
        val cleanCloud = cloudAction.lowercase(Locale.getDefault()).trim(' ', '.', ',', ':', ';', '—', '-')
        val cleanOffline = offlineAction.lowercase(Locale.getDefault()).trim()
        if (cleanCloud in setOf("встреча", "событие", "дело", "задача", "напоминание")) return cleanOffline.length > cleanCloud.length + 6
        if (cleanOffline.contains("день") && !cleanCloud.contains("день")) return true
        return false
    }

    private fun hasRiskSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf("риск", "опасно", "проблем", "ошибка", "сломается", "risk", "danger", "problem", "error", "fail").any { it in lower }
    }

    private fun hasReminderSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "завтра", "сегодня", "послезавтра", "вечером", "утром", "днём", "днем", "через", "напомни", "напомнить",
            "tomorrow", "today", "remind", "morning", "evening"
        ).any { it in lower } || Regex("\\b\\d{1,2}[:.]\\d{2}\\b").containsMatchIn(lower)
    }

    private fun buildCardTitle(text: String, intent: String): String {
        val prefix = when (intent) {
            "RISK" -> "Риск"
            "REMINDER" -> "Напоминание"
            "TASK" -> "Задача"
            else -> "Мысль"
        }
        val clean = text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return "$prefix: ${clean.take(64)}"
    }

    private fun buildWhyText(
        language: String,
        intent: String,
        hasAction: Boolean,
        hasRisk: Boolean,
        hasReminder: Boolean,
        dateSignal: DateSignal?,
        cloudResult: AiAnalysisResult?
    ): String {
        val signals = mutableListOf<String>()
        signals.add("ИИ: ${if (cloudResult != null) "Yandex AI + локальная проверка" else "локальный офлайн-редактор"}")
        signals.add("язык: $language")
        signals.add("тип: ${humanIntent(intent)}")
        if (hasAction) signals.add("действие найдено")
        if (hasRisk) signals.add("есть сигнал риска")
        if (hasReminder) signals.add("есть сигнал времени/напоминания")
        if (dateSignal != null) signals.add("когда: ${dateSignal.label}")
        if (cloudResult?.reason?.isNotBlank() == true) signals.add("причина ИИ: ${cloudResult.reason.take(90)}")
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
        DateTimeParser.parse(lower, nowMillis)?.let { return it }

        parseDateWordWithOptionalTime(lower, nowMillis)?.let { return it }
        parseExplicitTime(lower, nowMillis)?.let { return it }
        return null
    }

    private fun parseDateWordWithOptionalTime(text: String, nowMillis: Long): DateSignal? {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dateText = when {
            "послезавтра" in text -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                "послезавтра"
            }
            "завтра" in text || "tomorrow" in text -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                "завтра"
            }
            "сегодня" in text || "today" in text -> "сегодня"
            else -> null
        } ?: return null

        val exactTime = findClockTime(text) ?: findSpokenClockTime(text)
        if (exactTime != null) {
            calendar.set(Calendar.HOUR_OF_DAY, exactTime.hour)
            calendar.set(Calendar.MINUTE, exactTime.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return DateSignal("$dateText ${exactTime.label}", dateText, exactTime.label, calendar.timeInMillis)
        }

        val timeSignal = detectTimeOfDay(text)
        applyTimeOfDay(calendar, timeSignal)
        return DateSignal(buildDateLabel(dateText, timeSignal.label), dateText, timeSignal.label, calendar.timeInMillis)
    }

    private fun parseExplicitTime(text: String, nowMillis: Long): DateSignal? {
        val time = findClockTime(text) ?: findSpokenClockTime(text) ?: return null
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dateText = if (calendar.timeInMillis > nowMillis) "сегодня" else {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            "завтра"
        }
        return DateSignal("$dateText ${time.label}", dateText, time.label, calendar.timeInMillis)
    }

    private fun findClockTime(text: String): ClockTime? {
        val match = Regex("(?:\\bв\\s*)?(\\d{1,2})[:.](\\d{2})\\b").find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return ClockTime(hour, minute, "%02d:%02d".format(hour, minute))
    }

    private fun findSpokenClockTime(text: String): ClockTime? {
        val match = Regex("\\b(?:в\\s+)?(\\d{1,2}|час|один|одна|одну|два|две|три|четыре|пять|шесть|семь|восемь|девять|десять|одиннадцать|двенадцать)(?:\\s+час(?:а|ов)?)?\\s+(дня|вечера|утра|ночи)\\b", RegexOption.IGNORE_CASE).find(text) ?: return null
        val rawHour = match.groupValues[1].toIntOrNull() ?: when (match.groupValues[1].lowercase(Locale.getDefault()).replace('ё', 'е')) {
            "час", "один", "одна", "одну" -> 1
            "два", "две" -> 2
            "три" -> 3
            "четыре" -> 4
            "пять" -> 5
            "шесть" -> 6
            "семь" -> 7
            "восемь" -> 8
            "девять" -> 9
            "десять" -> 10
            "одиннадцать" -> 11
            "двенадцать" -> 12
            else -> return null
        }
        val period = match.groupValues[2].lowercase(Locale.getDefault())
        val hour = when (period) {
            "дня", "вечера" -> if (rawHour in 1..11) rawHour + 12 else rawHour
            "ночи" -> if (rawHour == 12) 0 else rawHour
            "утра" -> if (rawHour == 12) 0 else rawHour
            else -> rawHour
        }
        if (hour !in 0..23) return null
        return ClockTime(hour, 0, "%02d:00".format(hour))
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

    private fun extractActionText(text: String): String {
        return DateTimeParser.removeRelativeDuration(text)
            .replace(Regex("(?i)https?://\\S+"), "")
            .replace(Regex("(?i)\\bнапомни(ть)?\\b"), "")
            .replace(Regex("(?i)\\bмне\\b"), "")
            .replace(Regex("(?i)\\bпожалуйста\\b"), "")
            .replace(Regex("\\b(сегодня|завтра|послезавтра)\\b"), "")
            .replace(Regex("\\b(утром|днём|днем|вечером)\\b"), "")
            .replace(Regex("\\b(?:в\\s+)?(\\d{1,2}|час|один|одна|одну|два|две|три|четыре|пять|шесть|семь|восемь|девять|десять|одиннадцать|двенадцать)(?:\\s+час(?:а|ов)?)?\\s+(дня|вечера|утра|ночи)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:\\bв\\s*)?\\d{1,2}[:.]\\d{2}\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', '—', ':', ';')
            .removePrefix("нужно ")
            .removePrefix("надо ")
            .removePrefix("сделать ")
            .trim()
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

data class ClockTime(
    val hour: Int,
    val minute: Int,
    val label: String
)

data class QuickCaptureResult(
    val captureId: Long,
    val analysisId: Long,
    val cardId: Long,
    val title: String,
    val whyText: String
)
