package com.personalradar.app.quick

import java.util.Calendar
import java.util.Locale

object DateTimeParser {
    fun parse(text: String, nowMillis: Long): DateSignal? {
        val lower = normalize(text)
        parseRelativeDuration(lower, nowMillis)?.let { return it }
        return null
    }

    fun removeRelativeDuration(text: String): String {
        return RELATIVE_PHRASE_REGEX.replace(normalize(text), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', '—', ':', ';')
    }

    private fun parseRelativeDuration(text: String, nowMillis: Long): DateSignal? {
        val match = RELATIVE_PHRASE_REGEX.find(text) ?: return null
        val phrase = match.value.trim()
        val body = phrase.removePrefix("через").trim()
        val parts = parseDurationParts(body)
        if (parts.isEmpty()) return null

        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        parts.forEach { part ->
            when (part.unit) {
                TimeUnitName.SECOND -> calendar.add(Calendar.SECOND, part.amount)
                TimeUnitName.MINUTE -> calendar.add(Calendar.MINUTE, part.amount)
                TimeUnitName.HOUR -> calendar.add(Calendar.HOUR_OF_DAY, part.amount)
                TimeUnitName.DAY -> calendar.add(Calendar.DAY_OF_YEAR, part.amount)
                TimeUnitName.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, part.amount)
                TimeUnitName.MONTH -> calendar.add(Calendar.MONTH, part.amount)
                TimeUnitName.YEAR -> calendar.add(Calendar.YEAR, part.amount)
            }
        }

        val label = buildLabel(parts)
        return DateSignal(
            label = label,
            dateText = label,
            timeText = null,
            timestampMillis = calendar.timeInMillis
        )
    }

