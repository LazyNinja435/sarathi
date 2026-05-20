package com.sarathi.app.viewmodel

internal fun buildChatWelcomeMessage(userName: String): String {
    val greetingName = userName.trim().ifBlank { "one" }
    return buildString {
        appendLine("My dear $greetingName,")
        appendLine()
        appendLine("I have been seated quietly in the chariot of your heart.")
        appendLine()
        appendLine("Tell me \u2014 what battle stands before you today?")
    }.trim()
}
