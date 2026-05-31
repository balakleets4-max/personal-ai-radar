package com.personalradar.app

import android.app.Application
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.calendar.CalendarChangeObserverRegistry
import com.personalradar.app.reliability.BackgroundReliabilityStore
import com.personalradar.app.reliability.PersistentWatchService

class PersonalAiRadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
        if (BackgroundReliabilityStore(applicationContext).isPersistentWatchEnabled()) {
            PersistentWatchService.start(applicationContext)
        }
    }
}
