package com.sarathi.app

import android.app.Application
import com.sarathi.app.data.DharmaRepository
import com.sarathi.app.data.UserPreferencesRepository
import com.sarathi.app.data.VerseRepository
import com.sarathi.app.rag.RagRepository
import com.sarathi.app.viewmodel.SarathiViewModelFactory

class SarathiApp : Application() {

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set
    lateinit var ragRepository: RagRepository
        private set
    lateinit var verseRepository: VerseRepository
        private set
    lateinit var dharmaRepository: DharmaRepository
        private set
    lateinit var viewModelFactory: SarathiViewModelFactory
        private set

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(this)
        ragRepository = RagRepository(this)
        verseRepository = VerseRepository(this, ragRepository)
        dharmaRepository = DharmaRepository(userPreferencesRepository)
        viewModelFactory = SarathiViewModelFactory(this)
    }
}
