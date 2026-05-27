package com.sarathi.app.data

import android.content.Context
import com.sarathi.app.model.Verse
import com.sarathi.app.rag.RagRepository
import com.sarathi.app.rag.RagSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate

class VerseRepository(
    private val context: Context,
    private val rag: RagRepository,
) {

    private val verses: List<Verse> by lazy {
        try {
            val json = context.assets.open("verses.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Verse(
                            chapter = o.getInt("chapter"),
                            verse = o.getInt("verse"),
                            referenceLabel = o.getString("referenceLabel"),
                            translation = o.getString("translation"),
                            reflection = o.getString("reflection"),
                            sourceAttribution = null,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            listOf(
                Verse(
                    chapter = 2,
                    verse = 47,
                    referenceLabel = "Bhagavad Gita 2.47",
                    translation = "You have a right to action,\nbut not to the fruits of action.",
                    reflection = "Give yourself fully to action, dear one, but do not become a servant of the result.",
                    sourceAttribution = null,
                ),
            )
        }
    }

    suspend fun verseOfTheDay(): Verse = withContext(Dispatchers.IO) {
        if (rag.warmUp()) {
            val r = rag.getVerseOfDay()
            if (r != null) return@withContext r.toVerse()
        }
        if (verses.isEmpty()) {
            Verse(
                chapter = 2,
                verse = 47,
                referenceLabel = "Bhagavad Gita 2.47",
                translation = "You have a right to action,\nbut not to the fruits of action.",
                reflection = "Give yourself fully to action, dear one, but do not become a servant of the result. Peace arrives when effort is sincere and the heart is unchained from reward.",
                sourceAttribution = null,
            )
        } else {
            val day = LocalDate.now().dayOfYear
            verses[day % verses.size]
        }
    }

    fun allVerses(): List<Verse> = verses

    private fun RagSearchResult.toVerse(): Verse {
        val parts = id.split("_")
        val ch = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val vs = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val trans = translation.ifBlank { text }
        val reflection = if (themes.isNotEmpty()) {
            "Themes to sit with: ${themes.joinToString(", ")}."
        } else {
            "Sit quietly with this teaching; let one honest breath steady the heart."
        }
        val attr = listOf(sourceTitle, sourceUrl).filter { it.isNotBlank() }.joinToString(" — ")
        return Verse(
            chapter = ch,
            verse = vs,
            referenceLabel = citation.ifBlank { title },
            translation = trans,
            reflection = reflection,
            sourceAttribution = attr.ifBlank { null },
        )
    }
}
