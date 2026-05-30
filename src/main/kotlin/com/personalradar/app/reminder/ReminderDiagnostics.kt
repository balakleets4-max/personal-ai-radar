package com.personalradar.app.reminder

import android.content.Context

class ReminderDiagnostics(private val context: Context) {
    fun recordScheduled(
        cardId: Long,
        dueAt: Long,
        notificationText: String,
        exact: Boolean
    ) {
        prefs.edit()
            .putLong("last_card_id", cardId)
            .putLong("last_due_at", dueAt)
            .putLong("last_scheduled_at", System.currentTimeMillis())
            .putString("last_notification_text", notificationText)
            .putBoolean("last_exact", exact)
            .putString("last_state", "scheduled")
            .apply()
    }

    fun recordShown(cardId: Long, notificationText: String) {
        prefs.edit()
            .putLong("last_shown_card_id", cardId)
            .putLong("last_shown_at", System.currentTimeMillis())
            .putString("last_shown_text", notificationText)
            .putString("last_state", "shown")
            .apply()
    }

    fun recordNotScheduled(cardId: Long?, reason: String) {
        prefs.edit()
            .putLong("last_failed_at", System.currentTimeMillis())
            .putLong("last_failed_card_id", cardId ?: 0L)
            .putString("last_failed_reason", reason)
            .putString("last_state", "not_scheduled")
            .apply()
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "personal_ai_radar_reminder_diagnostics"
    }
}
