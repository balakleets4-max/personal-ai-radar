package com.personalradar.app.capture.domain

data class AddCaptureInput(
    val rawText: String,
    val source: String = "TEXT"
)
