package com.personalradar.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.personalradar.app.di.AppContainer
import com.personalradar.app.reminder.ReminderScheduleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarSyncRunner(
    private val context: Context
) {
    suspend fun syncUpcomingEvents(daysAhead: Int = 14, limit: Int = 60): CalendarImportResult? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (!hasCalendarPermission(appContext)) return@withContext null

        val appContainer = AppContainer.get(appContext)
        val events = CalendarSourceReader(appContext).readUpcomingEvents(
            daysAhead = daysAhead,
            limit = limit
        )
        val result = appContainer.calendarRadarImporter.importEvents(events)
        scheduleCreatedCards(appContainer, result)
        CalendarSyncNotifier.notifyCalendarSyncFinished()
        result
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun scheduleCreatedCards(appContainer: AppContainer, result: CalendarImportResult) {
        result.createdCardIds.forEach { cardId ->
            val card = appContainer.database.radarCardDao().getCardById(cardId) ?: return@forEach
            appContainer.reminderScheduler.schedule(card) as? ReminderScheduleResult.Scheduled
        }
    }
}
