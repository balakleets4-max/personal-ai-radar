package com.personalradar.app.calendar

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

object CalendarSyncNotifier {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyCalendarSyncFinished() {
        mainHandler.post {
            listeners.forEach { listener -> listener.invoke() }
        }
    }
}
