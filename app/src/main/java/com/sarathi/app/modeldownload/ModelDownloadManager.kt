package com.sarathi.app.modeldownload

import android.content.Context
import android.os.StatFs
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.InstalledModelInfo
import com.sarathi.app.update.GithubReleaseClient
import com.sarathi.app.update.ManifestCache
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

    /**
     * Resolves which manifest describes downloadable model chunks:
     * 1) Cached app manifest (from update check) if it already lists chunks.
     * 2) Otherwise same cached/fetched app manifest; if chunks are empty, fetch
     *    [ReleaseManifest.resolvedExternalModelManifestUrl] when present.
     * Does not replace the app manifest cache with a model-only JSON payload.
     */
    suspend fun resolveManifestForModelDownload(
        manifestUrl: String = GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL,
    ): ReleaseManifest = withContext(Dispatchers.IO) {
        val cachedJson = ManifestCache.readJson(context)
        val appManifest = when {
            cachedJson != null -> {
                val parsed = runCatching { ReleaseManifest.parse(cachedJson) }.getOrNull()
                if (parsed?.app != null) {
                    parsed
                } else {
                    fetchAndParseAppManifest(manifestUrl)
                }
            }
            else -> fetchAndParseAppManifest(manifestUrl)
        }

        if (appManifest.model.chunks.isNotEmpty()) {
            return@withContext appManifest
        }

        val externalUrl = appManifest.resolvedExternalModelManifestUrl()
            ?: throw IllegalStateException(
                "This Sarathi release does not list an offline model download. " +
                    "Try again after the team publishes a model release.",
            )

        val externalText = try {
            GithubReleaseClient.downloadText(externalUrl)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Could not load the offline model catalog. Check your network connection and try again. " +
                    "Vaikuntha is unreachable right now.",
                e,
            )
        }
        val modelManifest = runCatching { ReleaseManifest.parse(externalText) }.getOrElse {
            throw IllegalStateException(
                "The offline model catalog could not be read. Please try again later.",
                it,
            )
        }
        if (modelManifest.model.chunks.isEmpty()) {
            throw IllegalStateException(
                "The offline model catalog is missing downloadable parts. Please try again later.",
            )
        }
        modelManifest
    }

    private suspend fun fetchAndParseAppManifest(
        manifestUrl: String,
    ): ReleaseManifest {
        val text = GithubReleaseClient.downloadText(manifestUrl)
        val parsed = ReleaseManifest.parse(text)
        if (parsed.app == null) {
            throw IllegalStateException(
                "The Sarathi release manifest is missing app details. Try again later.",
            )
        }
        ManifestCache.save(context, text)
        return parsed
    }

    /**
     * Returns true when the manifest lists chunk assets that can be assembled into the model file.
     */
    fun canDownloadChunks(manifest: ReleaseManifest): Boolean = manifest.model.chunks.isNotEmpty()

    /**
     * Verifies the on-disk LiteRT model (size and optional SHA against [manifest]) and refreshes
     * [InstalledModelInfo] metadata. Does not download release chunks.
     */
    suspend fun verifyInstalledModel(
        manifest: ReleaseManifest,
        onProgress: (String) -> Unit,
    ): InstalledModelInfo = withContext(Dispatchers.IO) {
        onProgress("Locating model file")
        val path = ModelManager.resolveLiteRtLmPath(context, customPath = "")
            ?: error("No LiteRT-LM model file was found in app-private storage.")
        val file = File(path)
        onProgress("Verifying model (SHA may take a while)")
        val version = manifest.model.version.ifBlank { "unknown" }
        InstalledModelInfo.verifyOrReconstructMetadata(
            context = context,
            modelFile = file,
            expectedSize = manifest.model.sizeBytes,
            expectedSha = manifest.model.sha256,
            modelVersion = version,
        ).getOrElse { throw it }
    }

    suspend fun downloadAndInstall(
        manifest: ReleaseManifest,
        action: ModelDownloadAction,
        onProgress: (downloaded: Long, total: Long, label: String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        when (action) {
            ModelDownloadAction.KEEP_EXISTING_MODEL -> return@withContext
            ModelDownloadAction.VERIFY_MODEL -> {
                verifyInstalledModel(manifest) { label -> onProgress(0L, 1L, label) }
                return@withContext
            }
            ModelDownloadAction.INSTALL_MODEL,
            ModelDownloadAction.UPDATE_MODEL,
            -> { /* continue */ }
        }

        if (manifest.model.chunks.isEmpty()) {
            throw IllegalStateException(
                "This Sarathi release does not ship downloadable model parts. " +
                    "Use a full or model release from GitHub, or copy the .litertlm into app-private storage manually.",
            )
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

        val version = manifest.model.version.ifBlank { "unknown" }
        InstalledModelInfo.persistAfterVerification(
            context = context,
            modelFile = finalModel,
            modelVersion = version,
            sha256Hex = actual,
        )
    }
}
