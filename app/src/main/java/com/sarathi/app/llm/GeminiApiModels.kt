package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import org.json.JSONArray
import org.json.JSONObject

object GeminiRequestMapper {
    fun buildGenerateContentRequest(
        userMessage: String,
        history: List<ChatMessage>,
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult> = emptyList(),
        sessionMemory: ChatSessionMemory? = null,
        userMemory: UserMemory? = null,
        maxHistoryPairs: Int = 6,
    ): JSONObject {
        val recent = history
            .filter { it.sender == Sender.User || it.sender == Sender.Assistant }
            .takeLast(maxHistoryPairs * 2)

        val contents = JSONArray()
        recent.forEach { message ->
            contents.put(content(roleFor(message.sender), message.text))
        }
        contents.put(content("user", userMessage))

        return JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            PromptBuilder.buildSystemInstruction(
                                userName = userName,
                                tone = tone,
                                retrievedContext = retrievedContext,
                                sessionMemory = sessionMemory,
                                userMemory = userMemory,
                            ),
                        ),
                    ),
                ),
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", GoogleAiStudioConfig.TEMPERATURE)
                    .put("maxOutputTokens", GoogleAiStudioConfig.MAX_OUTPUT_TOKENS),
            )
    }

    private fun roleFor(sender: Sender): String = if (sender == Sender.User) "user" else "model"

    private fun content(role: String, text: String): JSONObject =
        JSONObject()
            .put("role", role)
            .put("parts", JSONArray().put(JSONObject().put("text", text)))
}

object GeminiResponseParser {
    fun parseGenerateContentResponse(json: String): String {
        val root = JSONObject(json)
        val promptFeedback = root.optJSONObject("promptFeedback")
        val blockReason = promptFeedback?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) {
            throw GeminiApiException.SafetyBlocked(blockReason)
        }

        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw GeminiApiException.EmptyResponse
        }

        val first = candidates.optJSONObject(0) ?: throw GeminiApiException.MalformedResponse
        val finishReason = first.optString("finishReason")
        if (finishReason == "SAFETY") {
            throw GeminiApiException.SafetyBlocked(finishReason)
        }
        val parts = first.optJSONObject("content")?.optJSONArray("parts")
            ?: throw GeminiApiException.EmptyResponse
        val text = buildString {
            for (i in 0 until parts.length()) {
                val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (partText.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(partText.trim())
                }
            }
        }.trim()
        if (text.isBlank()) throw GeminiApiException.EmptyResponse
        return text
    }
}

sealed class GeminiApiException(message: String) : Exception(message) {
    data object MissingApiKey : GeminiApiException("Missing Google AI Studio API key")
    data class Auth(val code: Int) : GeminiApiException("Google AI Studio authentication failed")
    data object Quota : GeminiApiException("Google AI Studio quota exceeded")
    data class Server(val code: Int) : GeminiApiException("Google AI Studio server error")
    data class Network(val error: Throwable) : GeminiApiException("Google AI Studio network error")
    data object MalformedResponse : GeminiApiException("Google AI Studio returned an unreadable response")
    data object EmptyResponse : GeminiApiException("Google AI Studio returned an empty response")
    data class SafetyBlocked(val reason: String) : GeminiApiException("Google AI Studio response was safety blocked")
}
