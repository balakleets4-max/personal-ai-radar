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
