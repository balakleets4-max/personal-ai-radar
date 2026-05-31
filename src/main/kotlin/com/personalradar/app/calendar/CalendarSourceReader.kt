package com.personalradar.app.calendar

import android.content.ContentUris
import android.content.Context
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
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val events = mutableListOf<CalendarSourceEvent>()

        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val eventIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val calendarNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext() && events.size < limit) {
                val title = cursor.getString(titleIndex).orEmpty().trim()
                if (title.isBlank()) continue

                events.add(
                    CalendarSourceEvent(
                        eventId = cursor.getLong(eventIdIndex),
                        calendarId = cursor.getLong(calendarIdIndex),
                        calendarName = cursor.getString(calendarNameIndex).orEmpty().ifBlank { "Календарь" },
                        title = title,
                        description = cursor.getString(descriptionIndex).orEmpty().trim(),
                        location = cursor.getString(locationIndex).orEmpty().trim(),
                        beginMillis = cursor.getLong(beginIndex),
                        endMillis = cursor.getLong(endIndex),
                        allDay = cursor.getInt(allDayIndex) == 1
                    )
                )
            }
        }

        return events
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
