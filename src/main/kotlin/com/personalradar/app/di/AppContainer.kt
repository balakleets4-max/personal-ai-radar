package com.personalradar.app.di

import android.content.Context
import androidx.room.Room
import com.personalradar.app.ai.AiSettingsStore
import com.personalradar.app.ai.YandexAiClient
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.quick.CaptureRadarController
import com.personalradar.app.quick.QuickCaptureRepository
import com.personalradar.app.reminder.ReminderScheduler

class AppContainer private constructor(
    val database: AppDatabase,
    val quickCaptureRepository: QuickCaptureRepository,
    val captureRadarController: CaptureRadarController,
    val reminderScheduler: ReminderScheduler,
    val aiSettingsStore: AiSettingsStore,
    val yandexAiClient: YandexAiClient
) {
    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppContainer {
            val database = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "personal-ai-radar.db"
            ).build()
            val aiSettingsStore = AiSettingsStore(context)
            val yandexAiClient = YandexAiClient()
            val repository = QuickCaptureRepository(database, aiSettingsStore, yandexAiClient)
            val controller = CaptureRadarController(database, repository)
            val reminderScheduler = ReminderScheduler(context)
            return AppContainer(database, repository, controller, reminderScheduler, aiSettingsStore, yandexAiClient)
        }
    }
}
