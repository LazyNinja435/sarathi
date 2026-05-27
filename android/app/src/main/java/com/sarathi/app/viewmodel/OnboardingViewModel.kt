package com.sarathi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.model.GuidanceTone
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    fun saveName(name: String) {
        viewModelScope.launch { prefs.setUserName(name) }
    }

    fun saveTone(tone: GuidanceTone) {
        viewModelScope.launch { prefs.setSelectedTone(tone) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingComplete(true) }
    }
}
