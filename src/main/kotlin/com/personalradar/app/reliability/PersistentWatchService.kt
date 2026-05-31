package com.personalradar.app.reliability

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.personalradar.app.MainActivity
import com.personalradar.app.R
import com.personalradar.app.calendar.CalendarBackgroundScheduler
import com.personalradar.app.calendar.CalendarChangeObserverRegistry

class PersistentWatchService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            BackgroundReliabilityStore(applicationContext).setPersistentWatchEnabled(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        BackgroundReliabilityStore(applicationContext).setPersistentWatchEnabled(true)
        CalendarBackgroundScheduler(applicationContext).ensureScheduled()
        CalendarChangeObserverRegistry.ensureRegistered(applicationContext)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Фоновое наблюдение",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Постоянный режим для более надёжного подхвата важных событий."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PersistentWatchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AI Радар работает в фоне")
            .setContentText("Следит за разрешёнными источниками и готовит напоминания.")
            .setContentIntent(openIntent)
            .addAction(0, "Остановить", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.personalradar.app.action.STOP_PERSISTENT_WATCH"
        private const val CHANNEL_ID = "persistent_watch"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context) {
            val intent = Intent(context, PersistentWatchService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PersistentWatchService::class.java))
        }
    }
}
