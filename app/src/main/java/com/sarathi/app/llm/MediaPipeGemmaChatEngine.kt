package com.sarathi.app.llm

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MediaPipe LLM Inference (Gemma `.task`) adapter.
 *
 * - Engine: [LlmInference.createFromOptions]
 * - Session: [LlmInferenceSession.createFromOptions] — recreated per request to avoid unbounded context.
 * - Generate: [LlmInferenceSession.addQueryChunk] + [LlmInferenceSession.generateResponseAsync]
 *
 * TODO: optional GPU backend via [LlmInference.LlmInferenceOptions.Builder.setPreferredBackend] when you want to benchmark devices.
 * TODO: in-app download of compatible `.task` bundles (terms-gated); keep manual side-load for v1.
 */
class MediaPipeGemmaChatEngine(
    private val appContext: Context,
    private val modelAbsolutePath: String,
    private val fallback: MockKrishnaChatEngine,
) : ChatEngine {

    private val mutex = Mutex()
    private var llmInference: LlmInference? = null

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
                if (llmInference == null) {
                    Log.i(TAG, "LlmInference load begin path=${modelAbsolutePath.substringAfterLast('/')}")
                    val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelAbsolutePath)
                        .setMaxTokens(512)
                        .build()
                    llmInference = LlmInference.createFromOptions(appContext, inferenceOptions)
                    Log.i(TAG, "LlmInference load ok")
                }
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(0.85f)
                    .setTopK(40)
                    .setTopP(0.9f)
                    .build()
                val session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
                try {
                    session.addQueryChunk(prompt)
                    val future = session.generateResponseAsync(
                        ProgressListener { _, _ -> },
                    )
                    val out = future.awaitString()
                    Log.i(TAG, "generateReply end chars=${out.length}")
                    LlmLastErrorStore.clear()
                    out
                } finally {
                    session.close()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "MediaPipe inference failed, using mock: ${t.message}", t)
            LlmLastErrorStore.set(t.message ?: t::class.java.simpleName)
            fallback.generateReply(userMessage, history, userName, tone, retrievedContext)
        }
    }

    fun close() {
        runBlocking(Dispatchers.IO) {
            mutex.withLock {
                try {
                    llmInference?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "close: ${e.message}")
                } finally {
                    llmInference = null
                }
            }
        }
    }

    private companion object {
        const val TAG = "MediaPipeGemmaChatEngine"
    }
}

@Suppress("UNCHECKED_CAST")
private suspend fun ListenableFuture<*>.awaitString(): String =
    suspendCancellableCoroutine { cont ->
        val future = this as ListenableFuture<String>
        Futures.addCallback(
            future,
            object : FutureCallback<String> {
                override fun onSuccess(result: String) {
                    if (cont.isActive) cont.resume(result)
                }

                override fun onFailure(t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                }
            },
            MoreExecutors.directExecutor(),
        )
        cont.invokeOnCancellation { future.cancel(true) }
    }
