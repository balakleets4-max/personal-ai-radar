package com.personalradar.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_capabilities",
    indices = [
        Index(value = ["capability"], unique = true),
        Index("category"),
        Index("state"),
        Index("checkedAt")
    ]
)
data class DeviceCapabilityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capability: String,
    val category: String,
    val state: String,
    val checkedAt: Long,
    val explanation: String,
    val canRequest: Boolean
)
