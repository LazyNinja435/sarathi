package com.sarathi.app.update

import android.content.Context
import com.sarathi.app.BuildConfig
import java.io.File

class UpdateDownloadManager(private val context: Context) {

    fun updateApkFile(): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(dir, "sarathi-update.apk")
    }

    suspend fun downloadApk(manifest: ReleaseManifest) {
        val url = ReleaseManifest.assetDownloadUrl(
            manifest.release.repo,
            manifest.release.tag,
            manifest.app.apkFileName,
        )
        val dest = updateApkFile()
        if (dest.exists()) dest.delete()
        GithubReleaseClient.downloadToFile(url, dest)
        val actual = Sha256Util.sha256Hex(dest)
        if (!Sha256Util.matchesExpected(actual, manifest.app.apkSha256)) {
            dest.delete()
            throw IllegalStateException("APK checksum did not match the manifest. The download was discarded.")
        }
        if (dest.length() != manifest.app.apkSizeBytes) {
            dest.delete()
            throw IllegalStateException("APK size did not match the manifest. The download was discarded.")
        }
        if (manifest.app.packageName != BuildConfig.APPLICATION_ID) {
            dest.delete()
            throw IllegalStateException("Manifest package name does not match this app.")
        }
    }
}