    private fun parseDurationParts(text: String): List<DurationPart> {
        val tokens = text
            .replace(Regex("[,;]+"), " ")
            .replace(" и ", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val result = mutableListOf<DurationPart>()
        var index = 0
        while (index < tokens.size) {
            val amountParse = parseAmount(tokens, index)
            if (amountParse == null) {
                index++
                continue
            }

            val unitIndex = amountParse.nextIndex
            val unitToken = tokens.getOrNull(unitIndex)
            if (unitToken == null) {
                break
            }

            val unit = parseUnit(unitToken)
            if (unit == null) {
                index++
                continue
            }

            result.add(DurationPart(amountParse.amount, unit))
            index = unitIndex + 1
        }
        return mergeSameUnits(result)
    }

    private fun parseAmount(tokens: List<String>, start: Int): AmountParse? {
        val token = tokens.getOrNull(start) ?: return null
        token.toIntOrNull()?.let { return AmountParse(it, start + 1) }
        parseUnit(token)?.let { return AmountParse(1, start) }

        var amount = 0
        var index = start
        var consumed = false
        while (index < tokens.size) {
            val value = WORD_NUMBERS[tokens[index]] ?: break
            amount += value
            consumed = true
            index++
        }
        return if (consumed && amount > 0) AmountParse(amount, index) else null
    }

    private fun parseUnit(token: String): TimeUnitName? {
        val clean = token.trim('.', ',', ';', ':')
        return when {
            clean.startsWith("секунд") || clean == "сек" || clean == "секунда" || clean == "секунду" -> TimeUnitName.SECOND
            clean.startsWith("минут") || clean == "мин" || clean == "минута" || clean == "минуту" -> TimeUnitName.MINUTE
            clean.startsWith("час") -> TimeUnitName.HOUR
            clean.startsWith("дн") || clean == "день" -> TimeUnitName.DAY
            clean.startsWith("недел") || clean == "неделю" -> TimeUnitName.WEEK
            clean.startsWith("месяц") || clean.startsWith("мес") -> TimeUnitName.MONTH
            clean.startsWith("год") || clean.startsWith("лет") -> TimeUnitName.YEAR
            else -> null
        }
    }

    private fun mergeSameUnits(parts: List<DurationPart>): List<DurationPart> {
        val order = listOf(
            TimeUnitName.YEAR,
            TimeUnitName.MONTH,
            TimeUnitName.WEEK,
            TimeUnitName.DAY,
            TimeUnitName.HOUR,
            TimeUnitName.MINUTE,
            TimeUnitName.SECOND
        )
        return order.mapNotNull { unit ->
            val sum = parts.filter { it.unit == unit }.sumOf { it.amount }
            if (sum > 0) DurationPart(sum, unit) else null
        }
    }

    private fun buildLabel(parts: List<DurationPart>): String {
        return "через " + parts.joinToString(" ") { part ->
            "${part.amount} ${unitLabel(part.amount, part.unit)}"
        }
    }

    private fun unitLabel(amount: Int, unit: TimeUnitName): String {
        return when (unit) {
            TimeUnitName.SECOND -> plural(amount, "секунду", "секунды", "секунд")
            TimeUnitName.MINUTE -> plural(amount, "минуту", "минуты", "минут")
            TimeUnitName.HOUR -> plural(amount, "час", "часа", "часов")
            TimeUnitName.DAY -> plural(amount, "день", "дня", "дней")
            TimeUnitName.WEEK -> plural(amount, "неделю", "недели", "недель")
            TimeUnitName.MONTH -> plural(amount, "месяц", "месяца", "месяцев")
            TimeUnitName.YEAR -> plural(amount, "год", "года", "лет")
        }
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

    private fun normalize(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class AmountParse(val amount: Int, val nextIndex: Int)
    private data class DurationPart(val amount: Int, val unit: TimeUnitName)

    private enum class TimeUnitName {
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    private val WORD_NUMBERS = mapOf(
        "ноль" to 0,
        "один" to 1,
        "одна" to 1,
        "одно" to 1,
        "два" to 2,
        "две" to 2,
        "три" to 3,
        "четыре" to 4,
        "пять" to 5,
        "шесть" to 6,
        "семь" to 7,
        "восемь" to 8,
        "девять" to 9,
        "десять" to 10,
        "одиннадцать" to 11,
        "двенадцать" to 12,
        "тринадцать" to 13,
        "четырнадцать" to 14,
        "пятнадцать" to 15,
        "шестнадцать" to 16,
        "семнадцать" to 17,
        "восемнадцать" to 18,
        "девятнадцать" to 19,
        "двадцать" to 20,
        "тридцать" to 30,
        "сорок" to 40,
        "пятьдесят" to 50,
        "шестьдесят" to 60,
        "семьдесят" to 70,
        "восемьдесят" to 80,
        "девяносто" to 90,
        "сто" to 100,
        "двести" to 200,
        "триста" to 300,
        "четыреста" to 400,
        "пятьсот" to 500,
        "шестьсот" to 600,
        "семьсот" to 700,
        "восемьсот" to 800,
        "девятьсот" to 900
    )

    private val UNIT_PATTERN = "(?:секунд[ауые]?|сек|минут[ауые]?|мин|час(?:а|ов)?|день|дня|дней|дн[яей]*|недел[яюьиь]*|месяц(?:а|ев)?|мес|год|года|лет)"
    private val NUMBER_PATTERN = "(?:\\d+|ноль|один|одна|одно|два|две|три|четыре|пять|шесть|семь|восемь|девять|десять|одиннадцать|двенадцать|тринадцать|четырнадцать|пятнадцать|шестнадцать|семнадцать|восемнадцать|девятнадцать|двадцать|тридцать|сорок|пятьдесят|шестьдесят|семьдесят|восемьдесят|девяносто|сто|двести|триста|четыреста|пятьсот|шестьсот|семьсот|восемьсот|девятьсот)"
    private val DURATION_PART_PATTERN = "(?:(?:$NUMBER_PATTERN)(?:\\s+$NUMBER_PATTERN)*\\s+)?$UNIT_PATTERN"
    private val RELATIVE_PHRASE_REGEX = Regex(
        "через\\s+$DURATION_PART_PATTERN(?:\\s*(?:и|,)?\\s*$DURATION_PART_PATTERN)*",
        RegexOption.IGNORE_CASE
    )
}
