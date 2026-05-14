package com.sarathi.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sarathi.app.model.GuidanceTone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sarathi_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val selectedTone = stringPreferencesKey("selected_tone")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val customModelPath = stringPreferencesKey("custom_model_path")
        val useMockMode = booleanPreferencesKey("use_mock_mode")
        val lastFeeling = stringPreferencesKey("last_feeling")
        val dharmaNote = stringPreferencesKey("dharma_note")
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

    /** Clears onboarding progress so splash/name/tone/blessing run again; keeps model paths and practice toggle. */
    suspend fun resetOnboarding() {
        context.dataStore.edit { p ->
            p.remove(Keys.onboardingComplete)
            p.remove(Keys.userName)
            p.remove(Keys.selectedTone)
        }
    }
}
