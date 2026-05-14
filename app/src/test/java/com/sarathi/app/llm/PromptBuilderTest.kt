package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.rag.RagSearchResult
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

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
}
