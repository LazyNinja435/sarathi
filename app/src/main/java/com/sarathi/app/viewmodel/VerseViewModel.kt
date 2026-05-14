package com.sarathi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.VerseRepository
import com.sarathi.app.model.Verse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VerseViewModel(
    private val repo: VerseRepository,
) : ViewModel() {

    private val _verse = MutableStateFlow<Verse?>(null)
    val verse: StateFlow<Verse?> = _verse.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _verse.value = repo.verseOfTheDay()
        }
    }
}
