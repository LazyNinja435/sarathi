package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockKrishnaChatEngineTest {

    @Test
    fun stableFallbackForUnknownQuery() = runBlocking {
        val engine = MockKrishnaChatEngine()
        val a = engine.generateReply(
            userMessage = "something completely unknown xyz123",
            history = emptyList(),
            userName = "Sam",
            tone = GuidanceTone.Gentle,
            retrievedContext = emptyList(),
        )
        val b = engine.generateReply(
            userMessage = "something completely unknown xyz123",
            history = emptyList(),
            userName = "Sam",
            tone = GuidanceTone.Gentle,
            retrievedContext = emptyList(),
        )
        assertEquals(a, b)
        assertTrue(a.contains("Sam"))
    }

    @Test
    fun anxietyKeywordBranch() = runBlocking {
        val engine = MockKrishnaChatEngine()
        val out = engine.generateReply(
            userMessage = "I feel anxious about work",
            history = emptyList(),
            userName = "R",
            tone = GuidanceTone.Gentle,
            retrievedContext = emptyList(),
        )
        assertTrue(out.contains("breath", ignoreCase = true) || out.contains("mind", ignoreCase = true))
    }
}
