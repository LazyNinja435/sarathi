package com.sarathi.app.llm

import com.sarathi.app.data.UserPreferences

enum class ChatProvider {
    GoogleAiStudio,
    OfflineDefault,
}

object ChatProviderSelector {
    fun select(preferences: UserPreferences): ChatProvider =
        if (preferences.googleAiStudioEnabled && preferences.googleAiStudioApiKeyConfigured) {
            ChatProvider.GoogleAiStudio
        } else {
            ChatProvider.OfflineDefault
        }
}
