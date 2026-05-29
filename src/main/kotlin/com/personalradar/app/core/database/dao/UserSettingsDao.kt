package com.personalradar.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.personalradar.app.core.database.entity.UserSettingsEntity

@Dao
interface UserSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: UserSettingsEntity)

    @Query("SELECT value FROM user_settings WHERE key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM user_settings ORDER BY key ASC")
    fun observeSettings(): Flow<List<UserSettingsEntity>>
}
