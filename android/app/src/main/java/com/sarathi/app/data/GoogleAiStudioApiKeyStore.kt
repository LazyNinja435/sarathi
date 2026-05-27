package com.sarathi.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface GoogleAiStudioApiKeyStore {
    fun getApiKey(): String?
    fun saveApiKey(apiKey: String): Boolean
    fun clearApiKey(): Boolean
    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()
}

class AndroidGoogleAiStudioApiKeyStore(context: Context) : GoogleAiStudioApiKeyStore {
    private val appContext = context.applicationContext

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getApiKey(): String? = runCatching {
        prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    override fun saveApiKey(apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return false

        return runCatching {
            prefs.edit().putString(KEY_API_KEY, trimmed).commit()
        }.getOrDefault(false)
    }

    override fun clearApiKey(): Boolean = runCatching {
        prefs.edit().remove(KEY_API_KEY).commit()
    }.getOrDefault(false)

    private companion object {
        const val PREFS_NAME = "sarathi_google_ai_studio_secure"
        const val KEY_API_KEY = "api_key"
    }
}
