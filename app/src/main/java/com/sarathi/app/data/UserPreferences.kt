package com.sarathi.app.data

import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory

data class UserPreferences(
    val userName: String = "",
    val selectedTone: GuidanceTone = GuidanceTone.Gentle,
    val onboardingComplete: Boolean = false,
    val customModelPath: String = "",
    val useMockMode: Boolean = false,
    val googleAiStudioEnabled: Boolean = false,
    val googleAiStudioApiKeyConfigured: Boolean = false,
    val lastFeeling: String = "",
    val dharmaNote: String = "",
    val userMemory: UserMemory = UserMemory(),
)
