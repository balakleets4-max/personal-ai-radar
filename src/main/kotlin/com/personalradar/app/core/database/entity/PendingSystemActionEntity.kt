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
