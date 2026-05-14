package com.sarathi.app.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LiteRT-LM adapter for Gemma-family **`.litertlm`** checkpoints.
 *
 * Does **not** use MediaPipe [com.google.mediapipe.tasks.genai.llminference.LlmInference].
 * Loads lazily on first generation, serializes work with a [Mutex], and falls back to [fallback] on failure.
 */
class LiteRtLmGemmaChatEngine(
    private val appContext: Context,
    private val modelAbsolutePath: String,
    private val fallback: MockKrishnaChatEngine,
) : ChatEngine {

    private val mutex = Mutex()
    private var engine: Engine? = null

    override suspend fun generateReply(
        userMessage: String,
        history: List<ChatMessage>,
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult>,
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "generateReply start model=${modelAbsolutePath.substringAfterLast('/')}")
            val prompt = PromptBuilder.buildFullPrompt(
                userName = userName,
                tone = tone,
                history = history,
                userMessage = userMessage,
                retrievedContext = retrievedContext,
            )
            mutex.withLock {
                if (engine == null) {
                    Log.i(TAG, "LiteRT-LM load begin path tail=${modelAbsolutePath.substringAfterLast('/')}")
                    val cfg = EngineConfig(
                        modelPath = modelAbsolutePath,
                        backend = Backend.CPU(),
                        cacheDir = appContext.cacheDir.absolutePath,
                        maxNumTokens = 4096,
                    )
                    val eng = Engine(cfg)
                    eng.initialize()
                    engine = eng
                    Log.i(TAG, "LiteRT-LM load ok")
                }
                Log.i(TAG, "LiteRT-LM generation start")
                val eng = engine!!
                val replyText = eng.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 40,
                            topP = 0.9,
                            temperature = 0.85,
                        ),
                    ),
                ).use { conversation ->
                    val response = conversation.sendMessage(prompt)
                    response.toString().trim()
                }
                val clipped = if (replyText.length > MAX_REPLY_CHARS) {
                    replyText.take(MAX_REPLY_CHARS) + "…"
                } else {
                    replyText
                }
                Log.i(TAG, "LiteRT-LM generation end chars=${clipped.length}")
                LlmLastErrorStore.clear()
                clipped
            }
        } catch (t: Throwable) {
            Log.w(TAG, "LiteRT-LM inference failed, using mock: ${t.message}", t)
            LlmLastErrorStore.set(t.message ?: t::class.java.simpleName)
            fallback.generateReply(userMessage, history, userName, tone, retrievedContext)
        }
    }

    fun close() {
        runBlocking(Dispatchers.IO) {
            mutex.withLock {
                try {
                    engine?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "close: ${e.message}")
                } finally {
                    engine = null
                }
            }
        }
    }

    private companion object {
        const val TAG = "LiteRtLmGemmaChatEngine"
        const val MAX_REPLY_CHARS = 6000
    }
}
