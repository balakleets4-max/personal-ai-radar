package com.personalradar.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personalradar.app.di.AppContainer
import com.personalradar.app.reminder.ReminderScheduleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarBackgroundWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext Result.success()

        try {
            val appContainer = AppContainer.get(applicationContext)
            val events = CalendarSourceReader(applicationContext).readUpcomingEvents(
                daysAhead = 14,
                limit = 60
            )
            val importResult = appContainer.calendarRadarImporter.importEvents(events)
            scheduleCreatedCards(appContainer, importResult)
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        } catch (_: Throwable) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun scheduleCreatedCards(appContainer: AppContainer, result: CalendarImportResult) {
        result.createdCardIds.forEach { cardId ->
            val card = appContainer.database.radarCardDao().getCardById(cardId) ?: return@forEach
            appContainer.reminderScheduler.schedule(card) as? ReminderScheduleResult.Scheduled
        }
    }

    companion object {
        const val WORK_NAME = "calendar_background_check"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
