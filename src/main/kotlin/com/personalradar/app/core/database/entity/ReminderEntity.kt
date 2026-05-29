package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = RadarCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["radarCardId"],
            onDelete = ForeignKey.SET_NULL
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
        Index("scheduledAt"),
        Index("status"),
        Index("deliveryMode"),
        Index("schedulerState"),
        Index("updatedAt")
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val radarCardId: Long? = null,
    val captureId: Long? = null,
    val title: String,
    val scheduledAt: Long,
    val status: String,
    val deliveryMode: String,
    val schedulerState: String,
    val permissionStateAtCreation: String,
    val createdAt: Long,
    val updatedAt: Long,
    val firedAt: Long? = null,
    val cancelledAt: Long? = null
)
