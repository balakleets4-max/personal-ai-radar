package com.personalradar.app.analysis.model

sealed class ParserWarning {
    data class PartialResult(val reason: String) : ParserWarning()
}
