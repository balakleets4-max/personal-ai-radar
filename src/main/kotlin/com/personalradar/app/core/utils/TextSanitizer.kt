package com.personalradar.app.core.utils

fun String.toSourceQuote(): String {
    val clean = trim()
    return if (clean.length <= TextLimits.SOURCE_QUOTE_MAX_LENGTH) {
        clean
    } else {
        clean.take(TextLimits.SOURCE_QUOTE_MAX_LENGTH - 3).trimEnd() + "..."
    }
}

fun String.normalizeKeyPart(): String {
    return trim()
        .lowercase()
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")
        .trim('.', ',', ';', ':', '!', '?', '"', '\'', '«', '»')
        .ifBlank { "_" }
}

fun buildDedupeKey(
    cardType: String,
    action: String?,
    person: String?,
    topic: String?
): String {
    return listOf(cardType, action, person, topic)
        .map { (it ?: "_").normalizeKeyPart() }
        .joinToString(separator = ":")
}
