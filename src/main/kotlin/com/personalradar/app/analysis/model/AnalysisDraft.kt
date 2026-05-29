package com.personalradar.app.analysis.model

data class AnalysisDraft(
    val mainIntent: String,
    val secondaryIntent: String?,
    val confidence: Float,
    val summary: String,
    val detectedDateText: String?,
    val detectedTimeText: String?,
    val normalizedDateTime: Long?,
    val hasAction: Boolean,
    val hasRisk: Boolean,
    val hasPerson: Boolean,
    val hasReminderSignal: Boolean,
    val explanation: String
)
