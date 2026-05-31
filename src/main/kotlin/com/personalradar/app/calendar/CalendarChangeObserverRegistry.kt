package com.personalradar.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object CalendarChangeObserverRegistry {
    private var observer: CalendarChangeObserver? = null

    fun ensureRegistered(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!hasCalendarPermission(appContext)) return false
        if (observer != null) return true

        val calendarObserver = CalendarChangeObserver(appContext)
        appContext.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            calendarObserver
        )
        observer = calendarObserver
        return true
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private class CalendarChangeObserver(
    context: Context
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncNow = Runnable {
        syncScope.launch {
            runCatching { CalendarSyncRunner(appContext).syncUpcomingEvents() }
            CalendarBackgroundScheduler(appContext).runChangeFollowUpChecks()
        }
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        mainHandler.removeCallbacks(syncNow)
        mainHandler.postDelayed(syncNow, DEBOUNCE_DELAY_MS)
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 1_000L
    }
}
