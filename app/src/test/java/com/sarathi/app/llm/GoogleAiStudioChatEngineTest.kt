package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleAiStudioChatEngineTest {
    @Test
    fun generateReply_usesGeminiWhenKeyExists() = runBlocking {
        val engine = GoogleAiStudioChatEngine(
            apiKeyProvider = { "key" },
            fallback = StaticChatEngine("offline"),
            client = object : GeminiContentClient {
                override fun generateContent(apiKey: String, request: JSONObject): String = "cloud"
            },
        )

        val reply = engine.generateReply("Hello", emptyList(), "A", GuidanceTone.Gentle)

        assertEquals("cloud", reply)
    }

    @Test
    fun generateReply_skipsGeminiWhenKeyMissing() = runBlocking {
        var called = false
        val engine = GoogleAiStudioChatEngine(
            apiKeyProvider = { null },
            fallback = StaticChatEngine("offline"),
            client = object : GeminiContentClient {
                override fun generateContent(apiKey: String, request: JSONObject): String {
                    called = true
                    return "cloud"
                }
            },
        )

        val reply = engine.generateReply("Hello", emptyList(), "A", GuidanceTone.Gentle)

        assertEquals("offline", reply)
        assertEquals(false, called)
    }

    @Test
    fun generateReply_fallsBackOnGeminiError() = runBlocking {
        val engine = GoogleAiStudioChatEngine(
            apiKeyProvider = { "key" },
            fallback = StaticChatEngine("offline"),
            client = object : GeminiContentClient {
                override fun generateContent(apiKey: String, request: JSONObject): String {
                    throw GeminiApiException.Auth(403)
                }
            },
        )

        val reply = engine.generateReply("Hello", emptyList(), "A", GuidanceTone.Gentle)

        assertEquals("offline", reply)
    }

    private class StaticChatEngine(private val reply: String) : ChatEngine {
        override suspend fun generateReply(
            userMessage: String,
            history: List<ChatMessage>,
            userName: String,
            tone: GuidanceTone,
            retrievedContext: List<RagSearchResult>,
            sessionMemory: ChatSessionMemory,
            userMemory: UserMemory,
        ): String = reply
    }
}
