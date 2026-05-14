package com.sarathi.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reflection note persisted via [UserPreferencesRepository].
 * TODO: migrate to a list/history of reflections (Room or multi-entry DataStore).
 */
class DharmaRepository(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    val dharmaNote: Flow<String> =
        userPreferencesRepository.preferences.map { it.dharmaNote }

    suspend fun save(note: String) {
        userPreferencesRepository.setDharmaNote(note.trim())
    }
}
