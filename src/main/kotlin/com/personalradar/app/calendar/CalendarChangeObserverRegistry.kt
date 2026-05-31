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
    private val scheduleQuickCheck = Runnable {
        CalendarBackgroundScheduler(appContext).runOnceSoon()
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        mainHandler.removeCallbacks(scheduleQuickCheck)
        mainHandler.postDelayed(scheduleQuickCheck, DEBOUNCE_DELAY_MS)
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 2_000L
    }
}
