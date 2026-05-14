package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.rag.RagSearchResult

object PromptBuilder {

    private const val PERSONA = """
You are Sarathi, a Krishna-inspired spiritual guide rooted in the Bhagavad Gita. Speak with warmth, wisdom, poetic clarity, dharma, karma yoga, devotion, detachment, and self-mastery. Give the user the feeling of being lovingly guided by Krishna as charioteer and friend. Do not claim to be the literal deity. Do not fabricate exact verse numbers or Sanskrit. If exact verse citation is needed and no source is retrieved, say that the teaching is Gita-inspired rather than quoting a verse.
"""

    fun buildFullPrompt(
        userName: String,
        tone: GuidanceTone,
        history: List<ChatMessage>,
        userMessage: String,
        maxHistoryPairs: Int = 6,
        retrievedContext: List<RagSearchResult> = emptyList(),
    ): String {
        val toneHint = when (tone) {
            GuidanceTone.Gentle -> "Guidance style: gentle, patient, soft pacing."
            GuidanceTone.Direct -> "Guidance style: direct, concise, action-oriented."
            GuidanceTone.Poetic -> "Guidance style: brief poetic imagery; avoid purple prose."
            GuidanceTone.Scriptural -> "Guidance style: Gita-inspired themes; avoid fabricated citations."
        }
        val recent = history
            .filter { it.sender != Sender.System }
            .takeLast(maxHistoryPairs * 2)
            .joinToString("\n") { m ->
                val role = if (m.sender == Sender.User) "Seeker" else "Sarathi"
                "$role: ${m.text}"
            }
        return buildString {
            appendLine(PERSONA.trim())
            appendLine()
            appendLine("The seeker's name: $userName")
            appendLine(toneHint)
            appendLine()
            if (retrievedContext.isNotEmpty()) {
                appendLine("Relevant scripture context:")
                retrievedContext.forEachIndexed { i, r ->
                    val src = r.sourceTitle.ifBlank { r.citation }
                    appendLine("[${i + 1}] ${r.citation} — source: $src")
                    val excerpt = r.translation.ifBlank { r.text }.trim()
                    val short = if (excerpt.length > 600) excerpt.take(600) + "…" else excerpt
                    appendLine(short)
                    appendLine()
                }
                appendLine("Rules:")
                appendLine("- Use this context when relevant.")
                appendLine("- Do not invent verse numbers.")
                appendLine("- If no exact verse is retrieved, say \"Gita-inspired\" rather than quoting.")
                appendLine("- Keep the response warm, Krishna-inspired, and concise.")
                appendLine()
            }
            if (recent.isNotBlank()) {
                appendLine("Recent conversation:")
                appendLine(recent)
                appendLine()
            }
            appendLine("Seeker: $userMessage")
            appendLine()
            appendLine("Sarathi (reply in plain text, under ~120 words):")
        }
    }
}
