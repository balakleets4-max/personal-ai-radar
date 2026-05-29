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
