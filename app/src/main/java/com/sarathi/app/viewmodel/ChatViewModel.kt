package com.sarathi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.UserPreferences
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.llm.ChatEngine
import com.sarathi.app.llm.MediaPipeGemmaChatEngine
import com.sarathi.app.llm.MockKrishnaChatEngine
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.rag.RagRepository
import com.sarathi.app.model.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val prefs: UserPreferencesRepository,
    private val rag: RagRepository,
) : AndroidViewModel(application) {

    private val mockEngine = MockKrishnaChatEngine()

    init {
        viewModelScope.launch {
            rag.warmUp()
        }
    }
    private var mediaPipeEngine: MediaPipeGemmaChatEngine? = null
    private var mediaPipePath: String? = null

    val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    fun setInput(text: String) {
        _input.value = text
    }

    fun ensureWelcomeMessage() {
        if (_messages.value.isNotEmpty()) return
        val name = preferences.value.userName.ifBlank { "dear one" }
        val welcome = buildString {
            appendLine("My dear $name,")
            appendLine()
            appendLine("I have been seated quietly in the chariot of your heart.")
            appendLine()
            appendLine("Tell me — what battle stands before you today?")
        }.trim()
        _messages.value = listOf(
            ChatMessage(sender = Sender.Assistant, text = welcome),
        )
    }

    fun sendFromInput() {
        sendUserMessage(_input.value)
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage(sender = Sender.User, text = trimmed)
            val priorHistory = _messages.value.dropLast(1)
            _input.value = ""
            _typing.value = true
            try {
                val retrieved = rag.search(trimmed, limit = 3)
                val engine = resolveEngine()
                val reply = engine.generateReply(
                    userMessage = trimmed,
                    history = priorHistory,
                    userName = preferences.value.userName.ifBlank { "dear one" },
                    tone = preferences.value.selectedTone,
                    retrievedContext = retrieved,
                )
                _messages.value = _messages.value + ChatMessage(sender = Sender.Assistant, text = reply)
            } finally {
                _typing.value = false
            }
        }
    }

    /** From "When I Feel" — prefill pattern: send immediately. */
    fun prefillAndSend(text: String) {
        _input.value = text
        sendUserMessage(text)
    }

    private suspend fun resolveEngine(): ChatEngine {
        val p = preferences.value
        if (p.useMockMode) {
            mediaPipeEngine?.close()
            mediaPipeEngine = null
            mediaPipePath = null
            return mockEngine
        }
        val path = ModelManager.resolveModelPath(getApplication(), p.customModelPath)
            ?: run {
                mediaPipeEngine?.close()
                mediaPipeEngine = null
                mediaPipePath = null
                return mockEngine
            }
        if (mediaPipePath != path || mediaPipeEngine == null) {
            mediaPipeEngine?.close()
            mediaPipePath = path
            mediaPipeEngine = MediaPipeGemmaChatEngine(getApplication(), path, mockEngine)
        }
        return mediaPipeEngine!!
    }

    override fun onCleared() {
        super.onCleared()
        mediaPipeEngine?.close()
        mediaPipeEngine = null
        mediaPipePath = null
    }
}
