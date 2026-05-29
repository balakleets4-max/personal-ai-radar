package com.personalradar.app.analysis.language

interface LanguageDetector {
    fun detect(text: String): String
}
