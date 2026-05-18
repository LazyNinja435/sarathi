package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiModelsTest {
    @Test
    fun buildGenerateContentRequest_mapsHistoryAndUserMessage() {
        val request = GeminiRequestMapper.buildGenerateContentRequest(
            userMessage = "What is dharma?",
            history = listOf(
                ChatMessage(sender = Sender.System, text = "local debug"),
                ChatMessage(sender = Sender.User, text = "Earlier question"),
                ChatMessage(sender = Sender.Assistant, text = "Earlier reply"),
            ),
            userName = "Arjun",
            tone = GuidanceTone.Gentle,
        )

        val contents = request.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals("user", contents.getJSONObject(2).getString("role"))
        assertEquals("What is dharma?", contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getString("text"))
        assertTrue(request.getJSONObject("systemInstruction").toString().contains("Krishna-inspired spiritual guide"))
        assertTrue(request.getJSONObject("generationConfig").getDouble("temperature") > 0.0)
    }

    @Test
    fun parseGenerateContentResponse_returnsCandidateText() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Steady your heart." },
                      { "text": "Then act." }
                    ]
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        assertEquals("Steady your heart.\nThen act.", GeminiResponseParser.parseGenerateContentResponse(json))
    }

    @Test
    fun parseGenerateContentResponse_rejectsSafetyBlockedPrompt() {
        val json = """{ "promptFeedback": { "blockReason": "SAFETY" } }"""

        assertThrows(GeminiApiException.SafetyBlocked::class.java) {
            GeminiResponseParser.parseGenerateContentResponse(json)
        }
    }

    @Test
    fun parseGenerateContentResponse_rejectsEmptyCandidate() {
        val json = """{ "candidates": [ { "content": { "parts": [] } } ] }"""

        assertThrows(GeminiApiException.EmptyResponse::class.java) {
            GeminiResponseParser.parseGenerateContentResponse(json)
        }
    }
}
