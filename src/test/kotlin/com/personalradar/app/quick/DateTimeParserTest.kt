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
        assertEquals("через 2 месяца 3 дня", result!!.label)

        val expected = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.MONTH, 2)
            add(Calendar.DAY_OF_YEAR, 3)
        }.timeInMillis
        assertEquals(expected, result.timestampMillis)
    }

    @Test
    fun removesRelativeDurationFromActionText() {
        val clean = DateTimeParser.removeRelativeDuration("Через минуту и 10 секунд напомни выпить воды")
        assertEquals("напомни выпить воды", clean)
    }
}
