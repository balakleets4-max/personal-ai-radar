package com.personalradar.app

import android.app.Application
import com.personalradar.app.calendar.CalendarBackgroundScheduler

class PersonalAiRadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
    }
}
