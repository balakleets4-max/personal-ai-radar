package com.personalradar.app.core.model

object UserSettingKey {
    const val PRIVACY_LOCAL_ONLY = "privacy_local_only"
    const val CLOUD_ENABLED = "cloud_enabled"
    const val MICROPHONE_ENABLED = "microphone_enabled"

    const val RADAR_MAX_CARDS_IMPORTANT = "radar_max_cards_important"
    const val RADAR_MAX_CARDS_TODAY = "radar_max_cards_today"
    const val RADAR_MAX_CARDS_HIDDEN_TASKS = "radar_max_cards_hidden_tasks"
    const val RADAR_MAX_CARDS_PROJECTS = "radar_max_cards_projects"
    const val RADAR_MAX_CARDS_REPEATING = "radar_max_cards_repeating"

    const val PARSER_MIN_CONFIDENCE_FOR_CARD = "parser_min_confidence_for_card"
    const val RADAR_DEFAULT_SNOOZE_DAYS = "radar_default_snooze_days"

    const val EVENT_LOG_RETENTION_DAYS_INFO = "event_log_retention_days_info"
    const val EVENT_LOG_RETENTION_DAYS_WARNING = "event_log_retention_days_warning"
    const val EVENT_LOG_RETENTION_DAYS_ERROR = "event_log_retention_days_error"

    val ALL = setOf(
        PRIVACY_LOCAL_ONLY,
        CLOUD_ENABLED,
        MICROPHONE_ENABLED,
        RADAR_MAX_CARDS_IMPORTANT,
        RADAR_MAX_CARDS_TODAY,
        RADAR_MAX_CARDS_HIDDEN_TASKS,
        RADAR_MAX_CARDS_PROJECTS,
        RADAR_MAX_CARDS_REPEATING,
        PARSER_MIN_CONFIDENCE_FOR_CARD,
        RADAR_DEFAULT_SNOOZE_DAYS,
        EVENT_LOG_RETENTION_DAYS_INFO,
        EVENT_LOG_RETENTION_DAYS_WARNING,
        EVENT_LOG_RETENTION_DAYS_ERROR
    )
}
