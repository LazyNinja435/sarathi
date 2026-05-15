package com.sarathi.app.model

import android.content.Context
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.update.ManifestCache
import com.sarathi.app.update.ReleaseManifest
import java.io.File

/**
 * User-facing states for Settings and offline Gemma availability (separate from app APK updates).
 */
sealed class OnDeviceWisdomStatus {
    data object Missing : OnDeviceWisdomStatus()
    data object Ready : OnDeviceWisdomStatus()
    data class OptionalModelUpdate(val approxSizeLabel: String) : OnDeviceWisdomStatus()
    data object ModelUpdateRequired : OnDeviceWisdomStatus()

    companion object {
        fun evaluate(context: Context, customModelPath: String): OnDeviceWisdomStatus {
            val path = ModelManager.resolveLiteRtLmPath(context, customModelPath)
            val manifest = ManifestCache.readManifest(context)
            if (path == null) return Missing
            val file = File(path)
            if (!file.isFile || file.length() <= 0L) return Missing

            val installedMeta = InstalledModelInfo.load(context)
            val installedId = installedMeta?.id ?: InstalledModelInfo.inferModelId(file.name)

            if (manifest != null) {
                val app = manifest.app
                if (app != null) {
                    val supported = app.supportedModelIds
                    if (supported.isNotEmpty() && installedId !in supported) {
                        return ModelUpdateRequired
                    }
                    if (app.requiresModelUpdate) {
                        val shaOk = when {
                            installedMeta != null &&
                                installedMeta.sha256.equals(manifest.model.sha256, ignoreCase = true) &&
                                installedMeta.sizeBytes == manifest.model.sizeBytes -> true
                            else -> false
                        }
                        if (!shaOk) return ModelUpdateRequired
                    }
                }
                if (
                    (
                        manifest.release.releaseType == ReleaseManifest.ReleaseType.FULL_MODEL ||
                            manifest.release.releaseType == ReleaseManifest.ReleaseType.MODEL_ONLY
                    ) &&
                    manifest.app?.requiresModelUpdate != true
                ) {
                    val sizeMismatch = file.length() != manifest.model.sizeBytes
                    val metaSha = installedMeta?.sha256?.lowercase()
                    val shaMismatch = when {
                        metaSha == null -> false
                        !metaSha.equals(manifest.model.sha256, ignoreCase = true) -> true
                        else -> false
                    }
                    if (sizeMismatch || shaMismatch) {
                        return OptionalModelUpdate(approxSizeBytes(manifest.model.sizeBytes))
                    }
                }
            }
            return Ready
        }

        private fun approxSizeBytes(bytes: Long): String {
            val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) String.format("about %.1f GB", gb) else "${bytes / (1024 * 1024)} MB"
        }
    }
}
