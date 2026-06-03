package com.personalradar.app.quick

import java.util.Calendar
import java.util.Locale

object DateTimeParser {
    fun parse(text: String, nowMillis: Long): DateSignal? {
        val lower = normalize(text)
        parseAbsoluteDateTime(lower, nowMillis)?.let { return it }
        parseNamedMonthDateRange(lower, nowMillis)?.let { return it }
        parseNamedMonthDateTime(lower, nowMillis)?.let { return it }
        parseRelativeDuration(lower, nowMillis)?.let { return it }
        return null
    }

    fun removeRelativeDuration(text: String): String {
        return RELATIVE_PHRASE_REGEX.replace(normalize(text), " ")
            .replace(ABSOLUTE_DATE_TIME_REGEX, " ")
            .replace(NAMED_MONTH_DATE_RANGE_REGEX, " ")
            .replace(NAMED_MONTH_DATE_TIME_REGEX, " ")
            .replace(SPOKEN_CLOCK_TIME_REGEX, " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', '—', ':', ';')
    }

    private fun parseAbsoluteDateTime(text: String, nowMillis: Long): DateSignal? {
        val match = ABSOLUTE_DATE_TIME_REGEX.find(text) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val rawYear = match.groupValues.getOrNull(3).orEmpty()
        val detectedTime = findClockTime(text) ?: findSpokenClockTime(text)
        val hour = match.groupValues.getOrNull(4).orEmpty().toIntOrNull() ?: detectedTime?.hour ?: DEFAULT_HOUR
        val minute = match.groupValues.getOrNull(5).orEmpty().toIntOrNull() ?: detectedTime?.minute ?: DEFAULT_MINUTE

        return buildAbsoluteDateSignal(day, month, rawYear, hour, minute, nowMillis)
    }

    private fun parseNamedMonthDateRange(text: String, nowMillis: Long): DateSignal? {
        val match = NAMED_MONTH_DATE_RANGE_REGEX.find(text) ?: return null
        val startDay = match.groupValues[1].toIntOrNull() ?: return null
        val endDay = match.groupValues[2].toIntOrNull() ?: return null
        val month = parseMonthName(match.groupValues[3]) ?: return null
        val rawYear = match.groupValues.getOrNull(4).orEmpty()
        val detectedTime = findClockTime(text) ?: findSpokenClockTime(text)
        val hour = match.groupValues.getOrNull(5).orEmpty().toIntOrNull() ?: detectedTime?.hour ?: DEFAULT_HOUR
        val minute = match.groupValues.getOrNull(6).orEmpty().toIntOrNull() ?: detectedTime?.minute ?: DEFAULT_MINUTE
        if (startDay !in 1..31 || endDay !in 1..31 || startDay > endDay || month !in 1..12 || hour !in 0..23 || minute !in 0..59) return null

        val startSignal = buildAbsoluteDateSignal(startDay, month, rawYear, hour, minute, nowMillis) ?: return null
        val endYear = startSignal.dateText?.takeLast(4).orEmpty()
        val endSignal = buildAbsoluteDateSignal(endDay, month, endYear, hour, minute, nowMillis) ?: return null
        val label = "${startSignal.dateText}–${endSignal.dateText} ${startSignal.timeText}"
        return DateSignal(
            label = label,
            dateText = "${startSignal.dateText}–${endSignal.dateText}",
            timeText = startSignal.timeText,
            timestampMillis = startSignal.timestampMillis
        )
    }

    private fun parseNamedMonthDateTime(text: String, nowMillis: Long): DateSignal? {
        val match = NAMED_MONTH_DATE_TIME_REGEX.find(text) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = parseMonthName(match.groupValues[2]) ?: return null
        val rawYear = match.groupValues.getOrNull(3).orEmpty()
        val detectedTime = findClockTime(text) ?: findSpokenClockTime(text)
        val hour = match.groupValues.getOrNull(4).orEmpty().toIntOrNull() ?: detectedTime?.hour ?: DEFAULT_HOUR
        val minute = match.groupValues.getOrNull(5).orEmpty().toIntOrNull() ?: detectedTime?.minute ?: DEFAULT_MINUTE

        return buildAbsoluteDateSignal(day, month, rawYear, hour, minute, nowMillis)
    }

    private fun buildAbsoluteDateSignal(
        day: Int,
        month: Int,
        rawYear: String,
        hour: Int,
        minute: Int,
        nowMillis: Long
    ): DateSignal? {
        if (day !in 1..31 || month !in 1..12 || hour !in 0..23 || minute !in 0..59) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val year = when {
            rawYear.isBlank() -> now.get(Calendar.YEAR)
            rawYear.length == 2 -> 2000 + rawYear.toInt()
            else -> rawYear.toIntOrNull() ?: now.get(Calendar.YEAR)
        }

        val calendar = Calendar.getInstance().apply {
            clear()
            isLenient = false
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            calendar.timeInMillis
        } catch (_: IllegalArgumentException) {
            return null
        }

        if (rawYear.isBlank() && calendar.timeInMillis < nowMillis - ONE_DAY_MILLIS) {
            calendar.add(Calendar.YEAR, 1)
        }

        val resolvedYear = calendar.get(Calendar.YEAR)
        val dateText = "%02d.%02d.%04d".format(day, month, resolvedYear)
        val timeText = "%02d:%02d".format(hour, minute)
        return DateSignal(
            label = "$dateText $timeText",
            dateText = dateText,
            timeText = timeText,
            timestampMillis = calendar.timeInMillis
        )
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

        val hasPreciseTimeUnit = parts.any { part ->
            part.unit == TimeUnitName.HOUR || part.unit == TimeUnitName.MINUTE || part.unit == TimeUnitName.SECOND
        }
        val explicitTime = if (hasPreciseTimeUnit) null else findClockTime(text) ?: findSpokenClockTime(text)
        val timeText = when {
            hasPreciseTimeUnit -> null
            explicitTime != null -> {
                calendar.set(Calendar.HOUR_OF_DAY, explicitTime.hour)
                calendar.set(Calendar.MINUTE, explicitTime.minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                explicitTime.label
            }
            else -> {
                calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR)
                calendar.set(Calendar.MINUTE, DEFAULT_MINUTE)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                DEFAULT_TIME_TEXT
            }
        }

        val label = buildLabel(parts)
        return DateSignal(
            label = if (timeText == null) label else "$label $timeText",
            dateText = label,
            timeText = timeText,
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

    private fun parseMonthName(token: String): Int? {
        return when (token.trim('.', ',', ';', ':').lowercase(Locale.getDefault())) {
            "января", "январь" -> 1
            "февраля", "февраль" -> 2
            "марта", "март" -> 3
            "апреля", "апрель" -> 4
            "мая", "май" -> 5
            "июня", "июнь" -> 6
            "июля", "июль" -> 7
            "августа", "август" -> 8
            "сентября", "сентябрь" -> 9
            "октября", "октябрь" -> 10
            "ноября", "ноябрь" -> 11
            "декабря", "декабрь" -> 12
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
        return normalizeRussianOrdinalDates(
            text
                .lowercase(Locale.getDefault())
                .replace('ё', 'е')
        )
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeRussianOrdinalDates(text: String): String {
        return ORDINAL_NAMED_MONTH_REGEX.replace(text) { match ->
            val day = parseOrdinalDayWords(match.groupValues[1]) ?: return@replace match.value
            "$day ${match.groupValues[2]}"
        }
    }

    private fun parseOrdinalDayWords(value: String): Int? {
        val tokens = value.lowercase(Locale.getDefault()).replace('ё', 'е').trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return null
        if (tokens.size == 1) return ORDINAL_DAY_WORDS[tokens[0]]
        if (tokens.size == 2) {
            val tens = when (tokens[0]) {
                "двадцать" -> 20
                "тридцать" -> 30
                else -> return null
            }
            val unit = ORDINAL_DAY_WORDS[tokens[1]] ?: return null
            return tens + unit
        }
        return null
    }

    private fun findClockTime(text: String): ClockTime? {
        val match = Regex("(?:\\bв\\s*)?(\\d{1,2})[:.](\\d{2})\\b").find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return ClockTime(hour, minute, "%02d:%02d".format(hour, minute))
    }

    private fun findSpokenClockTime(text: String): ClockTime? {
        val match = SPOKEN_CLOCK_TIME_REGEX.find(text) ?: return null
        val rawHour = parseSpokenHour(match.groupValues[1]) ?: return null
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

    private fun parseSpokenHour(value: String): Int? {
        return value.toIntOrNull() ?: HOUR_WORDS[value.lowercase(Locale.getDefault()).replace('ё', 'е')]
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

    private const val DEFAULT_HOUR = 9
    private const val DEFAULT_MINUTE = 0
    private const val DEFAULT_TIME_TEXT = "09:00"
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

    private val WORD_NUMBERS = mapOf(
        "ноль" to 0,
        "один" to 1,
        "одна" to 1,
        "одно" to 1,
        "одну" to 1,
        "одного" to 1,
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

    private const val MONTH_NAMES = "января|январь|февраля|февраль|марта|март|апреля|апрель|мая|май|июня|июнь|июля|июль|августа|август|сентября|сентябрь|октября|октябрь|ноября|ноябрь|декабря|декабрь"
    private const val ORDINAL_DAY_WORD_PATTERN = "первого|второго|третьего|четвертого|пятого|шестого|седьмого|восьмого|девятого|десятого|одиннадцатого|двенадцатого|тринадцатого|четырнадцатого|пятнадцатого|шестнадцатого|семнадцатого|восемнадцатого|девятнадцатого|двадцатого|тридцатого|двадцать\\s+(?:первого|второго|третьего|четвертого|пятого|шестого|седьмого|восьмого|девятого)|тридцать\\s+первого"
    private val ORDINAL_NAMED_MONTH_REGEX = Regex("\\b($ORDINAL_DAY_WORD_PATTERN)\\s+($MONTH_NAMES)\\b", RegexOption.IGNORE_CASE)
    private val SPOKEN_CLOCK_TIME_REGEX = Regex("\\b(?:в\\s+)?(\\d{1,2}|час|один|одна|одну|два|две|три|четыре|пять|шесть|семь|восемь|девять|десять|одиннадцать|двенадцать)(?:\\s+час(?:а|ов)?)?\\s+(дня|вечера|утра|ночи)\\b", RegexOption.IGNORE_CASE)

    private val ORDINAL_DAY_WORDS = mapOf(
        "первого" to 1,
        "второго" to 2,
        "третьего" to 3,
        "четвертого" to 4,
        "пятого" to 5,
        "шестого" to 6,
        "седьмого" to 7,
        "восьмого" to 8,
        "девятого" to 9,
        "десятого" to 10,
        "одиннадцатого" to 11,
        "двенадцатого" to 12,
        "тринадцатого" to 13,
        "четырнадцатого" to 14,
        "пятнадцатого" to 15,
        "шестнадцатого" to 16,
        "семнадцатого" to 17,
        "восемнадцатого" to 18,
        "девятнадцатого" to 19,
        "двадцатого" to 20,
        "тридцатого" to 30
    )

    private val HOUR_WORDS = mapOf(
        "час" to 1,
        "один" to 1,
        "одна" to 1,
        "одну" to 1,
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
        "двенадцать" to 12
    )

    private val UNIT_PATTERN = "(?:секунд[ауые]?|сек|минут[ауые]?|мин|час(?:а|ов)?|день|дня|дней|дн[яей]*|недел[яюьиь]*|месяц(?:а|ев)?|мес|год|года|лет)"
    private val NUMBER_PATTERN = "(?:\\d+|ноль|один|одна|одно|одну|одного|два|две|три|четыре|пять|шесть|семь|восемь|девять|десять|одиннадцать|двенадцать|тринадцать|четырнадцать|пятнадцать|шестнадцать|семнадцать|восемнадцать|девятнадцать|двадцать|тридцать|сорок|пятьдесят|шестьдесят|семьдесят|восемьдесят|девяносто|сто|двести|триста|четыреста|пятьсот|шестьсот|семьсот|восемьсот|девятьсот)"
    private val DURATION_PART_PATTERN = "(?:(?:$NUMBER_PATTERN)(?:\\s+$NUMBER_PATTERN)*\\s+)?$UNIT_PATTERN"
    private val RELATIVE_PHRASE_REGEX = Regex(
        "через\\s+$DURATION_PART_PATTERN(?:\\s*(?:и|,)?\\s*$DURATION_PART_PATTERN)*",
        RegexOption.IGNORE_CASE
    )
    private val ABSOLUTE_DATE_TIME_REGEX = Regex(
        "\\b(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{2,4}))?(?:\\s*(?:в|,)?\\s*(\\d{1,2})[:.](\\d{2}))?\\b",
        RegexOption.IGNORE_CASE
    )
    private val NAMED_MONTH_DATE_RANGE_REGEX = Regex(
        "\\b(?:с\\s+)?(\\d{1,2})\\s*(?:-|–|—|по)\\s*(\\d{1,2})\\s+($MONTH_NAMES)(?:\\s+(\\d{2,4}))?(?:\\s*(?:в|,)?\\s*(\\d{1,2})[:.](\\d{2}))?\\b",
        RegexOption.IGNORE_CASE
    )
    private val NAMED_MONTH_DATE_TIME_REGEX = Regex(
        "\\b(\\d{1,2})\\s+($MONTH_NAMES)(?:\\s+(\\d{2,4}))?(?:\\s*(?:в|,)?\\s*(\\d{1,2})[:.](\\d{2}))?\\b",
        RegexOption.IGNORE_CASE
    )
}
