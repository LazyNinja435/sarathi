package com.sarathi.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.model.UserMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sarathi_preferences")

class UserPreferencesRepository(
    private val context: Context,
) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val selectedTone = stringPreferencesKey("selected_tone")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val customModelPath = stringPreferencesKey("custom_model_path")
        val useMockMode = booleanPreferencesKey("use_mock_mode")
        val lastFeeling = stringPreferencesKey("last_feeling")
        val dharmaNote = stringPreferencesKey("dharma_note")
        val memoryPreferredLanguageLevel = stringPreferencesKey("memory_preferred_language_level")
        val memoryPreferredResponseStyle = stringPreferencesKey("memory_preferred_response_style")
        val memorySpiritualTonePreference = stringPreferencesKey("memory_spiritual_tone_preference")
        val memorySavedUserNotes = stringPreferencesKey("memory_saved_user_notes")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            userName = p[Keys.userName].orEmpty(),
            selectedTone = GuidanceTone.fromId(p[Keys.selectedTone]),
            onboardingComplete = p[Keys.onboardingComplete] ?: false,
            customModelPath = p[Keys.customModelPath].orEmpty(),
            useMockMode = p[Keys.useMockMode] ?: false,
            lastFeeling = p[Keys.lastFeeling].orEmpty(),
            dharmaNote = p[Keys.dharmaNote].orEmpty(),
            userMemory = UserMemory(
                preferredLanguageLevel = p[Keys.memoryPreferredLanguageLevel] ?: "simple",
                preferredResponseStyle = p[Keys.memoryPreferredResponseStyle]
                    ?: "quote_simple_meaning_practical_guidance",
                spiritualTonePreference = p[Keys.memorySpiritualTonePreference]
                    ?: "krishna_inspired_practical",
                savedUserNotes = decodeNotes(p[Keys.memorySavedUserNotes].orEmpty()),
            ),
        )
    }

    suspend fun setUserName(value: String) {
        context.dataStore.edit { it[Keys.userName] = value.trim() }
    }

    suspend fun setSelectedTone(tone: GuidanceTone) {
        context.dataStore.edit { it[Keys.selectedTone] = tone.id }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.onboardingComplete] = value }
    }

    suspend fun setCustomModelPath(path: String) {
        context.dataStore.edit { it[Keys.customModelPath] = path.trim() }
    }

    suspend fun setUseMockMode(value: Boolean) {
        context.dataStore.edit { it[Keys.useMockMode] = value }
    }

    suspend fun setLastFeeling(value: String) {
        context.dataStore.edit { it[Keys.lastFeeling] = value }
    }

    suspend fun setDharmaNote(value: String) {
        context.dataStore.edit { it[Keys.dharmaNote] = value }
    }

    suspend fun rememberUserNote(note: String) {
        val clean = note.trim().replace(Regex("\\s+"), " ").take(180)
        if (clean.isBlank()) return
        context.dataStore.edit { p ->
            val notes = (decodeNotes(p[Keys.memorySavedUserNotes].orEmpty()) + clean)
                .distinct()
                .takeLast(MAX_SAVED_NOTES)
            p[Keys.memorySavedUserNotes] = encodeNotes(notes)
        }
    }

    suspend fun clearUserMemory() {
        context.dataStore.edit { p ->
            p.remove(Keys.memoryPreferredLanguageLevel)
            p.remove(Keys.memoryPreferredResponseStyle)
            p.remove(Keys.memorySpiritualTonePreference)
            p.remove(Keys.memorySavedUserNotes)
        }
    }

    /** Clears onboarding progress so splash/name/tone/blessing run again; keeps model paths and practice toggle. */
    suspend fun resetOnboarding() {
        context.dataStore.edit { p ->
            p.remove(Keys.onboardingComplete)
            p.remove(Keys.userName)
            p.remove(Keys.selectedTone)
        }
    }

    private fun decodeNotes(encoded: String): List<String> {
        if (encoded.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (i in 0 until array.length()) {
                    val note = array.optString(i).trim()
                    if (note.isNotBlank()) add(note)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeNotes(notes: List<String>): String {
        val array = JSONArray()
        notes.forEach { array.put(it) }
        return array.toString()
    }

    private companion object {
        const val MAX_SAVED_NOTES = 8
    }
}
