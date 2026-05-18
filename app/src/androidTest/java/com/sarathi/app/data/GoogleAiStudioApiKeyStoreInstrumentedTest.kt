package com.sarathi.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleAiStudioApiKeyStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AndroidGoogleAiStudioApiKeyStore(context)

    @After
    fun tearDown() {
        store.clearApiKey()
    }

    @Test
    fun encryptedStore_persistsKeyInAppPrivateStorage() {
        assertTrue(store.saveApiKey("  ai-studio-test-key  "))

        val reopenedStore = AndroidGoogleAiStudioApiKeyStore(context)

        assertTrue(reopenedStore.hasApiKey())
        assertEquals("ai-studio-test-key", reopenedStore.getApiKey())
    }

    @Test
    fun encryptedStore_rejectsBlankKey() {
        assertFalse(store.saveApiKey("   "))
        assertFalse(store.hasApiKey())
    }
}
