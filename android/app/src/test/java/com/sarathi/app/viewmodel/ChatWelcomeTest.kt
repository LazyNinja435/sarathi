package com.sarathi.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatWelcomeTest {

    @Test
    fun welcomeForBlankNameDoesNotRepeatDear() {
        val welcome = buildChatWelcomeMessage("")

        assertEquals(
            "My dear one,\n\n" +
                "I have been seated quietly in the chariot of your heart.\n\n" +
                "Tell me \u2014 what battle stands before you today?",
            welcome,
        )
        assertFalse(welcome.contains("dear dear", ignoreCase = true))
    }

    @Test
    fun welcomeForNamedUserKeepsSingleDearGreeting() {
        val welcome = buildChatWelcomeMessage("Pruthvi")

        assertEquals(
            "My dear Pruthvi,\n\n" +
                "I have been seated quietly in the chariot of your heart.\n\n" +
                "Tell me \u2014 what battle stands before you today?",
            welcome,
        )
    }
}
