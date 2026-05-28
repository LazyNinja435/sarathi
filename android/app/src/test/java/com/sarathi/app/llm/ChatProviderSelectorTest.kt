package com.sarathi.app.llm

import com.sarathi.app.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatProviderSelectorTest {
    @Test
    fun select_usesServerManagedCloudByDefault() {
        assertEquals(ChatProvider.ServerManagedCloud, ChatProviderSelector.select(UserPreferences()))
    }

    @Test
    fun select_usesOfflineDefaultWhenPracticeModeIsOn() {
        assertEquals(
            ChatProvider.OfflineDefault,
            ChatProviderSelector.select(UserPreferences(useMockMode = true)),
        )
    }
}
