package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = RadarCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["radarCardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("radarCardId"),
        Index("captureId"),
        Index("type"),
        Index("createdAt")
    ]
)
data class ActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val radarCardId: Long,
    val captureId: Long? = null,
    val type: String,
    val createdAt: Long,
    val value: String? = null,
    val comment: String? = null
)
