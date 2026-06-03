package com.personalradar.app.quick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class DateTimeParserTest {
    private val baseMillis: Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.MAY)
        set(Calendar.DAY_OF_MONTH, 30)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun parsesOneMinute() {
        val result = DateTimeParser.parse("через минуту напомни выпить воды", baseMillis)
        assertNotNull(result)
        assertEquals("через 1 минуту", result!!.label)
        assertEquals(baseMillis + 60_000L, result.timestampMillis)
    }

    @Test
    fun parsesMinuteAndSeconds() {
        val result = DateTimeParser.parse("через минуту и 10 секунд напомни выпить воды", baseMillis)
        assertNotNull(result)
        assertEquals("через 1 минуту 10 секунд", result!!.label)
        assertEquals(baseMillis + 70_000L, result.timestampMillis)
    }

    @Test
    fun parsesHoursAndMinutes() {
        val result = DateTimeParser.parse("через 2 часа 5 минут позвонить маме", baseMillis)
        assertNotNull(result)
        assertEquals("через 2 часа 5 минут", result!!.label)
        assertEquals(baseMillis + 2 * 60 * 60_000L + 5 * 60_000L, result.timestampMillis)
    }

    @Test
    fun parsesDayHoursMinutesSeconds() {
        val result = DateTimeParser.parse("через 1 день 2 часа 30 минут 10 секунд проверить чай", baseMillis)
        assertNotNull(result)
        assertEquals("через 1 день 2 часа 30 минут 10 секунд", result!!.label)
        assertEquals(
            baseMillis + 24 * 60 * 60_000L + 2 * 60 * 60_000L + 30 * 60_000L + 10_000L,
            result.timestampMillis
        )
    }

    @Test
    fun parsesWrittenNumber() {
        val result = DateTimeParser.parse("через двадцать одну секунду проверить чай", baseMillis)
        assertNotNull(result)
        assertEquals("через 21 секунду", result!!.label)
        assertEquals(baseMillis + 21_000L, result.timestampMillis)
    }

    @Test
    fun parsesMonthAndDays() {
        val result = DateTimeParser.parse("через 2 месяца 3 дня позвонить", baseMillis)
        assertNotNull(result)
        assertEquals("через 2 месяца 3 дня 09:00", result!!.label)

        val expected = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.MONTH, 2)
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, result.timestampMillis)
    }

    @Test
    fun parsesNamedMonthDate() {
        val result = DateTimeParser.parse("съездить к родителям на дачу 26 августа", baseMillis)
        assertNotNull(result)
        assertEquals("26.08.2026 09:00", result!!.label)
        assertEquals("26.08.2026", result.dateText)
        assertEquals("09:00", result.timeText)

        val expected = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 26)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, result.timestampMillis)
    }

    @Test
    fun parsesNamedMonthDateWithClockTime() {
        val result = DateTimeParser.parse("съездить к родителям 26 августа в 17:05", baseMillis)
        assertNotNull(result)
        assertEquals("26.08.2026 17:05", result!!.label)
        assertEquals("17:05", result.timeText)
    }

    @Test
    fun rollsNamedMonthDateToNextYearWhenPast() {
        val septemberBase = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = DateTimeParser.parse("съездить к родителям на дачу 26 августа", septemberBase)
        assertNotNull(result)
        assertEquals("26.08.2027 09:00", result!!.label)
    }

    @Test
    fun parsesSpokenNamedMonthDateFromVoiceInput() {
        val result = DateTimeParser.parse("съездить к родителям на дачу двадцать шестого августа", baseMillis)
        assertNotNull(result)
        assertEquals("26.08.2026 09:00", result!!.label)
        assertEquals("26.08.2026", result.dateText)
        assertEquals("09:00", result.timeText)
    }

    @Test
    fun parsesFifthJanuaryWithSpokenAfternoonTime() {
        val result = DateTimeParser.parse("пятого января съездить к отцу на дачу и покопать грядки в час дня", baseMillis)
        assertNotNull(result)
        assertEquals("05.01.2027 13:00", result!!.label)
        assertEquals("13:00", result.timeText)
    }

    @Test
    fun parsesFifthFebruaryFromVoiceInput() {
        val result = DateTimeParser.parse("пятого февраля съездить к родителям", baseMillis)
        assertNotNull(result)
        assertEquals("05.02.2027 09:00", result!!.label)
    }

    @Test
    fun parsesThirtyFirstDecemberFromVoiceInput() {
        val result = DateTimeParser.parse("тридцать первого декабря проверить документы", baseMillis)
        assertNotNull(result)
        assertEquals("31.12.2026 09:00", result!!.label)
    }

    @Test
    fun parsesValentinesDayWithSpokenTimeAndDate() {
        val result = DateTimeParser.parse("день влюбленных час дня девятого октября", baseMillis)
        assertNotNull(result)
        assertEquals("09.10.2026 13:00", result!!.label)
        assertEquals("13:00", result.timeText)
    }

    @Test
    fun parsesNamedMonthDateRange() {
        val result = DateTimeParser.parse("день китайца с 27 по 29 января", baseMillis)
        assertNotNull(result)
        assertEquals("27.01.2027–29.01.2027 09:00", result!!.label)
        assertEquals("27.01.2027–29.01.2027", result.dateText)
        assertEquals("09:00", result.timeText)
    }

    @Test
    fun removesRelativeDurationFromActionText() {
        val clean = DateTimeParser.removeRelativeDuration("Через минуту и 10 секунд напомни выпить воды")
        assertEquals("напомни выпить воды", clean)
    }
}
