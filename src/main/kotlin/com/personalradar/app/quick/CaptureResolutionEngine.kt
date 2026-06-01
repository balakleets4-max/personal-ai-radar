package com.personalradar.app.quick

import com.personalradar.app.core.database.entity.RadarCardEntity
import java.util.Locale

/**
 * Decides whether a new manual / voice capture looks like an update of an existing card.
 *
 * This intentionally runs before a new card is saved. It does not trust the card type returned
 * by cloud AI, because the same human intent can be classified as CALENDAR, REMINDER, TASK, etc.
 */
class CaptureResolutionEngine {
    fun findSimilarCapture(
        newText: String,
        activeCards: List<RadarCardEntity>
    ): ManualDuplicateCandidate? {
        val newFingerprint = CaptureFingerprint.from(newText)
        if (!newFingerprint.isUseful) return null

        val best = activeCards
            .filterNot { it.isImportedCalendarCard() }
            .mapNotNull { card ->
                val score = scoreCard(newFingerprint, card)
                if (score >= DUPLICATE_SCORE_THRESHOLD) card to score else null
            }
            .maxWithOrNull(compareBy<Pair<RadarCardEntity, Double>> { it.second }.thenBy { it.first.updatedAt })
            ?: return null

        return ManualDuplicateCandidate(
            existingCard = best.first,
            existingText = best.first.sourceQuote.ifBlank { best.first.title },
            newText = newText.trim()
        )
    }

    private fun scoreCard(newFingerprint: CaptureFingerprint, card: RadarCardEntity): Double {
        return card.resolutionTexts()
            .map { CaptureFingerprint.from(it) }
            .filter { it.isUseful }
            .map { oldFingerprint -> scoreFingerprints(newFingerprint, oldFingerprint) }
            .maxOrNull()
            ?: 0.0
    }

