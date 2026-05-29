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
