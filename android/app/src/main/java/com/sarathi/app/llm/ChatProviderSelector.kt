package com.sarathi.app.llm

import com.sarathi.app.data.UserPreferences

enum class ChatProvider {
    ServerManagedCloud,
    OfflineDefault,
}

object ChatProviderSelector {
    fun select(preferences: UserPreferences): ChatProvider =
        if (preferences.useMockMode) {
            ChatProvider.OfflineDefault
        } else {
            ChatProvider.ServerManagedCloud
        }
}
