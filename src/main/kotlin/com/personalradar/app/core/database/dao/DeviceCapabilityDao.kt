package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.DeviceCapabilityEntity

@Dao
interface DeviceCapabilityDao {
    @Insert
    suspend fun insertCapability(capability: DeviceCapabilityEntity): Long

    @Query("SELECT * FROM device_capabilities WHERE capability = :capability LIMIT 1")
    suspend fun getCapability(capability: String): DeviceCapabilityEntity?

    @Query("""
        UPDATE device_capabilities
        SET state = :state, checkedAt = :checkedAt, explanation = :explanation, canRequest = :canRequest
        WHERE capability = :capability
    """)
    suspend fun updateCapabilityState(capability: String, state: String, checkedAt: Long, explanation: String, canRequest: Boolean)

    @Query("SELECT * FROM device_capabilities ORDER BY category ASC, capability ASC")
    fun observeCapabilities(): Flow<List<DeviceCapabilityEntity>>

    @Query("SELECT state FROM device_capabilities WHERE capability = :capability LIMIT 1")
    suspend fun getCapabilityState(capability: String): String?
}
