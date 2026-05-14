package com.sarathi.app.llm

import java.util.concurrent.atomic.AtomicReference

/**
 * Session-scoped last LLM inference error for developer diagnostics (cleared on success).
 */
object LlmLastErrorStore {

    private val ref = AtomicReference<String?>(null)

    fun set(message: String?) {
        ref.set(message?.trim()?.take(2000))
    }

    fun clear() {
        ref.set(null)
    }

    fun peek(): String? = ref.get()
}
