package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SarathiApiChatEngineTest {
    @Test
    fun generateReply_postsWithoutProviderOrApiKey() = runBlocking {
        var requestSeen: SarathiApiChatRequest? = null
        val engine = SarathiApiChatEngine(
            apiBaseUrl = "https://example.test/api",
            fallback = StaticChatEngine("fallback"),
            client = object : SarathiApiClient {
                override fun generate(request: SarathiApiChatRequest): SarathiApiChatResponse {
                    requestSeen = request
                    return SarathiApiChatResponse(
                        assistantMessage = "cloud",
                        provider = "gemini",
                        model = "gemini-flash-lite-latest",
                    )
                }
            },
        )

        val reply = engine.generateReply(
            userMessage = "Hello",
            history = emptyList(),
            userName = "dear one",
            tone = GuidanceTone.Gentle,
            retrievedContext = emptyList(),
            sessionMemory = ChatSessionMemory(),
            userMemory = UserMemory(),
        )

        assertEquals("cloud", reply)
        val json = requireNotNull(requestSeen).toJson().toString()
        assertEquals("Hello", requestSeen?.latestUserMessage)
        assertFalse(json.contains("apiKey"))
        assertFalse(json.contains("geminiApiKey"))
        assertFalse(json.contains("provider"))
    }

    @Test
    fun requestJson_mapsHistorySenderToApiRoleAndTimestampToCreatedAt() {
        val request = SarathiApiChatRequest(
            latestUserMessage = "Now",
            recentHistory = listOf(
                ChatMessage(sender = Sender.User, text = "Earlier question", timestampMillis = 0L),
                ChatMessage(sender = Sender.Assistant, text = "Earlier reply", timestampMillis = 1_000L),
                ChatMessage(sender = Sender.System, text = "Do not send system role", timestampMillis = 2_000L),
            ),
            userName = "dear one",
            shortTermMemory = ChatSessionMemory(),
            userMemory = UserMemory(),
        )

        val history = request.toJson().getJSONArray("recentHistory")

        assertEquals(2, history.length())
        assertEquals("user", history.getJSONObject(0).getString("role"))
        assertEquals("assistant", history.getJSONObject(1).getString("role"))
        assertEquals("1970-01-01T00:00:00Z", history.getJSONObject(0).getString("createdAt"))
    }

    @Test
    fun generateReply_fallsBackWhenApiFails() = runBlocking {
        val engine = SarathiApiChatEngine(
            apiBaseUrl = "https://example.test/api",
            fallback = StaticChatEngine("offline"),
            client = object : SarathiApiClient {
                override fun generate(request: SarathiApiChatRequest): SarathiApiChatResponse {
                    throw IOException("network unavailable")
                }
            },
        )

        val reply = engine.generateReply(
            userMessage = "Hello",
            history = emptyList(),
            userName = "dear one",
            tone = GuidanceTone.Gentle,
            retrievedContext = emptyList(),
            sessionMemory = ChatSessionMemory(),
            userMemory = UserMemory(),
        )

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
