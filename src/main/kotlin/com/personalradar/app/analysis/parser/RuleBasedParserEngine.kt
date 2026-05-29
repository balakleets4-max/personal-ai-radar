package com.personalradar.app.analysis.parser

import com.personalradar.app.analysis.model.ParsedEntityDraft
import com.personalradar.app.analysis.model.ParserResult
import com.personalradar.app.core.model.ParsedEntityType

class RuleBasedParserEngine : ParserEngine {
    private val taskSignals = listOf("надо", "нужно", "не забыть", "надо бы", "сделать", "проверить")
    private val reminderSignals = listOf("напомни", "напомнить", "поставь напоминание", "не забыть")
    private val ideaSignals = listOf("идея", "можно сделать", "хорошо бы", "было бы хорошо", "вариант", "мысль")
    private val riskSignals = listOf("забыл", "забываю", "опять забуду", "могу забыть", "срочно", "проблема", "не успею", "горит")
    private val uncertainDates = listOf("потом", "на днях", "когда будет время", "как-нибудь", "позже")
    private val dateSignals = listOf("сегодня", "завтра", "послезавтра", "в понедельник", "во вторник", "в среду", "в четверг", "в пятницу", "в субботу", "в воскресенье")
    private val actions = listOf("позвонить", "написать", "отправить", "проверить", "купить", "сделать", "доделать", "закончить", "подготовить", "разобрать", "разобраться", "уточнить", "спросить", "найти", "сфотографировать", "описать", "зарисовать", "измерить", "упаковать")
    private val projectSignals = listOf("музей", "раскопки", "находки", "альбом иллюстраций", "документы", "отчёт", "отчет", "приложение", "ии-радар")

    override fun parse(text: String, language: String, createdAt: Long): ParserResult {
        val lower = text.lowercase()
        val entities = mutableListOf<ParsedEntityDraft>()

        fun addMatches(type: String, values: List<String>, confidence: Float) {
            values.forEach { value ->
                val index = lower.indexOf(value)
                if (index >= 0) {
                    entities.add(
                        ParsedEntityDraft(
                            type = type,
                            rawValue = text.substring(index, (index + value.length).coerceAtMost(text.length)),
                            normalizedValue = value,
                            startIndex = index,
                            endIndex = index + value.length,
                            confidence = confidence.coerceIn(0f, 1f)
                        )
                    )
                }
            }
        }

        addMatches(ParsedEntityType.TASK_SIGNAL, taskSignals, 0.85f)
        addMatches(ParsedEntityType.REMINDER_SIGNAL, reminderSignals, 0.9f)
        addMatches(ParsedEntityType.IDEA_SIGNAL, ideaSignals, 0.9f)
        addMatches(ParsedEntityType.RISK_SIGNAL, riskSignals, 0.85f)
        addMatches(ParsedEntityType.UNCERTAIN_DATE, uncertainDates, 0.65f)
        addMatches(ParsedEntityType.DATE, dateSignals, 0.85f)
        addMatches(ParsedEntityType.ACTION, actions, 0.85f)
        addMatches(ParsedEntityType.PROJECT_SIGNAL, projectSignals, 0.75f)
        addMatches(ParsedEntityType.TOPIC, projectSignals, 0.7f)

        Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b").findAll(text).forEach { match ->
            entities.add(
                ParsedEntityDraft(
                    type = ParsedEntityType.TIME,
                    rawValue = match.value,
                    normalizedValue = match.value,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    confidence = 0.95f
                )
            )
        }

        // Очень осторожное PERSON-правило: только после понятных глаголов общения.
        Regex("(?:позвонить|написать|спросить)\\s+([А-ЯЁ][а-яё]+)").find(text)?.let { match ->
            val name = match.groupValues[1]
            val start = match.range.first + match.value.indexOf(name)
            entities.add(
                ParsedEntityDraft(
                    type = ParsedEntityType.PERSON,
                    rawValue = name,
                    normalizedValue = name.trim().replaceFirstChar { it.uppercaseChar() },
                    startIndex = start,
                    endIndex = start + name.length,
                    confidence = 0.75f
                )
            )
        }

        return ParserResult(language = language, entities = entities.distinctBy { it.type + it.startIndex + it.rawValue })
    }
}
