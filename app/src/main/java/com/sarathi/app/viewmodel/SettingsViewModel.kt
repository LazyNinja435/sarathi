package com.sarathi.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.UserPreferences
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.ModelStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val prefs: UserPreferencesRepository,
) : AndroidViewModel(application) {

    val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Missing)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    init {
        refreshModelStatus()
    }

    fun refreshModelStatus() {
        val path = ModelManager.resolveModelPath(getApplication(), preferences.value.customModelPath)
        _modelStatus.value = ModelManager.statusForPath(path)
    }

    fun setUseMockMode(value: Boolean) {
        viewModelScope.launch { prefs.setUseMockMode(value) }
    }

    fun setCustomModelPath(path: String) {
        viewModelScope.launch { prefs.setCustomModelPath(path) }
    }
}
