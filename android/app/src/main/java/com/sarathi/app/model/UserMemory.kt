package com.sarathi.app.model

data class UserMemory(
    val preferredLanguageLevel: String = "simple",
    val preferredResponseStyle: String = "quote_simple_meaning_practical_guidance",
    val spiritualTonePreference: String = "krishna_inspired_practical",
    val savedUserNotes: List<String> = emptyList(),
) {
    fun summaryLines(maxNotes: Int = 5): List<String> = buildList {
        add("Preferred language level: $preferredLanguageLevel.")
        add("Preferred response style: $preferredResponseStyle.")
        add("Spiritual tone preference: $spiritualTonePreference.")
        savedUserNotes.take(maxNotes).forEach { note ->
            if (note.isNotBlank()) add("Saved note: $note")
        }
    }
}
