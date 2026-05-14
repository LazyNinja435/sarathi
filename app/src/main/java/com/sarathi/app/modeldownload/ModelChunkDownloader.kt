package com.sarathi.app.modeldownload

import com.sarathi.app.update.GithubReleaseClient
import com.sarathi.app.update.ReleaseManifest
import com.sarathi.app.update.Sha256Util
import java.io.File

class ModelChunkDownloader {

    suspend fun downloadChunk(
        repo: String,
        tag: String,
        chunk: ReleaseManifest.ModelChunkInfo,
        dest: File,
    ) {
        dest.parentFile?.mkdirs()
        val url = ReleaseManifest.assetDownloadUrl(repo, tag, chunk.fileName)
        if (dest.exists()) dest.delete()
        GithubReleaseClient.downloadToFile(url, dest, readTimeoutMs = 0)
        val actual = Sha256Util.sha256Hex(dest)
        if (!Sha256Util.matchesExpected(actual, chunk.sha256)) {
            dest.delete()
            throw IllegalStateException("Chunk ${chunk.fileName} failed checksum verification.")
        }
        if (dest.length() != chunk.sizeBytes) {
            dest.delete()
            throw IllegalStateException("Chunk ${chunk.fileName} size did not match the manifest.")
        }
    }
}
