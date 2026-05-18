package com.sarathi.app.model

data class ChatSessionMemory(
    val currentEmotion: String = "",
    val currentConcern: String = "",
    val importantContext: String = "",
    val preferredGuidanceStyle: String = "",
    val lastGuidanceTheme: String = "",
) {
    fun summaryLines(): List<String> = listOfNotNull(
        currentEmotion.takeIf { it.isNotBlank() }?.let { "The user seems $it." },
        currentConcern.takeIf { it.isNotBlank() }?.let { "Current concern: $it." },
        importantContext.takeIf { it.isNotBlank() }?.let { "Important context: $it." },
        preferredGuidanceStyle.takeIf { it.isNotBlank() }?.let { "Preferred guidance style: $it." },
        lastGuidanceTheme.takeIf { it.isNotBlank() }?.let { "Last guidance theme: $it." },
    )

    companion object {
        fun updatedFrom(message: String, previous: ChatSessionMemory = ChatSessionMemory()): ChatSessionMemory {
            val lower = message.lowercase()
            val emotion = when {
                lower.containsAny("anxious", "anxiety", "worried", "worry", "afraid", "fear") -> "anxious or afraid"
                lower.containsAny("sad", "grief", "heartbroken", "lonely", "tired", "exhausted") -> "sad or tired"
                lower.containsAny("angry", "anger", "frustrated") -> "angry or frustrated"
                lower.containsAny("confused", "lost", "unclear", "overwhelmed") -> "confused or overwhelmed"
                else -> previous.currentEmotion
            }
            val concern = when {
                lower.containsAny("work", "job", "boss", "office", "career") -> "work pressure"
                lower.containsAny("future", "result", "outcome", "exam", "interview") -> "fear about results or the future"
                lower.containsAny("family", "relationship", "friend", "partner") -> "relationship or family pain"
                lower.containsAny("dharma", "purpose", "path", "duty") -> "dharma, purpose, or duty"
                else -> previous.currentConcern
            }
            val style = when {
                lower.containsAny("short answer", "keep it short", "simple answer", "explain simply") ->
                    "short, simple, and practical"
                else -> previous.preferredGuidanceStyle
            }
            val theme = when {
                lower.containsAny("gita", "verse", "scripture", "mahabharata") -> "simple scripture explanation"
                lower.containsAny("action", "duty", "step") -> "one small next step"
                else -> previous.lastGuidanceTheme
            }
            return previous.copy(
                currentEmotion = emotion,
                currentConcern = concern,
                importantContext = previous.importantContext,
                preferredGuidanceStyle = style.ifBlank { "calm, simple, practical guidance" },
                lastGuidanceTheme = theme,
            )
        }

        private fun String.containsAny(vararg needles: String): Boolean =
            needles.any { contains(it) }
    }
}
