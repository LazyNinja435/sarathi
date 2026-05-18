package com.sarathi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.UserMemoryCommand
import com.sarathi.app.data.UserPreferences
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.llm.ChatEngine
import com.sarathi.app.llm.ChatProvider
import com.sarathi.app.llm.ChatProviderSelector
import com.sarathi.app.llm.GoogleAiStudioChatEngine
import com.sarathi.app.llm.LiteRtLmGemmaChatEngine
import com.sarathi.app.llm.MediaPipeGemmaChatEngine
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceSurface
import com.sarathi.app.model.ModelEligibility
import com.sarathi.app.update.ManifestCache
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagRepository
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class ChatViewModel(
    application: Application,
    private val prefs: UserPreferencesRepository,
    private val rag: RagRepository,
) : AndroidViewModel(application) {

    private val unreachableEngine = object : ChatEngine {
        override suspend fun generateReply(
            userMessage: String,
            history: List<ChatMessage>,
            userName: String,
            tone: GuidanceTone,
            retrievedContext: List<RagSearchResult>,
            sessionMemory: ChatSessionMemory,
            userMemory: UserMemory,
        ): String = UNREACHABLE_MESSAGE
    }

    init {
        viewModelScope.launch {
            rag.warmUp()
        }
    }

    private var liteRtLmEngine: LiteRtLmGemmaChatEngine? = null
    private var liteRtLmPath: String? = null

    private var mediaPipeEngine: MediaPipeGemmaChatEngine? = null
    private var mediaPipePath: String? = null
    private val sendMutex = Mutex()

    val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    val guidanceSurface: StateFlow<GuidanceSurface> = combine(
        preferences,
        ManifestCache.revision,
    ) { p, _ ->
        guidanceSurfaceFor(p)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        guidanceSurfaceFor(preferences.value),
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sessionMemory = MutableStateFlow(ChatSessionMemory())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    private fun guidanceSurfaceFor(p: UserPreferences): GuidanceSurface {
        if (ChatProviderSelector.select(p) == ChatProvider.GoogleAiStudio) {
            return GuidanceSurface.GoogleAiStudio
        }
        val app = getApplication<Application>()
        val litePath = ModelManager.resolveLiteRtLmPath(app, p.customModelPath)
        if (litePath != null && ModelEligibility.shouldBlockLiteRt(app, p.customModelPath)) {
            return GuidanceSurface.ModelUpdateRequired
        }
        if (litePath != null) {
            return GuidanceSurface.OnDeviceGemma
        }
        if (ModelManager.resolveMediaPipeTaskPath(app, p.customModelPath) != null) {
            return GuidanceSurface.OnDeviceMediaPipe
        }
        return GuidanceSurface.OfflineGuidance
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun ensureWelcomeMessage() {
        if (_messages.value.isNotEmpty()) return
        if (!hasReachableGuidance(preferences.value)) {
            _messages.value = listOf(
                ChatMessage(sender = Sender.Assistant, text = UNREACHABLE_MESSAGE),
            )
            return
        }
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
            if (!sendMutex.tryLock()) return@launch
            try {
                _messages.value = _messages.value + ChatMessage(sender = Sender.User, text = trimmed)
                val priorHistory = _messages.value.dropLast(1)
                val updatedSessionMemory = ChatSessionMemory.updatedFrom(trimmed, _sessionMemory.value)
                _sessionMemory.value = updatedSessionMemory
                _input.value = ""
                _typing.value = true
                try {
                    val explicitMemory = UserMemoryCommand.extractExplicitMemory(trimmed)
                    if (explicitMemory != null) {
                        prefs.rememberUserNote(explicitMemory)
                        val name = preferences.value.userName.ifBlank { "dear one" }
                        _messages.value = _messages.value + ChatMessage(
                            sender = Sender.Assistant,
                            text = "I will remember that, $name. I will keep it in mind and answer simply.",
                        )
                        return@launch
                    }
                    val retrieved = rag.search(trimmed, limit = 3)
                    val engine = resolveEngine()
                    val currentPreferences = preferences.value
                    val reply = engine.generateReply(
                        userMessage = trimmed,
                        history = priorHistory,
                        userName = currentPreferences.userName.ifBlank { "dear one" },
                        tone = currentPreferences.selectedTone,
                        retrievedContext = retrieved,
                        sessionMemory = updatedSessionMemory,
                        userMemory = currentPreferences.userMemory,
                    )
                    _messages.value = _messages.value + ChatMessage(sender = Sender.Assistant, text = reply)
                } finally {
                    _typing.value = false
                }
            } finally {
                sendMutex.unlock()
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
        val offlineEngine = resolveOfflineEngine(p)
        if (ChatProviderSelector.select(p) == ChatProvider.GoogleAiStudio) {
            return GoogleAiStudioChatEngine(
                apiKeyProvider = { prefs.getGoogleAiStudioApiKey() },
                fallback = offlineEngine,
            )
        }
        return offlineEngine
    }

    private fun hasReachableGuidance(p: UserPreferences): Boolean {
        if (ChatProviderSelector.select(p) == ChatProvider.GoogleAiStudio) {
            return true
        }
        val app = getApplication<Application>()
        val litePath = ModelManager.resolveLiteRtLmPath(app, p.customModelPath)
        if (litePath != null && !ModelEligibility.shouldBlockLiteRt(app, p.customModelPath)) {
            return true
        }
        return ModelManager.resolveMediaPipeTaskPath(app, p.customModelPath) != null
    }

    private suspend fun resolveOfflineEngine(p: UserPreferences): ChatEngine {
        val litePath = ModelManager.resolveLiteRtLmPath(getApplication(), p.customModelPath)
        if (litePath != null && ModelEligibility.shouldBlockLiteRt(getApplication(), p.customModelPath)) {
            mediaPipeEngine?.close()
            mediaPipeEngine = null
            mediaPipePath = null
            liteRtLmEngine?.close()
            liteRtLmEngine = null
            liteRtLmPath = null
            return unreachableEngine
        }
        if (litePath != null) {
            mediaPipeEngine?.close()
            mediaPipeEngine = null
            mediaPipePath = null
            if (liteRtLmPath != litePath || liteRtLmEngine == null) {
                liteRtLmEngine?.close()
                liteRtLmPath = litePath
                liteRtLmEngine = LiteRtLmGemmaChatEngine(getApplication(), litePath, unreachableEngine)
            }
            return liteRtLmEngine!!
        }
        liteRtLmEngine?.close()
        liteRtLmEngine = null
        liteRtLmPath = null

        val taskPath = ModelManager.resolveMediaPipeTaskPath(getApplication(), p.customModelPath)
            ?: run {
                mediaPipeEngine?.close()
                mediaPipeEngine = null
                mediaPipePath = null
                return unreachableEngine
            }
        if (mediaPipePath != taskPath || mediaPipeEngine == null) {
            mediaPipeEngine?.close()
            mediaPipePath = taskPath
            mediaPipeEngine = MediaPipeGemmaChatEngine(getApplication(), taskPath, unreachableEngine)
        }
        return mediaPipeEngine!!
    }

    override fun onCleared() {
        super.onCleared()
        liteRtLmEngine?.close()
        liteRtLmEngine = null
        liteRtLmPath = null
        mediaPipeEngine?.close()
        mediaPipeEngine = null
        mediaPipePath = null
    }

    private companion object {
        const val UNREACHABLE_MESSAGE =
            "Vaikuntha is unreachable right now! Please check your connection settings."
    }
}
