package com.sarathi.app.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidanceRetrievalHintsTest {
    @Test
    fun failurePromptsIncludeActionWithoutAttachmentVerses() {
        val hint = GuidanceRetrievalHints.forMessage("I failed at something")

        assertEquals(GuidanceRetrievalHints.VerseRef(2, 47), hint.verseRefs.first())
        assertTrue(hint.verseRefs.contains(GuidanceRetrievalHints.VerseRef(2, 48)))
        assertTrue(GuidanceRetrievalHints.expandedQuery("I failed").contains("fruits of action"))
    }

    @Test
    fun unknownPromptsDoNotAddHints() {
        val hint = GuidanceRetrievalHints.forMessage("hello")

        assertTrue(hint.verseRefs.isEmpty())
        assertTrue(hint.queryTerms.isEmpty())
    }
}
