package com.personalradar.app.capture.domain

data class AddCaptureResult(
    val captureId: Long,
    val analysisId: Long?,
    val createdCardIds: List<Long>,
    val updatedCardIds: List<Long>,
    val summary: String?,
    val detectedLanguage: String,
    val mainIntent: String?,
    val warnings: List<AddCaptureWarning>
)
