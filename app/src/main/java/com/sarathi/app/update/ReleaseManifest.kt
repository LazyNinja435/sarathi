package com.sarathi.app.update



import org.json.JSONArray

import org.json.JSONObject



data class ReleaseManifest(

    val schemaVersion: Int,

    val app: AppReleaseInfo?,

    val model: ModelReleaseInfo,

    val release: ReleaseMeta,

    val modelSource: ModelSourceInfo?,

) {

    enum class ReleaseType {

        APP_ONLY,

        FULL_MODEL,

        /** Chunked model catalog published under a stable tag (no APK section). */
        MODEL_ONLY,

    }



    data class AppReleaseInfo(

        val packageName: String,

        val versionCode: Int,

        val versionName: String,

        val apkFileName: String,

        val apkSha256: String,

        val apkSizeBytes: Long,

        val minSupportedModelId: String,

        val supportedModelIds: List<String>,

        val requiresModelUpdate: Boolean,

    )



    data class ModelChunkInfo(

        val index: Int,

        val fileName: String,

        val sizeBytes: Long,

        val sha256: String,

    )



    data class ModelReleaseInfo(

        val required: Boolean,

        val updatePolicy: String,

        val id: String,

        val version: String,

        val fileName: String,

        val sizeBytes: Long,

        val sha256: String,

        val chunkSizeBytes: Long,

        val chunks: List<ModelChunkInfo>,

    )



    data class ReleaseMeta(

        val repo: String,

        val tag: String,

        val releaseType: ReleaseType,

        val manifestUrl: String,

        val publishedAt: String?,

    )



    data class ModelSourceInfo(

        val mode: String,

        val externalManifestUrl: String?,

    )



    /**

     * URL for the stable model-only manifest when inline chunks are absent.

     * Order: explicit [modelSource.externalManifestUrl], else bundled default for APP_ONLY + empty chunks.

     */

    fun resolvedExternalModelManifestUrl(): String? {

        val explicit = modelSource?.externalManifestUrl?.trim().orEmpty()

        if (explicit.isNotEmpty()) return explicit

        if (release.releaseType == ReleaseType.APP_ONLY && model.chunks.isEmpty()) {

            return GithubReleaseClient.DEFAULT_EXTERNAL_MODEL_MANIFEST_URL

        }

        return null

    }



    companion object {

        fun parse(jsonText: String): ReleaseManifest {

            val root = JSONObject(jsonText)

            val schemaVersion = root.optInt("schemaVersion", 0)



            val appJson: JSONObject? =

                if (root.has("app") && !root.isNull("app")) root.getJSONObject("app") else null



            val modelJson = root.getJSONObject("model")

            val releaseJson = root.getJSONObject("release")



            val modelSource = if (root.has("modelSource") && !root.isNull("modelSource")) {

                val ms = root.getJSONObject("modelSource")

                ModelSourceInfo(

                    mode = ms.optString("mode", "INLINE_OR_EXTERNAL"),

                    externalManifestUrl = ms.optString("externalManifestUrl", "").trim()

                        .takeIf { it.isNotEmpty() },

                )

            } else {

                null

            }



            val chunksJson: JSONArray? = modelJson.optJSONArray("chunks")

            val chunks = if (chunksJson == null || chunksJson.length() == 0) {

                emptyList()

            } else {

                buildList {

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

            }



            val releaseTypeStr = releaseJson.optString("releaseType", "")

            val releaseType = when {

                releaseTypeStr.equals("APP_ONLY", ignoreCase = true) -> ReleaseType.APP_ONLY

                releaseTypeStr.equals("MODEL_ONLY", ignoreCase = true) -> ReleaseType.MODEL_ONLY

                releaseTypeStr.equals("FULL_MODEL", ignoreCase = true) -> ReleaseType.FULL_MODEL

                appJson == null && chunks.isNotEmpty() -> ReleaseType.MODEL_ONLY

                chunks.isNotEmpty() -> ReleaseType.FULL_MODEL

                else -> ReleaseType.APP_ONLY

            }



            val modelId = modelJson.getString("id")

            val supportedIds = if (appJson != null && appJson.has("supportedModelIds") && !appJson.isNull("supportedModelIds")) {

                val arr = appJson.getJSONArray("supportedModelIds")

                buildList {

                    for (i in 0 until arr.length()) {

                        add(arr.getString(i))

                    }

                }

            } else {

                listOf(modelId)

            }



            val requiresModelUpdate =

                if (appJson != null && appJson.has("requiresModelUpdate")) {

                    appJson.getBoolean("requiresModelUpdate")

                } else {

                    false

                }



            val minSupported = if (appJson != null) {

                appJson.optString("minSupportedModelId", modelId).ifBlank { modelId }

            } else {

                modelId

            }



            val modelRequired = if (modelJson.has("required")) {

                modelJson.getBoolean("required")

            } else {

                chunks.isNotEmpty()

            }



            val updatePolicy = modelJson.optString(

                "updatePolicy",

                if (releaseType == ReleaseType.APP_ONLY && chunks.isEmpty()) {

                    "KEEP_EXISTING_IF_COMPATIBLE"

                } else {

                    "DOWNLOAD_IF_MISSING_OR_SHA_MISMATCH"

                },

            )



            val modelVersion = modelJson.optString("version", "")



            val app = appJson?.let { aj ->

                AppReleaseInfo(

                    packageName = aj.getString("packageName"),

                    versionCode = aj.getInt("versionCode"),

                    versionName = aj.getString("versionName"),

                    apkFileName = aj.getString("apkFileName"),

                    apkSha256 = aj.getString("apkSha256").lowercase(),

                    apkSizeBytes = aj.getLong("apkSizeBytes"),

                    minSupportedModelId = minSupported,

                    supportedModelIds = supportedIds,

                    requiresModelUpdate = requiresModelUpdate,

                )

            }



            return ReleaseManifest(

                schemaVersion = schemaVersion,

                app = app,

                model = ModelReleaseInfo(

                    required = modelRequired,

                    updatePolicy = updatePolicy,

                    id = modelId,

                    version = modelVersion,

                    fileName = modelJson.getString("fileName"),

                    sizeBytes = modelJson.getLong("sizeBytes"),

                    sha256 = modelJson.getString("sha256").lowercase(),

                    chunkSizeBytes = modelJson.optLong("chunkSizeBytes", 943_718_400L),

                    chunks = chunks,

                ),

                release = ReleaseMeta(

                    repo = releaseJson.getString("repo"),

                    tag = releaseJson.getString("tag"),

                    releaseType = releaseType,

                    manifestUrl = releaseJson.getString("manifestUrl"),

                    publishedAt = when {

                        !releaseJson.has("publishedAt") -> null

                        releaseJson.isNull("publishedAt") -> null

                        else -> releaseJson.optString("publishedAt", "")?.takeIf { it.isNotBlank() }

                    },

                ),

                modelSource = modelSource,

            )

        }



        fun assetDownloadUrl(repo: String, tag: String, fileName: String): String =

            "https://github.com/$repo/releases/download/$tag/$fileName"

    }

}

