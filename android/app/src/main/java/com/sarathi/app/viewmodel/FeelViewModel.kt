package com.sarathi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.sarathi.app.model.Emotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeelViewModel : ViewModel() {
    private val _selected = MutableStateFlow<Emotion?>(null)
    val selected: StateFlow<Emotion?> = _selected.asStateFlow()

    fun select(emotion: Emotion) {
        _selected.value = emotion
    }
}
