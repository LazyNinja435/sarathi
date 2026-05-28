package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.ChatSessionMemory
import com.sarathi.app.model.Sender
import com.sarathi.app.model.UserMemory
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class SarathiApiChatRequest(
    val conversationId: String = "android-main",
    val latestUserMessage: String,
    val recentHistory: List<ChatMessage>,
    val userName: String,
    val shortTermMemory: ChatSessionMemory,
    val userMemory: UserMemory,
)

data class SarathiApiChatResponse(
    val assistantMessage: String,
    val provider: String?,
    val model: String?,
)

fun SarathiApiChatRequest.toJson(): JSONObject =
    JSONObject()
        .put("conversationId", conversationId)
        .put("latestUserMessage", latestUserMessage)
        .put("recentHistory", recentHistory.toApiHistoryJson())
        .put("shortTermMemory", shortTermMemory.toJson())
        .put("longTermMemory", userMemory.toJson())
        .put("userName", userName)

fun parseSarathiApiChatResponse(json: String): SarathiApiChatResponse {
    val root = JSONObject(json)
    return SarathiApiChatResponse(
        assistantMessage = root.optString("assistantMessage").trim(),
        provider = root.optString("provider").takeIf { it.isNotBlank() },
        model = root.optString("model").takeIf { it.isNotBlank() },
    )
}

private fun List<ChatMessage>.toApiHistoryJson(): JSONArray {
    val array = JSONArray()
    filter { it.sender == Sender.User || it.sender == Sender.Assistant }
        .forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", roleFor(message.sender))
                    .put("text", message.text)
                    .put("createdAt", Instant.ofEpochMilli(message.timestampMillis).toString())
                    .put("source", "android"),
            )
        }
    return array
}

private fun roleFor(sender: Sender): String = when (sender) {
    Sender.User -> "user"
    Sender.Assistant -> "assistant"
    Sender.System -> "system"
}

private fun ChatSessionMemory.toJson(): JSONObject =
    JSONObject()
        .put("currentEmotion", currentEmotion)
        .put("currentConcern", currentConcern)
        .put("importantContext", importantContext)
        .put("preferredGuidanceStyle", preferredGuidanceStyle)
        .put("lastGuidanceTheme", lastGuidanceTheme)

private fun UserMemory.toJson(): JSONObject =
    JSONObject()
        .put("preferredLanguageLevel", preferredLanguageLevel)
        .put("preferredResponseStyle", preferredResponseStyle)
        .put("spiritualTonePreference", spiritualTonePreference)
        .put("savedUserNotes", JSONArray(savedUserNotes))
