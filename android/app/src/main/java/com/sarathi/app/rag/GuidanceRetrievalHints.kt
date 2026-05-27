package com.sarathi.app.rag

object GuidanceRetrievalHints {
    data class VerseRef(val chapter: Int, val verse: Int)

    data class Hint(
        val verseRefs: List<VerseRef> = emptyList(),
        val queryTerms: List<String> = emptyList(),
    )

    fun forMessage(message: String): Hint {
        val lower = message.lowercase()
        return when {
            lower.containsAny("fail", "failed", "failure", "mistake", "result", "outcome", "reward") ->
                Hint(
                    verseRefs = listOf(VerseRef(2, 47), VerseRef(2, 48)),
                    queryTerms = listOf("action", "fruits of action", "duty", "karma yoga", "detachment"),
                )
            lower.containsAny("purpose", "meaning of life", "why am i here", "dharma", "duty") ->
                Hint(
                    verseRefs = listOf(VerseRef(3, 35), VerseRef(18, 46)),
                    queryTerms = listOf("dharma", "duty", "purpose", "own duty"),
                )
            lower.containsAny("afraid", "fear", "anxious", "anxiety", "worry", "future") ->
                Hint(
                    verseRefs = listOf(VerseRef(2, 47), VerseRef(2, 48), VerseRef(6, 26)),
                    queryTerms = listOf("equanimity", "action", "fruits of action", "steadfast", "restless mind"),
                )
            else -> Hint()
        }
    }

    fun expandedQuery(message: String): String {
        val terms = forMessage(message).queryTerms
        if (terms.isEmpty()) return message
        return (listOf(message) + terms).joinToString(" ")
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { contains(it) }
}
