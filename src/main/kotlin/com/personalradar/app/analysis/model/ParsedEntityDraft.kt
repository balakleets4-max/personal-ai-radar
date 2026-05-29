package com.personalradar.app.analysis.model

data class ParsedEntityDraft(
    val type: String,
    val rawValue: String,
    val normalizedValue: String?,
    val startIndex: Int?,
    val endIndex: Int?,
    val confidence: Float
)
