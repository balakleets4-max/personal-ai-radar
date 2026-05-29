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
