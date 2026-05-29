package com.personalradar.app.device.domain

data class CapabilityCheckResult(
    val capability: String,
    val category: String,
    val state: String,
    val explanation: String,
    val canRequest: Boolean
)
