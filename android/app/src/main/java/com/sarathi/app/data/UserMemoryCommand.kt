package com.sarathi.app.data

object UserMemoryCommand {
    private val explicitPhrases = listOf(
        "remember this",
        "remember that",
        "remember:",
        "save this",
        "save that",
        "save:",
        "keep this in mind",
        "keep that in mind",
    )

    fun extractExplicitMemory(message: String): String? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase()
        val phrase = explicitPhrases.firstOrNull { lower.contains(it) } ?: return null
        val start = lower.indexOf(phrase) + phrase.length
        val raw = trimmed.substring(start)
            .trim(' ', ':', '-', '.', ',')
            .replace(Regex("\\s+"), " ")
        val note = when {
            raw.isNotBlank() -> raw
            phrase.endsWith("this") -> trimmed
            else -> ""
        }.trim()
        return note
            .take(180)
            .takeIf { it.length >= 3 }
    }
}
