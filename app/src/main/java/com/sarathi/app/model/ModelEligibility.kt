package com.sarathi.app.model

import android.content.Context
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.update.ManifestCache
import com.sarathi.app.update.ReleaseManifest
import com.sarathi.app.update.Sha256Util
import java.io.File

/**
 * Decides whether LiteRT Gemma must be blocked (e.g. installed file no longer satisfies
 * a manifest that marks [ReleaseManifest.AppReleaseInfo.requiresModelUpdate]).
 */
object ModelEligibility {

    fun shouldBlockLiteRt(context: Context, customModelPath: String): Boolean =
        blockReason(context, customModelPath) != null

    fun blockReason(context: Context, customModelPath: String): String? {
        val path = ModelManager.resolveLiteRtLmPath(context, customModelPath) ?: return null
        val manifest = ManifestCache.readManifest(context) ?: return null
        val app = manifest.app ?: return null
        val file = File(path)
        if (!file.isFile || file.length() <= 0L) return null

        val installedMeta = InstalledModelInfo.load(context)
        val installedId = installedMeta?.id ?: InstalledModelInfo.inferModelId(file.name)
        val supported = app.supportedModelIds
        if (supported.isNotEmpty() && installedId !in supported) {
            return "This app version no longer supports the installed offline model."
        }
        if (!app.requiresModelUpdate) return null

        val expectedSha = manifest.model.sha256.lowercase()
        val metaSha = installedMeta?.sha256?.lowercase()
        if (metaSha != null && metaSha == expectedSha && file.length() == manifest.model.sizeBytes) {
            return null
        }
        val diskSha = runCatching { Sha256Util.sha256Hex(file).lowercase() }.getOrNull()
        if (diskSha != null && diskSha == expectedSha && file.length() == manifest.model.sizeBytes) {
            return null
        }
        return "A newer offline model is required for on-device wisdom."
    }
}
