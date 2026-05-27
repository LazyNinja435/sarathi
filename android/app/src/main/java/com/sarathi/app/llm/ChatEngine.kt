package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory
import com.sarathi.app.rag.RagSearchResult

interface ChatEngine {
    suspend fun generateReply(
        userMessage: String,
        history: List<ChatMessage>,
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult> = emptyList(),
        sessionMemory: ChatSessionMemory = ChatSessionMemory(),
        userMemory: UserMemory = UserMemory(),
    ): String
}
