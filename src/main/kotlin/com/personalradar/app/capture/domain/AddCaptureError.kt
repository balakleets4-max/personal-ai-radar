package com.personalradar.app.capture.domain

sealed class AddCaptureError(message: String) : Throwable(message) {
    data object EmptyText : AddCaptureError("Capture text is empty")
    data class TextTooLong(val maxLength: Int) : AddCaptureError("Capture text is longer than $maxLength characters")
}
