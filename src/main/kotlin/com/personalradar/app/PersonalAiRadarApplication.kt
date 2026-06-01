package com.personalradar.app

import android.app.Application
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.calendar.CalendarChangeObserverRegistry
import com.personalradar.app.reliability.BackgroundReliabilityStore
import com.personalradar.app.reliability.PersistentWatchService
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid

class PersonalAiRadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initCrashMonitoring()
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
        if (BackgroundReliabilityStore(applicationContext).isPersistentWatchEnabled()) {
            PersistentWatchService.start(applicationContext)
        }
    }

    private fun initCrashMonitoring() {
        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (dsn.isBlank()) return

        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.tracesSampleRate = 0.0
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isSendDefaultPii = false
            options.beforeBreadcrumb = { breadcrumb, _ ->
                val category = breadcrumb.category.orEmpty()
                if (
                    category.contains("ui", ignoreCase = true) ||
                    category.contains("navigation", ignoreCase = true)
                ) {
                    null
                } else {
                    breadcrumb
                }
            }
            options.beforeSend = { event, _ ->
                event.message = null
                event.breadcrumbs.clear()
                event.setTag("app_mode", "offline_first")
                event.level = event.level ?: SentryLevel.ERROR
                event
            }
        }
    }
}
