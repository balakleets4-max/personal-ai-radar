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

        val afterPurposeMarker = extractAfterLastMarker(
            text,
            listOf(
                "о том что",
                "о том, что",
                "чтобы",
                "что мне надо",
                "что мне нужно",
                "что надо",
                "что нужно"
            )
        )
        if (afterPurposeMarker.isNotBlank()) text = afterPurposeMarker

        text = removeNoise(text)
        text = normalizeActionStart(text)
        text = cleanup(text)

        if (text.isBlank()) {
            text = fallbackFromOriginal(rawText)
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

    private fun removeNoise(text: String): String {
        var result = text
        val patterns = listOf(
            "так хочу поставить напоминание",
            "хочу поставить напоминание",
            "поставить напоминание",
            "поставь напоминание",
            "создать напоминание",
            "сделай напоминание",
            "должен сработать напоминание",
            "должно сработать напоминание",
            "сработать напоминание",
            "напоминание должно сработать",
            "напоминание о том что",
            "напоминание о том, что",
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
        patterns.forEach { phrase ->
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

    private fun cleanup(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.!?;:])"), "$1")
            .trim(' ', ',', '.', '-', '—', ':', ';')
            .trim()
    }

    private fun extractAfterLastMarker(text: String, markers: List<String>): String {
        var bestIndex = -1
        var bestMarker = ""
        markers.forEach { marker ->
            val index = text.lastIndexOf(marker)
            if (index >= 0 && index > bestIndex) {
                bestIndex = index
                bestMarker = marker
            }
        }
        return if (bestIndex >= 0) {
            text.substring(bestIndex + bestMarker.length).trim()
        } else {
            ""
        }
    }

    private fun fallbackFromOriginal(rawText: String): String {
        return cleanup(DateTimeParser.removeRelativeDuration(rawText))
    }

    private fun ensureDot(text: String): String {
        val clean = text.trim()
        return if (clean.endsWith(".") || clean.endsWith("!") || clean.endsWith("?")) clean else "$clean."
    }
}
