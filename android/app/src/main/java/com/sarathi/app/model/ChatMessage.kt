package com.sarathi.app.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)
