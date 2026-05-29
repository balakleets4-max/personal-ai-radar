package com.personalradar.app.analysis.parser

import com.personalradar.app.analysis.model.ParserResult

interface ParserEngine {
    fun parse(text: String, language: String, createdAt: Long): ParserResult
}
