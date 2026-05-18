package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun buildFullPrompt_containsSimpleLanguageInstructions() {
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "Test",
            tone = GuidanceTone.Gentle,
            history = emptyList(),
            userMessage = "I am sad",
        )

        assertTrue(prompt.contains("Use simple, calm English"))
        assertTrue(prompt.contains("Use short sentences"))
        assertTrue(prompt.contains("Speak like a gentle guide, not a scholar"))
    }

    @Test
    fun buildFullPrompt_containsNaturalSacredQuoteRule() {
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "Test",
            tone = GuidanceTone.Scriptural,
            history = emptyList(),
            userMessage = "What does the Gita say?",
        )

        assertTrue(prompt.contains("Do not use labels like \"It means:\" or \"For you right now:\""))
        assertTrue(prompt.contains("End with a grounding line when a reliable quote, teaching, or fact is available"))
        assertTrue(prompt.contains("Include the verse number when available"))
        assertTrue(prompt.contains("Never leave Sanskrit"))
    }

    @Test
    fun buildFullPrompt_skipsGroundingLineWhenNoReliableSourceIsAvailable() {
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "Test",
            tone = GuidanceTone.Gentle,
            history = emptyList(),
            userMessage = "What is the purpose of life?",
        )

        assertTrue(prompt.contains("Skip the final quote line"))
        assertTrue(prompt.contains("do not invent one"))
    }

    @Test
    fun buildFullPrompt_includesRetrievedRagContext() {
        val rag = RagSearchResult(
            id = "gita_01_001_001",
            work = "Bhagavad Gita",
            collection = "gita",
            title = "Arjuna Vishada Yoga",
            citation = "Bhagavad Gita 1.1",
            text = "Original",
            translation = "Dhritarashtra said: ...",
            sanskrit = "",
            sourceTitle = "Sample",
            sourceUrl = "",
            themes = emptyList(),
            score = 1.0,
        )
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "Test",
            tone = GuidanceTone.Gentle,
            history = emptyList(),
            userMessage = "What is dharma?",
            retrievedContext = listOf(rag),
        )
        assertTrue(prompt.contains("Relevant scripture context"))
        assertTrue(prompt.contains("Bhagavad Gita 1.1"))
        assertTrue(prompt.contains("Dhritarashtra said"))
    }

    @Test
    fun buildFullPrompt_includesRecentConversation() {
        val history = listOf(
            ChatMessage(sender = Sender.User, text = "Earlier question"),
            ChatMessage(sender = Sender.Assistant, text = "Earlier reply"),
        )
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "A",
            tone = GuidanceTone.Direct,
            history = history,
            userMessage = "Follow up",
            retrievedContext = emptyList(),
        )
        assertTrue(prompt.contains("Recent conversation"))
        assertTrue(prompt.contains("Earlier question"))
    }

    @Test
    fun buildFullPrompt_includesShortTermMemoryWhenProvided() {
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "A",
            tone = GuidanceTone.Gentle,
            history = emptyList(),
            userMessage = "Help",
            sessionMemory = ChatSessionMemory(
                currentEmotion = "anxious about work pressure",
                preferredGuidanceStyle = "short and simple",
            ),
        )

        assertTrue(prompt.contains("Current conversation memory"))
        assertTrue(prompt.contains("anxious about work pressure"))
        assertTrue(prompt.contains("Avoid long explanations"))
    }

    @Test
    fun buildFullPrompt_includesLongTermMemoryWhenProvided() {
        val prompt = PromptBuilder.buildFullPrompt(
            userName = "A",
            tone = GuidanceTone.Gentle,
            history = emptyList(),
            userMessage = "Help",
            userMemory = UserMemory(savedUserNotes = listOf("I prefer short answers.")),
        )

        assertTrue(prompt.contains("Saved user guidance preferences"))
        assertTrue(prompt.contains("Preferred language level: simple"))
        assertTrue(prompt.contains("I prefer short answers."))
    }
}
