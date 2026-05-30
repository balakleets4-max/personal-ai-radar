package com.personalradar.app.di

import android.content.Context
import androidx.room.Room
import com.personalradar.app.core.database.AppDatabase
import com.personalradar.app.quick.CaptureRadarController
import com.personalradar.app.quick.QuickCaptureRepository

class AppContainer private constructor(
    val database: AppDatabase,
    val quickCaptureRepository: QuickCaptureRepository,
    val captureRadarController: CaptureRadarController
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
            val repository = QuickCaptureRepository(database)
            val controller = CaptureRadarController(database, repository)
            return AppContainer(database, repository, controller)
        }
    }
}
