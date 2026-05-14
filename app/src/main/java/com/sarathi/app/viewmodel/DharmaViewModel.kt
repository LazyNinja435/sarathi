package com.sarathi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.data.DharmaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DharmaViewModel(
    private val repo: DharmaRepository,
) : ViewModel() {

    val note: StateFlow<String> = repo.dharmaNote.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )

    fun save(note: String) {
        viewModelScope.launch { repo.save(note) }
    }
}
