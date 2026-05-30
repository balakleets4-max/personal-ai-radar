package com.personalradar.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.personalradar.app.MainActivity
import com.personalradar.app.core.database.entity.RadarCardEntity

class ReminderScheduler(
    private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fallbackTimers = mutableMapOf<Long, Runnable>()

    fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(card: RadarCardEntity): ReminderScheduleResult {
        val dueAt = card.dueAt ?: return ReminderScheduleResult.NotScheduled("В карточке нет времени")
        val delayMs = dueAt - System.currentTimeMillis()
        if (delayMs <= 0L) {
            return ReminderScheduleResult.NotScheduled("Время уже прошло")
        }
        if (!canPostNotifications()) {
            return ReminderScheduleResult.NotScheduled("Нет разрешения на уведомления")
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            scheduleFallbackTimer(card, delayMs)
            return ReminderScheduleResult.Scheduled(dueAt, exact = false)
        }

        cancel(card.id)

        val pendingIntent = buildPendingIntent(card, PendingIntent.FLAG_UPDATE_CURRENT)
        if (delayMs <= ALARM_CLOCK_MAX_DELAY_MS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(dueAt, buildOpenAppPendingIntent(card.id)),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        }

        scheduleFallbackTimer(card, delayMs)

        return ReminderScheduleResult.Scheduled(dueAt, exact = true)
    }

    fun cancel(cardId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildCancelPendingIntent(cardId)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        fallbackTimers.remove(cardId)?.let { mainHandler.removeCallbacks(it) }
    }

    private fun scheduleFallbackTimer(card: RadarCardEntity, delayMs: Long) {
        if (delayMs > FALLBACK_MAX_DELAY_MS) return
        fallbackTimers.remove(card.id)?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            fallbackTimers.remove(card.id)
            context.sendBroadcast(buildReminderIntent(card))
        }
        fallbackTimers[card.id] = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun buildPendingIntent(card: RadarCardEntity, modeFlag: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            card.id.toInt(),
            buildReminderIntent(card),
            modeFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildOpenAppPendingIntent(cardId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            cardId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildReminderIntent(card: RadarCardEntity): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_CARD_ID, card.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "Напоминание")
            putExtra(ReminderReceiver.EXTRA_TEXT, buildHumanNotificationText(card))
        }
    }

    private fun buildCancelPendingIntent(cardId: Long): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            cardId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildHumanNotificationText(card: RadarCardEntity): String {
        val description = card.description.trim()
        if (description.isNotBlank() && card.whyText.contains("ИИ: Yandex AI")) {
            return normalizeAiNotification(description)
        }
        if (description.startsWith("Напоминаю:", ignoreCase = true) || description.startsWith("Пора ", ignoreCase = true)) {
            return normalizeAiNotification(description)
        }

        val titleAction = cleanActionText(card.title)
        val descriptionAction = cleanActionText(description)
        val action = when {
            descriptionAction.isNotBlank() -> descriptionAction
            titleAction.isNotBlank() -> titleAction
            description.isNotBlank() -> description
            else -> "откройте Личный ИИ-Радар"
        }
        return when {
            action.startsWith("Напоминаю:", ignoreCase = true) -> normalizeAiNotification(action)
            action.startsWith("пора ", ignoreCase = true) -> "Напоминаю: ${action.replaceFirstChar { it.lowercase() }}."
            else -> "Напоминаю: нужно $action."
        }
    }

    private fun normalizeAiNotification(text: String): String {
        val clean = text.trim().trimEnd('.', '!', '?')
        return when {
            clean.startsWith("Напоминаю:", ignoreCase = true) -> "$clean."
            clean.startsWith("Пора ", ignoreCase = true) -> "Напоминаю: ${clean.replaceFirstChar { it.lowercase() }}."
            else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + "."
        }
    }

    private fun cleanActionText(title: String): String {
        return title
            .removePrefix("Напоминание:")
            .removePrefix("Задача:")
            .removePrefix("Мысль:")
            .removePrefix("Риск:")
            .lowercase()
            .replace(Regex("\\bнапомни(ть)?\\b"), "")
            .replace(Regex("\\bнапоминаю:?\\b"), "")
            .replace(Regex("\\bмне\\b"), "")
            .replace(Regex("через\\s+\\d+\\s*(секунд[уы]?|минут[уы]?|час(?:а|ов)?|дн(?:я|ей|ь)?)"), "")
            .replace(Regex("через\\s+(секунду|минуту|час|день)"), "")
            .replace(Regex("\\s+и\\s+\\d+\\s*секунд[уы]?"), "")
            .replace(Regex("\\b(сегодня|завтра|послезавтра)\\b"), "")
            .replace(Regex("\\b(утром|днём|днем|вечером)\\b"), "")
            .replace(Regex("(?:\\bв\\s*)?\\d{1,2}[:.]\\d{2}\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', '-', '—', ':', ';')
            .removePrefix("нужно ")
            .removePrefix("надо ")
            .removePrefix("сделать ")
            .trim()
    }

    companion object {
        private const val FALLBACK_MAX_DELAY_MS = 5 * 60 * 1000L
        private const val ALARM_CLOCK_MAX_DELAY_MS = 10 * 60 * 1000L
    }
}

sealed class ReminderScheduleResult {
    data class Scheduled(val dueAt: Long, val exact: Boolean) : ReminderScheduleResult()
    data class NotScheduled(val reason: String) : ReminderScheduleResult()
}
