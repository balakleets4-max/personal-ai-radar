package com.personalradar.app.settings.usecase

import com.personalradar.app.core.database.dao.UserSettingsDao
import com.personalradar.app.core.database.entity.UserSettingsEntity
import com.personalradar.app.core.model.UserSettingKey
import com.personalradar.app.core.time.TimeProvider

class InitializeDefaultSettingsUseCase(
    private val userSettingsDao: UserSettingsDao,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val defaults = mapOf(
            UserSettingKey.PRIVACY_LOCAL_ONLY to "true",
            UserSettingKey.CLOUD_ENABLED to "false",
            UserSettingKey.MICROPHONE_ENABLED to "false",
            UserSettingKey.RADAR_MAX_CARDS_IMPORTANT to "5",
            UserSettingKey.RADAR_MAX_CARDS_TODAY to "7",
            UserSettingKey.RADAR_MAX_CARDS_HIDDEN_TASKS to "5",
            UserSettingKey.RADAR_MAX_CARDS_PROJECTS to "5",
            UserSettingKey.RADAR_MAX_CARDS_REPEATING to "3",
            UserSettingKey.PARSER_MIN_CONFIDENCE_FOR_CARD to "0.40",
            UserSettingKey.RADAR_DEFAULT_SNOOZE_DAYS to "1",
            UserSettingKey.EVENT_LOG_RETENTION_DAYS_INFO to "7",
            UserSettingKey.EVENT_LOG_RETENTION_DAYS_WARNING to "30",
            UserSettingKey.EVENT_LOG_RETENTION_DAYS_ERROR to "90"
        )
        val now = timeProvider.nowMillis()
        for ((key, value) in defaults) {
            if (userSettingsDao.getValue(key) == null) {
                userSettingsDao.upsertSetting(UserSettingsEntity(key = key, value = value, updatedAt = now))
            }
        }
    }
}
