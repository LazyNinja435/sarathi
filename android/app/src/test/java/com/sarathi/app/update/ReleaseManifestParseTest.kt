package com.sarathi.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseManifestParseTest {

    @Test
    fun parseAppOnlyWithModelSource() {
        val apkSha = "aa".repeat(32)
        val modelSha = "bb".repeat(32)
        val json =
            """
            {
              "schemaVersion": 1,
              "release": {
                "repo": "LazyNinja435/sarathi",
                "tag": "v0.1.0",
                "releaseType": "APP_ONLY",
                "manifestUrl": "https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json",
                "publishedAt": null
              },
              "app": {
                "packageName": "com.sarathi.app",
                "versionCode": 1,
                "versionName": "0.1.0",
                "apkFileName": "sarathi-v0.1.0.apk",
                "apkSha256": "$apkSha",
                "apkSizeBytes": 100,
                "minSupportedModelId": "gemma-4-e2b-it-litertlm",
                "supportedModelIds": ["gemma-4-e2b-it-litertlm"],
                "requiresModelUpdate": false
              },
              "model": {
                "required": false,
                "updatePolicy": "KEEP_EXISTING_IF_COMPATIBLE",
                "id": "gemma-4-e2b-it-litertlm",
                "version": "2026.04",
                "fileName": "gemma-4-E2B-it.litertlm",
                "sizeBytes": 10,
                "sha256": "$modelSha",
                "chunkSizeBytes": 943718400,
                "chunks": []
              },
              "modelSource": {
                "mode": "INLINE_OR_EXTERNAL",
                "externalManifestUrl": "https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json"
              }
            }
            """.trimIndent()

        val m = ReleaseManifest.parse(json)
        assertNotNull(m.app)
        assertEquals("0.1.0", m.app!!.versionName)
        assertTrue(m.model.chunks.isEmpty())
        assertEquals(
            "https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json",
            m.resolvedExternalModelManifestUrl(),
        )
    }

    @Test
    fun parseModelOnlyManifest() {
        val modelSha = "cc".repeat(32)
        val chunkSha = "dd".repeat(32)
        val json =
            """
            {
              "schemaVersion": 1,
              "release": {
                "repo": "LazyNinja435/sarathi",
                "tag": "model-gemma-4-e2b",
                "releaseType": "MODEL_ONLY",
                "manifestUrl": "https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json",
                "publishedAt": null
              },
              "model": {
                "required": false,
                "updatePolicy": "DOWNLOAD_IF_MISSING_OR_SHA_MISMATCH",
                "id": "gemma-4-e2b-it-litertlm",
                "version": "2026.04",
                "fileName": "gemma-4-E2B-it.litertlm",
                "sizeBytes": 2588147712,
                "sha256": "$modelSha",
                "chunkSizeBytes": 943718400,
                "chunks": [
                  {
                    "index": 1,
                    "fileName": "gemma-4-E2B-it.litertlm.part001",
                    "sizeBytes": 100,
                    "sha256": "$chunkSha"
                  }
                ]
              }
            }
            """.trimIndent()

        val m = ReleaseManifest.parse(json)
        assertNull(m.app)
        assertEquals(ReleaseManifest.ReleaseType.MODEL_ONLY, m.release.releaseType)
        assertEquals(1, m.model.chunks.size)
        assertEquals(1, m.model.chunks[0].index)
    }

    @Test
    fun parseModelCatalogWithLegacyFullModelReleaseType() {
        val modelSha = "ee".repeat(32)
        val chunkSha = "ff".repeat(32)
        val json =
            """
            {
              "schemaVersion": 1,
              "release": {
                "repo": "LazyNinja435/sarathi",
                "tag": "model-gemma-4-e2b",
                "releaseType": "FULL_MODEL",
                "manifestUrl": "https://github.com/LazyNinja435/sarathi/releases/download/model-gemma-4-e2b/model-latest.json",
                "publishedAt": null
              },
              "model": {
                "required": false,
                "updatePolicy": "DOWNLOAD_IF_MISSING_OR_SHA_MISMATCH",
                "id": "gemma-4-e2b-it-litertlm",
                "version": "2026.04",
                "fileName": "gemma-4-E2B-it.litertlm",
                "sizeBytes": 2588147712,
                "sha256": "$modelSha",
                "chunkSizeBytes": 943718400,
                "chunks": [
                  {
                    "index": 1,
                    "fileName": "gemma-4-E2B-it.litertlm.part001",
                    "sizeBytes": 100,
                    "sha256": "$chunkSha"
                  }
                ]
              }
            }
            """.trimIndent()

        val m = ReleaseManifest.parse(json)
        assertNull(m.app)
        assertEquals(ReleaseManifest.ReleaseType.FULL_MODEL, m.release.releaseType)
    }

    @Test
    fun appOnlyWithoutModelSourceFallsBackToDefaultExternalUrl() {
        val apkSha = "aa".repeat(32)
        val modelSha = "bb".repeat(32)
        val json =
            """
            {
              "schemaVersion": 1,
              "release": {
                "repo": "LazyNinja435/sarathi",
                "tag": "v0.1.0",
                "releaseType": "APP_ONLY",
                "manifestUrl": "https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json",
                "publishedAt": null
              },
              "app": {
                "packageName": "com.sarathi.app",
                "versionCode": 1,
                "versionName": "0.1.0",
                "apkFileName": "sarathi-v0.1.0.apk",
                "apkSha256": "$apkSha",
                "apkSizeBytes": 100,
                "minSupportedModelId": "gemma-4-e2b-it-litertlm",
                "supportedModelIds": ["gemma-4-e2b-it-litertlm"],
                "requiresModelUpdate": false
              },
              "model": {
                "required": false,
                "id": "gemma-4-e2b-it-litertlm",
                "version": "2026.04",
                "fileName": "gemma-4-E2B-it.litertlm",
                "sizeBytes": 10,
                "sha256": "$modelSha",
                "chunkSizeBytes": 943718400,
                "chunks": []
              }
            }
            """.trimIndent()

        val m = ReleaseManifest.parse(json)
        assertEquals(GithubReleaseClient.DEFAULT_EXTERNAL_MODEL_MANIFEST_URL, m.resolvedExternalModelManifestUrl())
    }
}
