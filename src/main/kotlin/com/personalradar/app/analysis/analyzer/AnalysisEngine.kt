package com.personalradar.app.analysis.analyzer

import com.personalradar.app.analysis.model.AnalysisDraft
import com.personalradar.app.analysis.model.ParserResult

interface AnalysisEngine {
    fun analyze(rawText: String, language: String, parserResult: ParserResult, now: Long): AnalysisDraft
}
