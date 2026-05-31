package com.personalradar.app

import android.app.Application
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.calendar.CalendarChangeObserverRegistry

class PersonalAiRadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
    }
}
