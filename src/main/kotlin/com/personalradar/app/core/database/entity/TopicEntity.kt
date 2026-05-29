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
