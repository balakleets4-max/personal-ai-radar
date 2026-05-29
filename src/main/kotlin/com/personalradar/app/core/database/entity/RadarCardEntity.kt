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
