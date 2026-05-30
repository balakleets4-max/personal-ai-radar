package com.personalradar.app.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.personalradar.app.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val cardId = intent.getLongExtra(EXTRA_CARD_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Напоминание"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "Откройте Личный ИИ-Радар"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            cardId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(cardId.toInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания Радара",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления по карточкам Личного ИИ-Радара"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "personal_ai_radar_reminders"
        const val EXTRA_CARD_ID = "extra_card_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
    }
}
