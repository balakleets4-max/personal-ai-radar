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
