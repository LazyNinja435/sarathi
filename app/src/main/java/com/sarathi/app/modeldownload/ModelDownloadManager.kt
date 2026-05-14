package com.sarathi.app.modeldownload

import android.content.Context
import android.os.StatFs
import com.sarathi.app.update.GithubReleaseClient
import com.sarathi.app.update.ReleaseManifest
import com.sarathi.app.update.Sha256Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModelDownloadManager(
    private val context: Context,
    private val chunkDownloader: ModelChunkDownloader = ModelChunkDownloader(),
) {

    private fun downloadDir(): File = File(context.filesDir, "model-downloads").apply { mkdirs() }
    private fun modelsDir(): File = File(context.filesDir, "models").apply { mkdirs() }

    fun requiredSpaceBytes(manifest: ReleaseManifest): Long {
        val chunkSum = manifest.model.chunks.sumOf { it.sizeBytes }
        return manifest.model.sizeBytes + chunkSum + 64L * 1024 * 1024
    }

    fun hasEnoughSpace(manifest: ReleaseManifest): Boolean {
        val avail = StatFs(context.filesDir.path).availableBytes
        return avail >= requiredSpaceBytes(manifest)
    }

    suspend fun fetchManifest(url: String = GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL): ReleaseManifest =
        withContext(Dispatchers.IO) {
            val text = GithubReleaseClient.downloadText(url)
            ReleaseManifest.parse(text)
        }

    suspend fun downloadAndInstall(
        manifest: ReleaseManifest,
        onProgress: (downloaded: Long, total: Long, label: String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (manifest.model.chunks.isEmpty()) {
            throw IllegalStateException("The release manifest does not list any model chunks.")
        }
        if (!hasEnoughSpace(manifest)) {
            val mb = manifest.model.sizeBytes / (1024 * 1024)
            throw IllegalStateException(
                "Not enough free space on this device. About $mb MB is needed for the model plus temporary files.",
            )
        }
        val repo = manifest.release.repo
        val tag = manifest.release.tag
        val dir = downloadDir()
        dir.listFiles()?.forEach { it.delete() }

        val ordered = manifest.model.chunks.sortedBy { it.index }
        var done = 0L
        val total = manifest.model.sizeBytes
        ordered.forEach { chunk ->
            onProgress(done, total, "Downloading ${chunk.fileName}")
            val part = File(dir, chunk.fileName)
            chunkDownloader.downloadChunk(repo, tag, chunk, part)
            done += chunk.sizeBytes
            onProgress(done, total, "Downloaded ${chunk.fileName}")
        }

        val tmpModel = File(modelsDir(), "${manifest.model.fileName}.tmp")
        val finalModel = File(modelsDir(), manifest.model.fileName)
        if (tmpModel.exists()) tmpModel.delete()

        FileOutputStream(tmpModel).use { out ->
            ordered.forEach { chunk ->
                val part = File(dir, chunk.fileName)
                part.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
        }

        onProgress(total, total, "Verifying model")
        val actual = Sha256Util.sha256Hex(tmpModel)
        if (!Sha256Util.matchesExpected(actual, manifest.model.sha256)) {
            tmpModel.delete()
            dir.listFiles()?.forEach { it.delete() }
            throw IllegalStateException("Full model checksum did not match. Files were discarded.")
        }
        if (tmpModel.length() != manifest.model.sizeBytes) {
            tmpModel.delete()
            dir.listFiles()?.forEach { it.delete() }
            throw IllegalStateException("Assembled model size did not match the manifest.")
        }
        if (finalModel.exists()) finalModel.delete()
        if (!tmpModel.renameTo(finalModel)) {
            tmpModel.copyTo(finalModel, overwrite = true)
            tmpModel.delete()
        }
        dir.listFiles()?.forEach { it.delete() }
    }
}
