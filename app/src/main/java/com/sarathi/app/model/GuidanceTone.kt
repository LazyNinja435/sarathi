package com.sarathi.app.model

enum class GuidanceTone(val id: String, val label: String, val blurb: String) {
    Gentle("gentle", "Gentle", "We will untangle the heart together."),
    Direct("direct", "Direct", "Clarity comes through action."),
    Poetic("poetic", "Poetic", "The soul is not dimmed by passing clouds."),
    Scriptural("scriptural", "Scriptural", "Let us return to the Gita."),
    ;

    companion object {
        fun fromId(id: String?): GuidanceTone =
            entries.find { it.id == id } ?: Gentle
    }
}
