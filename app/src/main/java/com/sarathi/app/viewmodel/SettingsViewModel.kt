package com.sarathi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.UserPreferences
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.llm.LlmLastErrorStore
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.LlmModelFileKind
import com.sarathi.app.model.LlmRuntimeDiagnostics
import com.sarathi.app.model.LlmRuntimeKind
import com.sarathi.app.model.ModelStatus
import com.sarathi.app.rag.RagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    application: Application,
    private val prefs: UserPreferencesRepository,
    private val rag: RagRepository,
) : AndroidViewModel(application) {

    val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Missing)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _llmDiagnostics = MutableStateFlow(
        LlmRuntimeDiagnostics(
            activeRuntime = LlmRuntimeKind.Mock,
            modelFileKind = LlmModelFileKind.Missing,
            liteRtLmPath = null,
            mediaPipeTaskPath = null,
            selectedPath = null,
        ),
    )
    val llmDiagnostics: StateFlow<LlmRuntimeDiagnostics> = _llmDiagnostics.asStateFlow()

    private val _ragReady = MutableStateFlow(false)
    val ragReady: StateFlow<Boolean> = _ragReady.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.refreshGoogleAiStudioApiKeyConfigured()
        }
        refreshModelStatus()
    }

    fun refreshModelStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                rag.warmUp()
            }
            _ragReady.value = rag.isReady()
            val pr = preferences.value
            val path = ModelManager.resolvePreferredModelPath(getApplication(), pr.customModelPath)
            _modelStatus.value = ModelManager.statusForPath(path)
            _llmDiagnostics.value = ModelManager.diagnostics(
                getApplication(),
                useMockMode = pr.useMockMode,
                customModelPath = pr.customModelPath,
            )
        }
    }

    fun setUseMockMode(value: Boolean) {
        viewModelScope.launch { prefs.setUseMockMode(value) }
    }

    fun setGoogleAiStudioEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setGoogleAiStudioEnabled(value) }
    }

    fun saveGoogleAiStudioApiKey(apiKey: String) {
        viewModelScope.launch { prefs.saveGoogleAiStudioApiKey(apiKey) }
    }

    fun clearGoogleAiStudioApiKey() {
        viewModelScope.launch { prefs.clearGoogleAiStudioApiKey() }
    }

    fun clearUserMemory() {
        viewModelScope.launch { prefs.clearUserMemory() }
    }

    fun setCustomModelPath(path: String) {
        viewModelScope.launch { prefs.setCustomModelPath(path) }
    }

    fun resetOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            prefs.resetOnboarding()
            refreshModelStatus()
            onComplete()
        }
    }

    fun lastInferenceError(): String? = LlmLastErrorStore.peek()
}
