package com.personalradar.app.quick

import java.util.Locale

object OfflineTextPolisher {
    fun polishAction(rawText: String, dateSignal: DateSignal?): String {
        var text = rawText
            .lowercase(Locale.getDefault())
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()

        text = DateTimeParser.removeRelativeDuration(text)
        text = removeCalendarWords(text)
        text = removeGenericReminderOutcome(text)

        val afterPurposeMarker = extractAfterLastUsefulMarker(text)
        if (afterPurposeMarker.isNotBlank()) text = afterPurposeMarker

        text = removeNoise(text)
        text = normalizeActionStart(text)
        text = normalizeKnownBadPhrases(text)
        text = cleanup(text)

        if (text.isBlank()) {
            text = fallbackFromOriginal(rawText, dateSignal)
        }

        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun buildOfflineNotification(action: String): String {
        val clean = cleanup(action).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        if (clean.isBlank()) return "Напоминаю: откройте Личный ИИ-Радар."
        return when {
            clean.startsWith("Напоминаю:", ignoreCase = true) -> ensureDot(clean)
            clean.startsWith("Пора ", ignoreCase = true) -> "Напоминаю: ${clean.replaceFirstChar { it.lowercase() }}."
            clean.startsWith("Нужно ", ignoreCase = true) -> "Напоминаю: ${clean.replaceFirstChar { it.lowercase() }}."
            clean.startsWith("Надо ", ignoreCase = true) -> "Напоминаю: нужно ${clean.removePrefix("Надо ").replaceFirstChar { it.lowercase() }}."
            else -> "Напоминаю: нужно ${clean.replaceFirstChar { it.lowercase() }}."
        }
    }

    private fun removeCalendarWords(text: String): String {
        return text
            .replace(Regex("(?i)\\b(сегодня|завтра|послезавтра)\\b"), " ")
            .replace(Regex("(?i)\\b(утром|днем|днём|вечером)\\b"), " ")
            .replace(Regex("(?i)(?:\\bв\\s*)?\\d{1,2}[:.]\\d{2}\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun removeGenericReminderOutcome(text: String): String {
        return text
            .replace(Regex("(?i)\\bчтобы\\s+оно\\s+(?:сработало|сработает|сыграло|сыграет)\\b"), " ")
            .replace(Regex("(?i)\\bчтоб\\s+оно\\s+(?:сработало|сработает|сыграло|сыграет)\\b"), " ")
            .replace(Regex("(?i)\\bоно\\s+(?:сработало|сработает|сыграло|сыграет)\\b"), " ")
            .replace(Regex("(?i)\\b(?:должно|должен|должна)\\s+(?:сработать|сыграть)\\b"), " ")
            .replace(Regex("(?i)\\b(?:сработать|сработали|сработало|сработает|сыграть|сыграли|сыграло|сыграет)\\s+(?:уведомление|уведомления|напоминание|напоминания)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun removeNoise(text: String): String {
        var result = text
        val patterns = listOf(
            "так хочу поставить напоминание",
            "хочу поставить напоминание",
            "поставить напоминание",
            "поставь напоминание",
            "создать напоминание",
            "сделай напоминание",
            "пусть сыграет напоминание",
            "пусть сработает напоминание",
            "пускай сыграет напоминание",
            "пускай сработает напоминание",
            "должен сработать напоминание",
            "должно сработать напоминание",
            "сработать напоминание",
            "сработать уведомление",
            "сработать уведомления",
            "сработали уведомления",
            "сработало уведомление",
            "сработает напоминание",
            "сработает уведомление",
            "сработает уведомления",
            "сыграет напоминание",
            "сыграет уведомление",
            "сыграет уведомления",
            "напоминание должно сработать",
            "напоминание должно сработает",
            "уведомление должно сработать",
            "уведомление должно сработает",
            "напоминание о том что",
            "напоминание о том, что",
            "напоминание что",
            "уведомление что",
            "напомни мне",
            "напомнить мне",
            "напомни",
            "напомнить",
            "пожалуйста",
            "короче",
            "значит",
            "ну",
            "в общем",
            "как бы"
        )
        patterns.sortedByDescending { it.length }.forEach { phrase ->
            result = result.replace(Regex("(?i)(^|\\s)" + Regex.escape(phrase) + "(\\s|$)"), " ")
        }
        return result
    }

    private fun normalizeActionStart(text: String): String {
        return text
            .removePrefix("мне надо ")
            .removePrefix("мне нужно ")
            .removePrefix("надо ")
            .removePrefix("нужно ")
            .removePrefix("что ")
            .removePrefix("чтобы ")
            .trim()
    }

    private fun normalizeKnownBadPhrases(text: String): String {
        return text
            .replace(Regex("(?i)\\bпоставить\\s+воду\\s+кипятиться\\s+в\\s+чайник\\b"), "вскипятить воду в чайнике")
            .replace(Regex("(?i)\\bпоставить\\s+воду\\s+кипятится\\s+в\\s+чайник\\b"), "вскипятить воду в чайнике")
            .replace(Regex("(?i)\\bпоставить\\s+кипятиться\\s+воду\\s+в\\s+чайник\\b"), "вскипятить воду в чайнике")
            .replace(Regex("(?i)\\bпоставить\\s+кипятится\\s+воду\\s+в\\s+чайник\\b"), "вскипятить воду в чайнике")
            .replace(Regex("(?i)\\bпоставить\\s+чайник\\s+с\\s+водой\\b"), "вскипятить воду в чайнике")
            .replace(Regex("(?i)\\bпоставить\\s+кипятиться\\s+воду\\b"), "вскипятить воду")
            .replace(Regex("(?i)\\bпоставить\\s+кипятится\\s+воду\\b"), "вскипятить воду")
            .replace(Regex("(?i)\\bпоставить\\s+воду\\s+кипятиться\\b"), "вскипятить воду")
            .replace(Regex("(?i)\\bпоставить\\s+воду\\s+кипятится\\b"), "вскипятить воду")
            .replace(Regex("(?i)\\bпоставить\\s+греться\\s+воду\\b"), "поставить воду греться")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanup(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.!?;:])"), "\$1")
            .trim(' ', ',', '.', '-', '—', ':', ';')
            .trim()
    }

    private fun extractAfterLastUsefulMarker(text: String): String {
        val markers = listOf(
            "о том что",
            "о том, что",
            "что мне надо",
            "что мне нужно",
            "что надо",
            "что нужно"
        )
        var bestIndex = -1
        var bestMarker = ""
        markers.forEach { marker ->
            val index = text.lastIndexOf(marker)
            if (index >= 0 && index > bestIndex) {
                bestIndex = index
                bestMarker = marker
            }
        }
        if (bestIndex < 0) return ""
        return cleanup(text.substring(bestIndex + bestMarker.length))
    }

    private fun fallbackFromOriginal(rawText: String, dateSignal: DateSignal?): String {
        val clean = cleanup(
            normalizeKnownBadPhrases(
                removeNoise(
                    removeGenericReminderOutcome(
                        removeCalendarWords(DateTimeParser.removeRelativeDuration(rawText.lowercase(Locale.getDefault()).replace('ё', 'е')))
                    )
                )
            )
        )
        if (clean.isNotBlank()) return normalizeActionStart(clean)
        return if (dateSignal != null) "проверить напоминание" else cleanup(rawText)
    }

    private fun ensureDot(text: String): String {
        val clean = text.trim()
        return if (clean.endsWith(".") || clean.endsWith("!") || clean.endsWith("?")) clean else "$clean."
    }
}
