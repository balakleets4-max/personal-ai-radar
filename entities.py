from pathlib import Path
root = Path('/mnt/data/personal_ai_radar_v014/src/main/kotlin/com/personalradar/app')
def write(rel, content):
    p=root/rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content.strip()+"\n", encoding='utf-8')

base='core/database/entity'
write(f'{base}/CaptureEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "captures",
    indices = [
        Index("createdAt"),
        Index("status"),
        Index("language"),
        Index("source")
    ]
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String,
    val language: String,
    val status: String,
    val userEdited: Boolean = false,
    val isFavorite: Boolean = false,
    val note: String? = null
)
''')
write(f'{base}/AnalysisResultEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_results",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("mainIntent"),
        Index("confidence"),
        Index("analyzedAt"),
        Index("parserVersion"),
        Index("analyzerVersion"),
        Index(value = ["captureId", "isLatest"])
    ]
)
data class AnalysisResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val captureId: Long,
    val analyzedAt: Long,
    val parserVersion: String,
    val analyzerVersion: String,
    val isLatest: Boolean = true,
    val language: String,
    val mainIntent: String,
    val secondaryIntent: String? = null,
    val confidence: Float,
    val summary: String,
    val detectedDateText: String? = null,
    val detectedTimeText: String? = null,
    val normalizedDateTime: Long? = null,
    val hasAction: Boolean = false,
    val hasRisk: Boolean = false,
    val hasPerson: Boolean = false,
    val hasReminderSignal: Boolean = false,
    val explanation: String
)
''')
write(f'{base}/ParsedEntityEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parsed_entities",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnalysisResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("analysisId"),
        Index("type"),
        Index("normalizedValue")
    ]
)
data class ParsedEntityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val captureId: Long,
    val analysisId: Long,
    val type: String,
    val rawValue: String,
    val normalizedValue: String? = null,
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val confidence: Float
)
''')
write(f'{base}/RadarCardEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "radar_cards",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnalysisResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("analysisId"),
        Index("type"),
        Index("status"),
        Index("priority"),
        Index("dueAt"),
        Index("createdAt"),
        Index("snoozedUntil"),
        Index("dedupeKey"),
        Index("radarEngineVersion"),
        Index(value = ["status", "priority", "dueAt", "createdAt"]),
        Index(value = ["status", "type", "priority", "createdAt"]),
        Index(value = ["status", "snoozedUntil"]),
        Index(value = ["dedupeKey", "status"])
    ]
)
data class RadarCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val captureId: Long,
    val analysisId: Long,
    val radarEngineVersion: String,
    val type: String,
    val title: String,
    val description: String,
    val whyText: String,
    val sourceQuote: String,
    val priority: Int,
    val confidence: Float,
    val status: String,
    val dueAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastShownAt: Long? = null,
    val shownCount: Int = 0,
    val duplicateHitCount: Int = 0,
    val snoozedUntil: Long? = null,
    val completedAt: Long? = null,
    val hiddenAt: Long? = null,
    val dedupeKey: String? = null,
    val hasReminder: Boolean = false
)
''')
write(f'{base}/ActionEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = RadarCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["radarCardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("radarCardId"),
        Index("captureId"),
        Index("type"),
        Index("createdAt")
    ]
)
data class ActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val radarCardId: Long,
    val captureId: Long? = null,
    val type: String,
    val createdAt: Long,
    val value: String? = null,
    val comment: String? = null
)
''')
write(f'{base}/ReminderEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = RadarCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["radarCardId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("radarCardId"),
        Index("captureId"),
        Index("scheduledAt"),
        Index("status"),
        Index("deliveryMode"),
        Index("schedulerState"),
        Index("updatedAt")
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val radarCardId: Long? = null,
    val captureId: Long? = null,
    val title: String,
    val scheduledAt: Long,
    val status: String,
    val deliveryMode: String,
    val schedulerState: String,
    val permissionStateAtCreation: String,
    val createdAt: Long,
    val updatedAt: Long,
    val firedAt: Long? = null,
    val cancelledAt: Long? = null
)
''')
write(f'{base}/TopicEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    indices = [
        Index(value = ["normalizedName"], unique = true),
        Index("type"),
        Index("lastSeenAt"),
        Index("seenCount"),
        Index("importanceScore")
    ]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val type: String,
    val createdAt: Long,
    val lastSeenAt: Long,
    val seenCount: Int = 1,
    val importanceScore: Float = 0.0f
)
''')
write(f'{base}/CaptureTopicCrossRef.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "capture_topic_cross_refs",
    primaryKeys = ["captureId", "topicId"],
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("captureId"), Index("topicId")]
)
data class CaptureTopicCrossRef(
    val captureId: Long,
    val topicId: Long,
    val confidence: Float
)
''')
write(f'{base}/DeviceCapabilityEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_capabilities",
    indices = [
        Index(value = ["capability"], unique = true),
        Index("category"),
        Index("state"),
        Index("checkedAt")
    ]
)
data class DeviceCapabilityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capability: String,
    val category: String,
    val state: String,
    val checkedAt: Long,
    val explanation: String,
    val canRequest: Boolean
)
''')
write(f'{base}/MemoryFactEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_facts",
    indices = [
        Index("type"),
        Index("key"),
        Index(value = ["type", "key"], unique = true),
        Index("confidence"),
        Index("updatedAt"),
        Index("expiresAt")
    ]
)
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val key: String,
    val value: String,
    val confidence: Float,
    val evidenceCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long? = null
)
''')
write(f'{base}/ReflectionLogEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reflection_logs",
    indices = [
        Index(value = ["date"], unique = true),
        Index("createdAt")
    ]
)
data class ReflectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val captureCount: Int,
    val radarCardCount: Int,
    val completedActionCount: Int,
    val hiddenCardCount: Int,
    val topTopicsJson: String,
    val createdAt: Long
)
''')
write(f'{base}/UserSettingsEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    indices = [Index(value = ["key"], unique = true)]
)
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val updatedAt: Long
)
''')
write(f'{base}/AppEventLogEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_event_logs",
    indices = [
        Index("createdAt"),
        Index("level"),
        Index("category")
    ]
)
data class AppEventLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val level: String,
    val category: String,
    val message: String,
    val relatedCaptureId: Long? = null,
    val relatedCardId: Long? = null,
    val relatedReminderId: Long? = null
)
''')
write(f'{base}/PendingSystemActionEntity.kt', r'''
package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_system_actions",
    indices = [
        Index("type"),
        Index("status"),
        Index("createdAt"),
        Index("relatedReminderId")
    ]
)
data class PendingSystemActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val relatedReminderId: Long? = null,
    val relatedCardId: Long? = null,
    val payloadJson: String,
    val attemptCount: Int = 0,
    val lastError: String? = null
)
''')
