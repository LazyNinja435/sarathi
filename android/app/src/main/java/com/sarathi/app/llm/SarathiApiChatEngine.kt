package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SarathiApiClient {
    fun generate(request: SarathiApiChatRequest): SarathiApiChatResponse
}

class HttpSarathiApiClient(
    private val apiBaseUrl: String,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 45_000,
) : SarathiApiClient {
    override fun generate(request: SarathiApiChatRequest): SarathiApiChatResponse {
        val base = apiBaseUrl.trimEnd('/')
        val connection = (URL("$base/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return try {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
                it.write(request.toJson().toString())
            }
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) {
                throw IOException("Sarathi API request failed with $code")
            }
            val parsed = parseSarathiApiChatResponse(body)
            if (parsed.assistantMessage.isBlank()) {
                throw IOException("Sarathi API returned an empty response")
            }
            parsed
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(StandardCharsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
    }
}

class SarathiApiChatEngine(
    private val apiBaseUrl: String,
    private val fallback: ChatEngine,
    private val client: SarathiApiClient = HttpSarathiApiClient(apiBaseUrl),
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
        try {
            val response = client.generate(
                SarathiApiChatRequest(
                    latestUserMessage = userMessage,
                    recentHistory = history,
                    userName = userName,
                    shortTermMemory = sessionMemory,
                    userMemory = userMemory,
                ),
            )
            LlmLastErrorStore.clear()
            response.assistantMessage.trim().ifBlank {
                fallback.generateReply(userMessage, history, userName, tone, retrievedContext, sessionMemory, userMemory)
            }
        } catch (t: Throwable) {
            LlmLastErrorStore.set("Sarathi online guidance failed: ${t::class.java.simpleName}")
            fallback.generateReply(userMessage, history, userName, tone, retrievedContext, sessionMemory, userMemory)
        }
    }
}
