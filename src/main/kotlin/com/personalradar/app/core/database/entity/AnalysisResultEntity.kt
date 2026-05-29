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
