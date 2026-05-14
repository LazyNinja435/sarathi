package com.sarathi.app.update

import com.sarathi.app.BuildConfig

object AppUpdateManager {

    fun compareWithManifest(manifest: ReleaseManifest): UpdateComparison {
        val remote = manifest.app.versionCode
        val local = BuildConfig.VERSION_CODE
        return when {
            remote > local -> UpdateComparison.UpdateAvailable(
                versionName = manifest.app.versionName,
                apkSizeBytes = manifest.app.apkSizeBytes,
                manifest = manifest,
            )
            else -> UpdateComparison.UpToDate(
                versionName = manifest.app.versionName,
                remoteVersionCode = remote,
            )
        }
    }

    sealed class UpdateComparison {
        data class UpToDate(val versionName: String, val remoteVersionCode: Int) : UpdateComparison()
        data class UpdateAvailable(
            val versionName: String,
            val apkSizeBytes: Long,
            val manifest: ReleaseManifest,
        ) : UpdateComparison()
    }
}
