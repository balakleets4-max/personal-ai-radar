package com.personalradar.app.analysis.language

import com.personalradar.app.core.model.LanguageCode

class BasicLanguageDetector : LanguageDetector {
    override fun detect(text: String): String {
        val letters = text.filter { it.isLetter() }
        if (letters.length < 3) return LanguageCode.UNKNOWN

        val cyrillic = letters.count { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
        val latin = letters.count { it in 'A'..'Z' || it in 'a'..'z' }
        val total = cyrillic + latin
        if (total == 0) return LanguageCode.UNKNOWN

        val cyrRatio = cyrillic.toFloat() / total
        val latRatio = latin.toFloat() / total

        return when {
            cyrRatio > 0.75f -> LanguageCode.RU
            latRatio > 0.75f -> LanguageCode.EN
            cyrillic > 0 && latin > 0 -> LanguageCode.MIXED
            else -> LanguageCode.UNKNOWN
        }
    }
}
