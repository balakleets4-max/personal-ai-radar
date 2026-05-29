package com.personalradar.app.analysis.analyzer

import com.personalradar.app.analysis.model.AnalysisDraft
import com.personalradar.app.analysis.model.ParserResult
import com.personalradar.app.core.model.IntentType
import com.personalradar.app.core.model.ParsedEntityType

class RuleBasedAnalysisEngine : AnalysisEngine {
    override fun analyze(rawText: String, language: String, parserResult: ParserResult, now: Long): AnalysisDraft {
        val types = parserResult.entities.map { it.type }.toSet()

        var taskScore = 0
        var reminderScore = 0
        var ideaScore = 0
        var riskScore = 0
        var projectScore = 0

        if (ParsedEntityType.TASK_SIGNAL in types) taskScore += 2
        if (ParsedEntityType.ACTION in types) taskScore += 3
        if (ParsedEntityType.DATE in types) reminderScore += 2
        if (ParsedEntityType.TIME in types) reminderScore += 3
        if (ParsedEntityType.REMINDER_SIGNAL in types) reminderScore += 5
        if (ParsedEntityType.RISK_SIGNAL in types) riskScore += 4
        if (ParsedEntityType.IDEA_SIGNAL in types) ideaScore += 5
        if (ParsedEntityType.PROJECT_SIGNAL in types) projectScore += 3
        if (ParsedEntityType.UNCERTAIN_DATE in types) riskScore += 1

        val mainIntent = when {
            reminderScore >= 6 && ParsedEntityType.REMINDER_SIGNAL in types -> IntentType.REMINDER
            ideaScore >= 5 && taskScore <= ideaScore + 2 -> IntentType.IDEA
            taskScore >= 5 -> IntentType.TASK
            riskScore >= 4 -> IntentType.RISK
            projectScore >= 4 -> IntentType.PROJECT_NOTE
            else -> IntentType.MEMORY_NOTE
        }

        val secondaryIntent = when {
            mainIntent == IntentType.REMINDER && taskScore >= 3 -> IntentType.TASK
            mainIntent == IntentType.IDEA && taskScore >= 3 -> IntentType.TASK
            mainIntent == IntentType.RISK && taskScore >= 3 -> IntentType.TASK
            mainIntent != IntentType.PROJECT_NOTE && projectScore >= 3 -> IntentType.PROJECT_NOTE
            else -> null
        }

        val maxScore = listOf(taskScore, reminderScore, ideaScore, riskScore, projectScore).maxOrNull() ?: 0
        var confidence = (maxScore / 8.0f).coerceIn(0f, 1f)
        if (ParsedEntityType.ACTION in types && ParsedEntityType.DATE in types) confidence += 0.1f
        if (ParsedEntityType.ACTION in types && ParsedEntityType.PERSON in types) confidence += 0.1f
        confidence = confidence.coerceIn(0f, 1f)

        val action = parserResult.entities.firstOrNull { it.type == ParsedEntityType.ACTION }?.normalizedValue
        val topic = parserResult.entities.firstOrNull { it.type == ParsedEntityType.TOPIC || it.type == ParsedEntityType.PROJECT_SIGNAL }?.normalizedValue
        val summary = buildString {
            if (action != null) append(action.replaceFirstChar { it.uppercaseChar() })
            if (topic != null) append(if (isEmpty()) topic else " — $topic")
            if (isEmpty()) append(rawText.take(80))
        }

        return AnalysisDraft(
            mainIntent = mainIntent,
            secondaryIntent = secondaryIntent,
            confidence = confidence,
            summary = summary,
            detectedDateText = parserResult.entities.firstOrNull { it.type == ParsedEntityType.DATE }?.rawValue,
            detectedTimeText = parserResult.entities.firstOrNull { it.type == ParsedEntityType.TIME }?.rawValue,
            normalizedDateTime = null, // v0.1.4: точная нормализация дат будет отдельным небольшим модулем.
            hasAction = ParsedEntityType.ACTION in types,
            hasRisk = ParsedEntityType.RISK_SIGNAL in types,
            hasPerson = ParsedEntityType.PERSON in types,
            hasReminderSignal = ParsedEntityType.REMINDER_SIGNAL in types,
            explanation = "Найдены признаки: " + types.joinToString()
        )
    }
}
