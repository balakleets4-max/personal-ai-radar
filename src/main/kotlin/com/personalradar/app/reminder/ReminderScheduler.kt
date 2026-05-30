package com.personalradar.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.personalradar.app.core.database.entity.RadarCardEntity

class ReminderScheduler(
    private val context: Context
) {
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
        if (dueAt <= System.currentTimeMillis()) {
            return ReminderScheduleResult.NotScheduled("Время уже прошло")
        }
        if (!canPostNotifications()) {
            return ReminderScheduleResult.NotScheduled("Нет разрешения на уведомления")
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(card, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
            return ReminderScheduleResult.Scheduled(dueAt, exact = true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        }

        return ReminderScheduleResult.Scheduled(dueAt, exact = false)
    }

    fun cancel(cardId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildCancelPendingIntent(cardId)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(card: RadarCardEntity, modeFlag: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_CARD_ID, card.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "Напоминание")
            putExtra(ReminderReceiver.EXTRA_TEXT, cleanNotificationText(card.title))
        }
        return PendingIntent.getBroadcast(
            context,
            card.id.toInt(),
            intent,
            modeFlag or PendingIntent.FLAG_IMMUTABLE
        )
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

    private fun cleanNotificationText(title: String): String {
        return title
            .removePrefix("Напоминание:")
            .removePrefix("Задача:")
            .removePrefix("Мысль:")
            .removePrefix("Риск:")
            .trim()
            .ifBlank { "Откройте Личный ИИ-Радар" }
    }
}

sealed class ReminderScheduleResult {
    data class Scheduled(val dueAt: Long, val exact: Boolean) : ReminderScheduleResult()
    data class NotScheduled(val reason: String) : ReminderScheduleResult()
}
