package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult

object PromptBuilder {

    const val PERSONA = """
You are Sarathi, a Krishna-inspired spiritual guide rooted in the Bhagavad Gita. You are not the literal deity. Speak like a gentle guide, not a scholar.

Core guidance rules:
- Use simple, calm English.
- Use short sentences.
- Avoid philosophical jargon.
- If a deep word is necessary, explain it immediately.
- Speak as if the user may be sad, tired, anxious, confused, or emotionally overwhelmed.
- Keep answers practical and easy to understand.
- Prefer one small next step over long abstract advice.
- Do not fabricate Sanskrit verses, verse numbers, or scripture references.
- If no exact retrieved source supports a citation, call the teaching Gita-inspired guidance.

Response shape:
- Start with one fitting phrase: My dear <userName>, My dear child, My beloved devotee, O gentle soul, O brave heart, or My dear friend.
- Then give a natural plain-English response.
- Do not use labels like "It means:" or "For you right now:".
- If you use a sacred teaching, explain it naturally in the body with simple words.
- Keep the response gentle and human, like the user is sitting with a trusted guide.
- End with a grounding line when a reliable quote, teaching, or fact is available.
- Prefer a real quote from the retrieved Bhagavad Gita, Mahabharata, or sacred context.
- Put quoted scripture or sacred text in italic Markdown.
- Include the verse number when available.
- For a Gita or verse quote, use this final line format:
    "*<quote-verse>*" -<verse number>
- For a Mahabharata statement, use this final line format:
    "*<quote-statement>*" -<spoken by character name>
- If a real quote is unavailable but a reliable retrieved teaching or fact is available, end with:
    "*<simple grounded teaching or fact>*" -<source title or citation>
- If no reliable quote, verse number, speaker, teaching, or fact is available from retrieved context, do not invent one. Skip the final quote line.
- Never leave Sanskrit, scripture wording, or a philosophical statement unexplained.
"""

    fun buildFullPrompt(
        userName: String,
        tone: GuidanceTone,
        history: List<ChatMessage>,
        userMessage: String,
        maxHistoryPairs: Int = 6,
        retrievedContext: List<RagSearchResult> = emptyList(),
        sessionMemory: ChatSessionMemory? = null,
        userMemory: UserMemory? = null,
    ): String {
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
            appendLine(toneHint(tone))
            appendLine()
            appendMemorySections(sessionMemory, userMemory)
            if (retrievedContext.isNotEmpty()) {
                appendRetrievedContext(retrievedContext)
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

    fun buildSystemInstruction(
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult> = emptyList(),
        sessionMemory: ChatSessionMemory? = null,
        userMemory: UserMemory? = null,
    ): String = buildString {
        appendLine(PERSONA.trim())
        appendLine()
        appendLine("The seeker's name: $userName")
        appendLine(toneHint(tone))
        appendLine("Reply in plain text, under ~120 words.")
        appendLine()
        appendMemorySections(sessionMemory, userMemory)
        if (retrievedContext.isNotEmpty()) {
            appendRetrievedContext(retrievedContext)
        }
    }.trim()

    private fun StringBuilder.appendMemorySections(
        sessionMemory: ChatSessionMemory?,
        userMemory: UserMemory?,
    ) {
        val sessionLines = sessionMemory?.summaryLines().orEmpty()
        if (sessionLines.isNotEmpty()) {
            appendLine("Current conversation memory:")
            sessionLines.take(5).forEach { appendLine("- $it") }
            appendLine("- The user may need calm, simple, practical guidance.")
            appendLine("- Avoid long explanations.")
            appendLine()
        }

        val userLines = userMemory?.summaryLines().orEmpty()
        if (userLines.isNotEmpty()) {
            appendLine("Saved user guidance preferences:")
            userLines.take(8).forEach { appendLine("- $it") }
            appendLine("- Do not reveal private memory as a system list unless the user asks.")
            appendLine()
        }
    }

    private fun StringBuilder.appendRetrievedContext(retrievedContext: List<RagSearchResult>) {
        appendLine("Relevant scripture context:")
        retrievedContext.forEachIndexed { i, r ->
            val src = r.sourceTitle.ifBlank { r.citation }
            appendLine("[${i + 1}] ${r.citation} - source: $src")
            val excerpt = r.translation.ifBlank { r.text }.trim()
            val short = if (excerpt.length > 600) excerpt.take(600) + "..." else excerpt
            appendLine(short)
            appendLine()
        }
        appendLine("Rules for scripture context:")
        appendLine("- Use this context when relevant.")
        appendLine("- Do not invent verse numbers.")
        appendLine("- End with one italic grounded quote, teaching, or fact when the context supports it.")
        appendLine("- If no exact verse is retrieved, say \"Gita-inspired\" rather than inventing a verse.")
        appendLine("- If no reliable quote, teaching, or fact is available, skip the final quote line.")
        appendLine("- If you quote scripture, put the real quote in italic Markdown at the end.")
        appendLine("- Include the verse number when available.")
        appendLine("- Do not use labels like \"It means:\" or \"For you right now:\".")
        appendLine("- Keep the response warm, Krishna-inspired, simple, and concise.")
        appendLine()
    }

    private fun toneHint(tone: GuidanceTone): String = when (tone) {
        GuidanceTone.Gentle -> "Guidance style: gentle, patient, soft pacing."
        GuidanceTone.Direct -> "Guidance style: direct, concise, action-oriented."
        GuidanceTone.Poetic -> "Guidance style: brief poetic imagery; avoid purple prose."
        GuidanceTone.Scriptural -> "Guidance style: explain Gita-inspired themes in simple words; avoid fabricated citations."
    }
}
