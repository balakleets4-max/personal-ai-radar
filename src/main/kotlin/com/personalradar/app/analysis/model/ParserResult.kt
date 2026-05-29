package com.personalradar.app.analysis.model

data class ParserResult(
    val language: String,
    val entities: List<ParsedEntityDraft>,
    val warnings: List<ParserWarning> = emptyList()
)
