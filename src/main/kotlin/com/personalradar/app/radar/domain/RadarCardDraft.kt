package com.personalradar.app.radar.domain

data class RadarCardDraft(
    val type: String,
    val title: String,
    val description: String,
    val whyText: String,
    val sourceQuote: String,
    val priority: Int,
    val confidence: Float,
    val dueAt: Long?,
    val dedupeKey: String?
)
