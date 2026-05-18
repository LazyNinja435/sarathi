package com.sarathi.app.llm

import com.sarathi.app.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatProviderSelectorTest {
    @Test
    fun select_defaultsToOfflineWhenGoogleAiStudioDisabled() {
        val prefs = UserPreferences(
            googleAiStudioEnabled = false,
            googleAiStudioApiKeyConfigured = true,
        )

        assertEquals(ChatProvider.OfflineDefault, ChatProviderSelector.select(prefs))
    }

    @Test
    fun select_defaultsToOfflineWhenApiKeyMissing() {
        val prefs = UserPreferences(
            googleAiStudioEnabled = true,
            googleAiStudioApiKeyConfigured = false,
        )

        assertEquals(ChatProvider.OfflineDefault, ChatProviderSelector.select(prefs))
    }

    @Test
    fun select_usesGoogleAiStudioOnlyWhenEnabledAndConfigured() {
        val prefs = UserPreferences(
            googleAiStudioEnabled = true,
            googleAiStudioApiKeyConfigured = true,
        )

        assertEquals(ChatProvider.GoogleAiStudio, ChatProviderSelector.select(prefs))
    }

    @Test
    fun select_ignoresLegacyPracticeModeWhenGoogleAiStudioIsConfigured() {
        val prefs = UserPreferences(
            useMockMode = true,
            googleAiStudioEnabled = true,
            googleAiStudioApiKeyConfigured = true,
        )

        assertEquals(ChatProvider.GoogleAiStudio, ChatProviderSelector.select(prefs))
    }
}
