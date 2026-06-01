package com.personalradar.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.calendar.CalendarChangeObserverRegistry
import com.personalradar.app.reliability.BackgroundReliabilityStore
import com.personalradar.app.reliability.PersistentWatchService
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

class PersonalAiRadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        safeStartup("sentry_init") { initCrashMonitoring() }
        safeStartup("calendar_background_schedule") {
            CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        }
        safeStartup("calendar_observer_register") {
            CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
        }
        safeStartup("persistent_watch_start") {
            if (BackgroundReliabilityStore(applicationContext).isPersistentWatchEnabled()) {
                PersistentWatchService.start(applicationContext)
            }
        }
    }

    private fun initCrashMonitoring() {
        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (dsn.isNotBlank()) {
            SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.environment = if (BuildConfig.DEBUG) "debug" else "release"
                options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                options.tracesSampleRate = 0.0
                options.isAttachScreenshot = false
                options.isAttachViewHierarchy = false
                options.isSendDefaultPii = false
            }
        }

        FirebaseCrashlytics.getInstance().setCustomKey("app_mode", "offline_first")
        FirebaseCrashlytics.getInstance().setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
    }

    private fun safeStartup(stepName: String, block: () -> Unit) {
        runCatching(block).onFailure { throwable ->
            runCatching {
                Sentry.captureException(throwable)
            }
            runCatching {
                FirebaseCrashlytics.getInstance().setCustomKey("startup_step", stepName)
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }
}
