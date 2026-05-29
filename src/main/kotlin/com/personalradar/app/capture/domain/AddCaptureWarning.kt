package com.personalradar.app.capture.domain

sealed class AddCaptureWarning {
    data object LowConfidence : AddCaptureWarning()
    data object NoRadarCardCreated : AddCaptureWarning()
    data object DuplicateCardUpdated : AddCaptureWarning()
    data object UnknownLanguage : AddCaptureWarning()
    data class ParserPartialResult(val reason: String) : AddCaptureWarning()
    data class ParserFailedButCaptureSaved(val reason: String) : AddCaptureWarning()
}
