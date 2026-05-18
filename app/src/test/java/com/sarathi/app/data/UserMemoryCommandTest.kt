package com.sarathi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMemoryCommandTest {
    @Test
    fun arbitraryPersonalDetailIsNotSavedWithoutExplicitRememberWording() {
        val note = UserMemoryCommand.extractExplicitMemory("I am anxious about my family today.")

        assertNull(note)
    }

    @Test
    fun explicitRememberWordingExtractsMemory() {
        val note = UserMemoryCommand.extractExplicitMemory("Remember that I prefer short answers.")

        assertEquals("I prefer short answers", note)
    }
}
