package com.sarathi.app.rag

import org.junit.Assert.assertEquals
import org.junit.Test

class RagRepositoryTest {
    @Test
    fun gitaVerseIdUsesEnrichedKnowledgeIdFormat() {
        assertEquals("BG-2-47", RagRepository.gitaVerseId(2, 47))
    }
}