    private fun scoreFingerprints(new: CaptureFingerprint, old: CaptureFingerprint): Double {
        val tokenScore = tokenSimilarity(new.tokens, old.tokens)
        val topicOverlap = new.topicTokens.intersect(old.topicTokens).isNotEmpty()
        val specificOverlap = new.specificTokens.intersect(old.specificTokens).isNotEmpty()

        var score = tokenScore

        // Strong case: the user keeps the same specific context, for example
        // “встреча с клиентом магазина” -> “встреча с клиентом магазина перенеслась”.
        if (topicOverlap && specificOverlap) {
            score = maxOf(score, 0.82)
        }

        // Ambiguous but important case: “встреча с клиентом магазина...” -> “встреча перенеслась...”.
        // The second phrase lost the specific context, but update wording says it probably refers
        // to an existing card. We ask the user instead of silently creating a duplicate.
        if (new.hasUpdateSignal && topicOverlap) {
            score = maxOf(score, if (specificOverlap) 0.86 else 0.68)
        }

        // Same short core, one side contains more details. This catches “встреча” vs
        // “встреча с мэром” without hardcoding one concrete test phrase.
        if (topicOverlap && tokenScore >= CONTAINMENT_MATCH_SCORE) {
            score = maxOf(score, tokenScore)
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun tokenSimilarity(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left == right) return 1.0
        val overlap = left.intersect(right).size.toDouble()
        if (overlap <= 0.0) return 0.0
        val union = left.union(right).size.toDouble()
        val jaccard = overlap / union
        val containment = maxOf(overlap / left.size.toDouble(), overlap / right.size.toDouble())
        return maxOf(jaccard, containment * 0.92)
    }

    private fun RadarCardEntity.resolutionTexts(): List<String> {
        return listOf(sourceQuote, title, description, whyText)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun RadarCardEntity.isImportedCalendarCard(): Boolean {
        return radarEngineVersion.startsWith("calendar-radar") ||
            sourceQuote.startsWith("Календарь:", ignoreCase = true)
    }

    companion object {
        private const val DUPLICATE_SCORE_THRESHOLD = 0.62
        private const val CONTAINMENT_MATCH_SCORE = 0.62
    }
}

private data class CaptureFingerprint(
    val tokens: Set<String>,
    val topicTokens: Set<String>,
    val specificTokens: Set<String>,
    val hasUpdateSignal: Boolean
) {
    val isUseful: Boolean get() = tokens.isNotEmpty() && (topicTokens.isNotEmpty() || tokens.size >= 2)

    companion object {
        fun from(text: String): CaptureFingerprint {
            val normalizedText = text.lowercase(Locale.getDefault())
            val hasUpdateSignal = UPDATE_WORDS.any { it in normalizedText }
            val tokens = normalizedText
                .replace(Regex("\\b\\d{1,2}[:.]\\d{2}\\b"), " ")
                .replace(Regex("\\b\\d+\\b"), " ")
                .replace(Regex("[^а-яёa-z0-9]+"), " ")
                .split(' ')
                .mapNotNull { normalizeToken(it) }
                .filter { it.length > 2 }
                .toSet()
            val topicTokens = tokens.filter { it in TOPIC_TOKENS }.toSet()
            val specificTokens = tokens
                .filterNot { it in TOPIC_TOKENS }
                .filterNot { it in GENERIC_TOKENS }
                .toSet()
            return CaptureFingerprint(tokens, topicTokens, specificTokens, hasUpdateSignal)
        }

        private fun normalizeToken(rawToken: String): String? {
            val token = rawToken.trim()
            if (token.isBlank() || token in STOP_WORDS) return null
            return when {
                token.startsWith("встреч") || token.startsWith("встрет") -> "встреч"
                token.startsWith("созвон") -> "созвон"
                token.startsWith("звон") || token.startsWith("позвон") -> "звон"
                token.startsWith("куп") -> "куп"
                token.startsWith("заказ") || token.startsWith("закаж") -> "заказ"
                token.startsWith("запис") -> "запис"
                token.startsWith("врач") || token.startsWith("доктор") -> "врач"
                token.startsWith("музе") -> "музей"
                token.startsWith("работ") -> "работ"
                token.startsWith("клиент") -> "клиент"
                token.startsWith("магаз") -> "магазин"
                token.startsWith("мэр") || token.startsWith("мер") -> "мэр"
                token.startsWith("перенес") || token.startsWith("перенос") -> null
                token.startsWith("измени") || token.startsWith("поменя") -> null
                token.startsWith("сдвин") || token.startsWith("перестав") -> null
                token.startsWith("назнач") -> null
                token.startsWith("тест") -> "тест"
                else -> token
            }
        }

        private val TOPIC_TOKENS = setOf(
            "встреч", "созвон", "звон", "куп", "заказ", "запис", "врач", "музей", "работ", "тест"
        )

        private val GENERIC_TOKENS = setOf(
            "дело", "событие", "задача", "напоминание", "карточка"
        )

        private val UPDATE_WORDS = setOf(
            "перенес", "перенос", "перенести", "перенеслась", "перенесли",
            "измени", "изменить", "изменилась", "изменилось", "поменя", "поменять",
            "теперь", "вместо", "другое время", "другой срок", "сдвин", "перестав", "назнач"
        )

        private val STOP_WORDS = setOf(
            "это", "этот", "эта", "эту", "как", "для", "что", "чтобы", "или", "уже", "будет", "была", "был", "были",
            "в", "во", "на", "с", "со", "к", "ко", "по", "из", "от", "до", "при", "про", "без", "над", "под",
            "мне", "пожалуйста", "надо", "нужно", "необходимо", "там", "еще", "ещё", "снова",
            "напомни", "напомнить", "поставь", "поставить", "время", "было", "поставлено",
            "сегодня", "завтра", "послезавтра", "понедельник", "понедельника", "вторник", "вторника", "среда", "среду",
            "четверг", "четверга", "пятница", "пятницу", "суббота", "субботу", "воскресенье", "воскресенья",
            "утром", "днём", "днем", "вечером", "ночью",
            "ноль", "нуль", "один", "одна", "одно", "два", "две", "три", "четыре", "пять", "шесть", "семь",
            "восемь", "девять", "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
            "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать", "двадцать", "тридцать", "сорок", "пятьдесят",
            "час", "часа", "часов", "минут", "минута", "минуты",
            "причина", "локальная", "проверка", "действие", "тип", "язык", "когда", "ии", "yandex"
        )
    }
}
