package com.personalradar.app.ai

import android.content.Context

class AiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    fun getSettings(): AiSettings {
        return AiSettings(
            cloudAnalysisEnabled = prefs.getBoolean(KEY_CLOUD_ENABLED, false),
            provider = prefs.getString(KEY_PROVIDER, PROVIDER_YANDEX_AI) ?: PROVIDER_YANDEX_AI,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            allowManualText = prefs.getBoolean(KEY_ALLOW_MANUAL_TEXT, true),
            allowSharedText = prefs.getBoolean(KEY_ALLOW_SHARED_TEXT, true),
            allowConversations = prefs.getBoolean(KEY_ALLOW_CONVERSATIONS, false)
        )
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun setCloudAnalysisEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_ENABLED, enabled).apply()
    }

    fun setAllowManualText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MANUAL_TEXT, enabled).apply()
    }

    fun setAllowSharedText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_SHARED_TEXT, enabled).apply()
    }

    fun setAllowConversations(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_CONVERSATIONS, enabled).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        const val PROVIDER_YANDEX_AI = "Yandex AI"
        private const val KEY_CLOUD_ENABLED = "cloud_enabled"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_ALLOW_MANUAL_TEXT = "allow_manual_text"
        private const val KEY_ALLOW_SHARED_TEXT = "allow_shared_text"
        private const val KEY_ALLOW_CONVERSATIONS = "allow_conversations"
    }
}

data class AiSettings(
    val cloudAnalysisEnabled: Boolean,
    val provider: String,
    val apiKey: String,
    val allowManualText: Boolean,
    val allowSharedText: Boolean,
    val allowConversations: Boolean
) {
    val hasApiKey: Boolean = apiKey.isNotBlank()
}
