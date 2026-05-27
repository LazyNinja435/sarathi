package com.sarathi.app.update

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Persists the last successfully fetched release manifest JSON so chat/runtime can
 * evaluate [com.sarathi.app.model.ModelEligibility] without a network call.
 */
object ManifestCache {

    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    private fun cacheFile(context: Context): File {
        val dir = File(context.filesDir, "cache").apply { mkdirs() }
        return File(dir, "last-release-manifest.json")
    }

    fun save(context: Context, jsonText: String) {
        val f = cacheFile(context)
        f.parentFile?.mkdirs()
        f.writeText(jsonText)
        _revision.value = _revision.value + 1L
    }

    fun readJson(context: Context): String? {
        val f = cacheFile(context)
        if (!f.isFile || f.length() == 0L) return null
        return runCatching { f.readText() }.getOrNull()
    }

    fun readManifest(context: Context): ReleaseManifest? {
        val text = readJson(context) ?: return null
        return runCatching { ReleaseManifest.parse(text) }.getOrNull()
    }
}
