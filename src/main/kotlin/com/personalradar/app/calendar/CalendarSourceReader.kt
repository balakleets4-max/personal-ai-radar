package com.personalradar.app.calendar

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CalendarSourceReader(private val context: Context) {
    fun readUpcomingEvents(daysAhead: Int = 7, limit: Int = 20): List<CalendarSourceEvent> {
        val now = System.currentTimeMillis()
        val end = now + TimeUnit.DAYS.toMillis(daysAhead.toLong().coerceAtLeast(1L))
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
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

                events.add(
                    CalendarSourceEvent(
                        eventId = cursor.getLongSafe(COL_EVENT_ID),
                        calendarId = cursor.getLongSafe(COL_CALENDAR_ID),
                        calendarName = cursor.getStringSafe(COL_CALENDAR_DISPLAY_NAME).ifBlank { "Календарь" },
                        title = title,
                        description = cursor.getStringSafe(COL_DESCRIPTION).trim(),
                        location = cursor.getStringSafe(COL_EVENT_LOCATION).trim(),
                        beginMillis = cursor.getLongSafe(COL_BEGIN),
                        endMillis = cursor.getLongSafe(COL_END),
                        allDay = cursor.getIntSafe(COL_ALL_DAY) == 1
                    )
                )
            }
        }

        return events
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
    val allDay: Boolean
) {
    fun toRadarCaptureText(): String {
        val dateFormatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val dayFormatter = SimpleDateFormat("dd.MM", Locale.getDefault())
        val whenText = if (allDay) {
            "весь день ${dayFormatter.format(Date(beginMillis))}"
        } else {
            dateFormatter.format(Date(beginMillis))
        }
        val locationText = location.takeIf { it.isNotBlank() }?.let { "; место: $it" }.orEmpty()
        val descriptionText = description.takeIf { it.isNotBlank() }?.let { "; описание: ${it.take(120)}" }.orEmpty()
        return "Календарь: $title; когда: $whenText; источник: $calendarName$locationText$descriptionText"
    }
}
