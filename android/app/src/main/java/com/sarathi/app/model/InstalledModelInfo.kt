package com.sarathi.app.model

import android.content.Context
import com.sarathi.app.update.Sha256Util
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * Metadata for the on-device Gemma LiteRT-LM bundle, persisted under
 * [modelsDir]/installed-model.json (app-private storage).
 */
data class InstalledModelInfo(
    val id: String,
    val version: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val installedAt: String,
    val runtime: String,
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("version", version)
        put("fileName", fileName)
        put("sizeBytes", sizeBytes)
        put("sha256", sha256)
        put("installedAt", installedAt)
        put("runtime", runtime)
    }

    companion object {
        const val DEFAULT_RUNTIME = "LiteRT-LM"

        fun fromJsonObject(o: JSONObject): InstalledModelInfo = InstalledModelInfo(
            id = o.getString("id"),
            version = o.getString("version"),
            fileName = o.getString("fileName"),
            sizeBytes = o.getLong("sizeBytes"),
            sha256 = o.getString("sha256").lowercase(),
            installedAt = o.getString("installedAt"),
            runtime = o.optString("runtime", DEFAULT_RUNTIME),
        )

        fun inferModelId(fileName: String): String = when (fileName.lowercase()) {
            "gemma-4-e2b-it.litertlm" -> "gemma-4-e2b-it-litertlm"
            "gemma4-e2b.litertlm" -> "gemma-4-e2b-it-litertlm"
            "gemma.litertlm" -> "gemma-4-e2b-it-litertlm"
            else -> fileName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }

        fun modelsDir(context: Context): File =
            File(context.filesDir, "models").apply { mkdirs() }

        fun installedMetadataFile(context: Context): File =
            File(modelsDir(context), "installed-model.json")

        fun load(context: Context): InstalledModelInfo? {
            val f = installedMetadataFile(context)
            if (!f.isFile || f.length() == 0L) return null
            return runCatching {
                fromJsonObject(JSONObject(f.readText()))
            }.getOrNull()
        }

        fun save(context: Context, info: InstalledModelInfo) {
            val f = installedMetadataFile(context)
            f.parentFile?.mkdirs()
            f.writeText(info.toJsonObject().toString(2))
        }

        fun delete(context: Context) {
            installedMetadataFile(context).delete()
        }

        /**
         * After a full SHA verification, (re)write metadata for the resolved LiteRT file.
         */
        fun persistAfterVerification(
            context: Context,
            modelFile: File,
            modelVersion: String,
            sha256Hex: String,
        ) {
            val id = inferModelId(modelFile.name)
            save(
                context,
                InstalledModelInfo(
                    id = id,
                    version = modelVersion,
                    fileName = modelFile.name,
                    sizeBytes = modelFile.length(),
                    sha256 = sha256Hex.lowercase(),
                    installedAt = Instant.now().toString(),
                    runtime = DEFAULT_RUNTIME,
                ),
            )
        }

        /**
         * Verify file size (and optionally SHA), then refresh [installed-model.json].
         * Call from a background dispatcher; SHA can be expensive for multi‑GB files.
         */
        fun verifyOrReconstructMetadata(
            context: Context,
            modelFile: File,
            expectedSize: Long?,
            expectedSha: String?,
            modelVersion: String,
        ): Result<InstalledModelInfo> = runCatching {
            if (!modelFile.isFile || modelFile.length() <= 0L) {
                error("Model file is missing or empty.")
            }
            if (expectedSize != null && modelFile.length() != expectedSize) {
                error("Model file size does not match the expected size.")
            }
            val sha = Sha256Util.sha256Hex(modelFile).lowercase()
            if (expectedSha != null && !Sha256Util.matchesExpected(sha, expectedSha)) {
                error("Model SHA-256 does not match the expected value.")
            }
            val info = InstalledModelInfo(
                id = inferModelId(modelFile.name),
                version = modelVersion,
                fileName = modelFile.name,
                sizeBytes = modelFile.length(),
                sha256 = sha,
                installedAt = Instant.now().toString(),
                runtime = DEFAULT_RUNTIME,
            )
            save(context, info)
            info
        }
    }
}
