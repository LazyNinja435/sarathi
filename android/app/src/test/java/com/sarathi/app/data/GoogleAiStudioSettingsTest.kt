package com.sarathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAiStudioSettingsTest {
    @Test
    fun userPreferences_defaultsGoogleAiStudioOffAndUnconfigured() {
        val prefs = UserPreferences()

        assertFalse(prefs.googleAiStudioEnabled)
        assertFalse(prefs.googleAiStudioApiKeyConfigured)
    }

    @Test
    fun apiKeyStore_detectsSaveAndClearWithoutExposingThroughPreferences() {
        val store = InMemoryGoogleAiStudioApiKeyStore()
        val prefs = UserPreferences(
            googleAiStudioEnabled = true,
            googleAiStudioApiKeyConfigured = store.hasApiKey(),
        )

        assertFalse(prefs.googleAiStudioApiKeyConfigured)

        assertTrue(store.saveApiKey("  secret-key  "))
        val configured = prefs.copy(googleAiStudioApiKeyConfigured = store.hasApiKey())

        assertTrue(configured.googleAiStudioApiKeyConfigured)
        assertEquals("secret-key", store.getApiKey())

        assertTrue(store.clearApiKey())
        assertFalse(store.hasApiKey())
    }

    private class InMemoryGoogleAiStudioApiKeyStore : GoogleAiStudioApiKeyStore {
        private var key: String? = null

        override fun getApiKey(): String? = key

        override fun saveApiKey(apiKey: String): Boolean {
            val trimmed = apiKey.trim()
            if (trimmed.isBlank()) return false
            key = trimmed
            return true
        }

        override fun clearApiKey(): Boolean {
            key = null
            return true
        }
    }
}
