package com.personalradar.app.calendar

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CalendarSourceReader(private val context: Context) {
    fun readUpcomingEvents(daysAhead: Int = 28, limit: Int = 120): List<CalendarSourceEvent> {
        val now = System.currentTimeMillis()
        val start = startOfToday(now)
        val end = now + TimeUnit.DAYS.toMillis(daysAhead.toLong().coerceAtLeast(1L))
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        val projection = arrayOf(
            COL_EVENT_ID,
            COL_CALENDAR_ID,
            COL_CALENDAR_DISPLAY_NAME,
            COL_TITLE,
            COL_DESCRIPTION,
            COL_EVENT_LOCATION,
            COL_BEGIN,
            COL_END,
            COL_ALL_DAY
        )

        val sortOrder = "$COL_BEGIN ASC"
        val events = mutableListOf<CalendarSourceEvent>()

        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext() && events.size < limit) {
                val title = cursor.getStringSafe(COL_TITLE).trim()
                if (title.isBlank()) continue

                val beginMillis = cursor.getLongSafe(COL_BEGIN)
                val daysFromNow = ((beginMillis - now).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1L)).toInt()

                events.add(
                    CalendarSourceEvent(
                        eventId = cursor.getLongSafe(COL_EVENT_ID),
                        calendarId = cursor.getLongSafe(COL_CALENDAR_ID),
                        calendarName = cursor.getStringSafe(COL_CALENDAR_DISPLAY_NAME).ifBlank { "Календарь" },
                        title = title,
                        description = cursor.getStringSafe(COL_DESCRIPTION).trim(),
                        location = cursor.getStringSafe(COL_EVENT_LOCATION).trim(),
                        beginMillis = beginMillis,
                        endMillis = cursor.getLongSafe(COL_END),
                        allDay = cursor.getIntSafe(COL_ALL_DAY) == 1,
                        controlMode = CalendarControlMode.fromDaysFromNow(daysFromNow)
                    )
                )
            }
        }

        return events
    }

    private fun startOfToday(nowMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun Cursor.getStringSafe(columnName: String): String {
        val index = getColumnIndex(columnName)
        if (index < 0 || isNull(index)) return ""
        return getString(index).orEmpty()
    }

    private fun Cursor.getLongSafe(columnName: String): Long {
        val index = getColumnIndex(columnName)
        if (index < 0 || isNull(index)) return 0L
        return getLong(index)
    }

    private fun Cursor.getIntSafe(columnName: String): Int {
        val index = getColumnIndex(columnName)
        if (index < 0 || isNull(index)) return 0
        return getInt(index)
    }

    companion object {
        private const val COL_EVENT_ID = "event_id"
        private const val COL_CALENDAR_ID = "calendar_id"
        private const val COL_CALENDAR_DISPLAY_NAME = "calendar_displayName"
        private const val COL_TITLE = "title"
        private const val COL_DESCRIPTION = "description"
        private const val COL_EVENT_LOCATION = "eventLocation"
        private const val COL_BEGIN = "begin"
        private const val COL_END = "end"
        private const val COL_ALL_DAY = "allDay"
    }
}

data class CalendarSourceEvent(
    val eventId: Long,
    val calendarId: Long,
    val calendarName: String,
    val title: String,
    val description: String,
    val location: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val controlMode: CalendarControlMode
) {
    fun stableKey(): String {
        return "calendar:$calendarId:$eventId:$beginMillis"
    }

    fun displayWhenText(): String {
        val dateFormatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val dayFormatter = SimpleDateFormat("dd.MM", Locale.getDefault())
        return if (allDay) "весь день ${dayFormatter.format(Date(beginMillis))}" else dateFormatter.format(Date(beginMillis))
    }

    fun displayClockText(): String? {
        if (allDay) return null
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(beginMillis))
    }

    fun toRadarCaptureText(): String {
        val locationText = location.takeIf { it.isNotBlank() }?.let { "; место: $it" }.orEmpty()
        val descriptionText = description.takeIf { it.isNotBlank() }?.let { "; описание: ${it.take(120)}" }.orEmpty()
        return "Календарь: $title; когда: ${displayWhenText()}; контроль: ${controlMode.label}$locationText$descriptionText"
    }

    fun toPreviewText(): String {
        val place = location.takeIf { it.isNotBlank() }?.let { "\n  Место: $it" }.orEmpty()
        return "• $title\n  Когда: ${displayWhenText()}\n  Контроль: ${controlMode.label}$place"
    }
}

enum class CalendarControlMode(val label: String, val priority: Int) {
    ACTIVE("активный контроль", 5),
    MEDIUM("средний контроль", 4),
    WEAK("слабый контроль", 3),
    BACKGROUND("фоновый обзор", 2);

    companion object {
        fun fromDaysFromNow(days: Int): CalendarControlMode {
            return when {
                days < 7 -> ACTIVE
                days < 14 -> MEDIUM
                days < 21 -> WEAK
                else -> BACKGROUND
            }
        }
    }
}
