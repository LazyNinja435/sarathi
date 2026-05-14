package com.sarathi.app.update

import org.json.JSONArray
import org.json.JSONObject

data class ReleaseManifest(
    val app: AppReleaseInfo,
    val model: ModelReleaseInfo,
    val release: ReleaseMeta,
) {
    data class AppReleaseInfo(
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
        val apkFileName: String,
        val apkSha256: String,
        val apkSizeBytes: Long,
    )

    data class ModelChunkInfo(
        val index: Int,
        val fileName: String,
        val sizeBytes: Long,
        val sha256: String,
    )

    data class ModelReleaseInfo(
        val id: String,
        val fileName: String,
        val sizeBytes: Long,
        val sha256: String,
        val chunkSizeBytes: Long,
        val chunks: List<ModelChunkInfo>,
    )

    data class ReleaseMeta(
        val repo: String,
        val tag: String,
        val manifestUrl: String,
    )

    companion object {
        fun parse(jsonText: String): ReleaseManifest {
            val root = JSONObject(jsonText)
            val app = root.getJSONObject("app")
            val model = root.getJSONObject("model")
            val release = root.getJSONObject("release")
            val chunksJson: JSONArray = model.getJSONArray("chunks")
            val chunks = buildList {
                for (i in 0 until chunksJson.length()) {
                    val c = chunksJson.getJSONObject(i)
                    add(
                        ModelChunkInfo(
                            index = c.getInt("index"),
                            fileName = c.getString("fileName"),
                            sizeBytes = c.getLong("sizeBytes"),
                            sha256 = c.getString("sha256").lowercase(),
                        ),
                    )
                }
            }.sortedBy { it.index }
            return ReleaseManifest(
                app = AppReleaseInfo(
                    packageName = app.getString("packageName"),
                    versionCode = app.getInt("versionCode"),
                    versionName = app.getString("versionName"),
                    apkFileName = app.getString("apkFileName"),
                    apkSha256 = app.getString("apkSha256").lowercase(),
                    apkSizeBytes = app.getLong("apkSizeBytes"),
                ),
                model = ModelReleaseInfo(
                    id = model.getString("id"),
                    fileName = model.getString("fileName"),
                    sizeBytes = model.getLong("sizeBytes"),
                    sha256 = model.getString("sha256").lowercase(),
                    chunkSizeBytes = model.getLong("chunkSizeBytes"),
                    chunks = chunks,
                ),
                release = ReleaseMeta(
                    repo = release.getString("repo"),
                    tag = release.getString("tag"),
                    manifestUrl = release.getString("manifestUrl"),
                ),
            )
        }

        fun assetDownloadUrl(repo: String, tag: String, fileName: String): String =
            "https://github.com/$repo/releases/download/$tag/$fileName"
    }
}
