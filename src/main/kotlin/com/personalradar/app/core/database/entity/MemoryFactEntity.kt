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
