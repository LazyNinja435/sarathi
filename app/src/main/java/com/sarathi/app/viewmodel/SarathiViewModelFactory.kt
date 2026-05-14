package com.sarathi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sarathi.app.SarathiApp

@Suppress("UNCHECKED_CAST")
class SarathiViewModelFactory(
    private val app: SarathiApp,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) ->
                ChatViewModel(app, app.userPreferencesRepository, app.ragRepository) as T
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(app.userPreferencesRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app, app.userPreferencesRepository) as T
            modelClass.isAssignableFrom(DharmaViewModel::class.java) ->
                DharmaViewModel(app.dharmaRepository) as T
            modelClass.isAssignableFrom(VerseViewModel::class.java) ->
                VerseViewModel(app.verseRepository) as T
            modelClass.isAssignableFrom(FeelViewModel::class.java) ->
                FeelViewModel() as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
