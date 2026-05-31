package com.personalradar.app.reliability

import android.content.Context

class BackgroundReliabilityStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPersistentWatchEnabled(): Boolean {
        return prefs.getBoolean(KEY_PERSISTENT_WATCH_ENABLED, false)
    }

    fun setPersistentWatchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSISTENT_WATCH_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "background_reliability"
        private const val KEY_PERSISTENT_WATCH_ENABLED = "persistent_watch_enabled"
    }
}
