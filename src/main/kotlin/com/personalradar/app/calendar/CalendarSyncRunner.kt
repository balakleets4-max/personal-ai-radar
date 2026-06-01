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
        scheduleReminderAlarms(appContainer, result.createdCardIds)
        rescheduleReminderAlarms(appContainer, result.updatedCardIds)
        removeReminderAlarmsForMissingCards(appContainer, result)
        CalendarSyncNotifier.notifyCalendarSyncFinished()
        result
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun scheduleReminderAlarms(appContainer: AppContainer, cardIds: List<Long>) {
        cardIds.forEach { cardId ->
            val card = appContainer.database.radarCardDao().getCardById(cardId) ?: return@forEach
            appContainer.reminderScheduler.schedule(card) as? ReminderScheduleResult.Scheduled
        }
    }

    private suspend fun rescheduleReminderAlarms(appContainer: AppContainer, cardIds: List<Long>) {
        cardIds.forEach { cardId ->
            appContainer.reminderScheduler.cancel(cardId)
            val card = appContainer.database.radarCardDao().getCardById(cardId) ?: return@forEach
            appContainer.reminderScheduler.schedule(card) as? ReminderScheduleResult.Scheduled
        }
    }

    private fun removeReminderAlarmsForMissingCards(appContainer: AppContainer, result: CalendarImportResult) {
        result.archivedMissingCardIds.forEach { cardId ->
            appContainer.reminderScheduler.cancel(cardId)
        }
    }
}
