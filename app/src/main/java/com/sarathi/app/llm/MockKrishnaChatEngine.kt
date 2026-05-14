package com.sarathi.app.llm

import com.sarathi.app.model.ChatMessage
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.rag.RagSearchResult

/**
 * Deterministic offline responses. Always safe to use when no model is present.
 */
class MockKrishnaChatEngine : ChatEngine {

    override suspend fun generateReply(
        userMessage: String,
        history: List<ChatMessage>,
        userName: String,
        tone: GuidanceTone,
        retrievedContext: List<RagSearchResult>,
    ): String {
        val m = userMessage.lowercase()
        val name = userName.ifBlank { "dear one" }
        val body = when {
            m.contains("anxious") || m.contains("anxiety") || m.contains("worry") ->
                "My dear $name, the mind runs ahead and calls its shadow truth. Come back to the breath. Come back to the duty before you. Peace is not found by holding every outcome, but by offering sincere action."

            m.contains("fail") || m.contains("failed") || m.contains("mistake") ->
                "You are looking at the fruit and calling yourself the tree. Do not measure your worth by the harvest of one season. Rise again. Offer your effort. Let the outcome rest where it belongs."

            m.contains("dharma") || m.contains("purpose") || m.contains("path") ->
                "Dharma is often not the path that flatters the mind, but the one that steadies the soul. Name the duty before you. Then take one sincere step."

            m.contains("attach") || m.contains("outcome") || m.contains("result") ->
                "Offer the work, $name, without bargaining with the future. Attachment to fruit makes the heart tremble; sincere action makes it steady."

            m.contains("verse") || (m.contains("teach") && m.contains("gita")) ->
                "Let us walk slowly with a Gita-inspired teaching: act with steadiness, speak with truth, and release the grip on reward. When you need an exact verse, consult a trusted translation; here, I offer the spirit of the teaching."

            m.contains("afraid") || m.contains("fear") ->
                "Fear visits the mind when it forgets the Self.\n\nCome back to the breath. Come back to the duty before you. Come back to the stillness within your own heart."

            m.contains("angry") || m.contains("anger") ->
                "Anger can burn the house it was meant to protect. Name the wound beneath the heat, $name. Then return to one measured action that honors both truth and peace."

            m.contains("lost") || m.contains("confused") ->
                "When the path is hidden, do not demand the whole map. Light one lamp: one truthful word, one kind act, one small duty completed with care. Clarity often follows sincerity."

            m.contains("heartbroken") || m.contains("grief") || m.contains("sad") ->
                "The heart may ache, yet you are more than this weather. Grieve if you must—gently. Then place one tender hand on the duty that still calls you, however small."

            m.contains("jealous") || m.contains("envy") ->
                "Comparison steals the sweetness of your own offering. Turn your gaze from another's harvest to the seed in your own palm, $name. Cultivate what is yours to grow."

            m.contains("proud") || m.contains("ego") ->
                "Let your joy in good work be humble and bright, like a lamp in a shrine—not a fire that blinds. Pride that serves gratitude becomes grace."

            m.contains("peace") || m.contains("calm") || m.contains("grateful") ->
                "Peace is not the absence of waves, but a depth beneath them. Rest in that depth a little longer, and carry one quiet kindness into your next action."

            m.contains("unmotivated") || m.contains("lazy") || m.contains("stuck") ->
                "Do not ask the mountain of yourself all at once. Begin with one small motion—tidy a corner, write one line, take one walk. Momentum returns to the sincere."

            else ->
                "My dear $name, speak plainly of what weighs upon you, and we shall walk with it together—one breath, one duty, one gentle step at a time."
        }
        var out = toneWrap(tone, body)
        if (retrievedContext.isNotEmpty()) {
            val lines = retrievedContext.take(3).joinToString("\n") { r ->
                "[${r.citation}] — ${r.sourceTitle}"
            }
            out += "\n\n(Inspired by:\n$lines)"
        }
        return out
    }

    private fun toneWrap(tone: GuidanceTone, body: String): String =
        when (tone) {
            GuidanceTone.Gentle -> body
            GuidanceTone.Direct -> body.replace("\n\n", "\n").trim() + "\n\nWhat is one action you will take today—small, sincere, and complete?"
            GuidanceTone.Poetic -> "Like dawn finding the lotus, listen:\n\n$body"
            GuidanceTone.Scriptural ->
                body + "\n\n— Inspired by Bhagavad Gita themes (not a verbatim citation) —"
        }
}
