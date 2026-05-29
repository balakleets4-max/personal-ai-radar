package com.personalradar.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personalradar.app.core.database.dao.*
import com.personalradar.app.core.database.entity.*

@Database(
    entities = [
        CaptureEntity::class,
        AnalysisResultEntity::class,
        ParsedEntityEntity::class,
        RadarCardEntity::class,
        ActionEntity::class,
        ReminderEntity::class,
        TopicEntity::class,
        CaptureTopicCrossRef::class,
        DeviceCapabilityEntity::class,
        MemoryFactEntity::class,
        ReflectionLogEntity::class,
        UserSettingsEntity::class,
        AppEventLogEntity::class,
        PendingSystemActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun parsedEntityDao(): ParsedEntityDao
    abstract fun radarCardDao(): RadarCardDao
    abstract fun actionDao(): ActionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun topicDao(): TopicDao
    abstract fun deviceCapabilityDao(): DeviceCapabilityDao
    abstract fun memoryFactDao(): MemoryFactDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun appEventLogDao(): AppEventLogDao
    abstract fun pendingSystemActionDao(): PendingSystemActionDao
}
