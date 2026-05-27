package com.sarathi.app.llm

import android.util.Log
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAiStudioChatEngine(
    private val apiKeyProvider: () -> String?,
    private val fallback: ChatEngine,
    private val client: GeminiContentClient = GoogleAiStudioGeminiClient(),
) : ChatEngine {
    override suspend fun generateReply(
        userMessage: String,
        history: List<ChatMessage>,
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult>,
        sessionMemory: ChatSessionMemory,
        userMemory: UserMemory,
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext fallback.generateReply(userMessage, history, userName, tone, retrievedContext, sessionMemory, userMemory)
        }
        try {
            val request = GeminiRequestMapper.buildGenerateContentRequest(
                userMessage = userMessage,
                history = history,
                userName = userName,
                tone = tone,
                retrievedContext = retrievedContext,
                sessionMemory = sessionMemory,
                userMemory = userMemory,
            )
            val reply = client.generateContent(apiKey, request).trim()
            LlmLastErrorStore.clear()
            reply
        } catch (e: GeminiApiException) {
            safeWarn("Google AI Studio failed; using offline fallback: ${sanitizedReason(e)}")
            LlmLastErrorStore.set(sanitizedReason(e))
            fallback.generateReply(userMessage, history, userName, tone, retrievedContext, sessionMemory, userMemory)
        } catch (t: Throwable) {
            safeWarn("Google AI Studio failed; using offline fallback: ${t::class.java.simpleName}")
            LlmLastErrorStore.set("Google AI Studio failed: ${t::class.java.simpleName}")
            fallback.generateReply(userMessage, history, userName, tone, retrievedContext, sessionMemory, userMemory)
        }
    }

    private fun safeWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: RuntimeException) {
            // Android Log is not available in local JVM unit tests.
        }
    }

    private fun sanitizedReason(e: GeminiApiException): String = when (e) {
        is GeminiApiException.Auth -> "Google AI Studio authentication failed (${e.code})"
        GeminiApiException.EmptyResponse -> "Google AI Studio returned an empty response"
        GeminiApiException.MalformedResponse -> "Google AI Studio returned an unreadable response"
        GeminiApiException.MissingApiKey -> "Google AI Studio API key is missing"
        is GeminiApiException.Network -> "Google AI Studio network error"
        GeminiApiException.Quota -> "Google AI Studio quota exceeded"
        is GeminiApiException.SafetyBlocked -> "Google AI Studio response was safety blocked"
        is GeminiApiException.Server -> "Google AI Studio server error (${e.code})"
    }

    private companion object {
        const val TAG = "GoogleAiStudioChatEngine"
    }
}
