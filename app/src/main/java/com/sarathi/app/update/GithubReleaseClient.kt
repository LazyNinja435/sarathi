package com.sarathi.app.update

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GithubReleaseClient {

    const val DEFAULT_LATEST_MANIFEST_URL =
        "https://github.com/LazyNinja435/sarathi/releases/latest/download/sarathi-latest.json"

    suspend fun downloadText(url: String): String = withContext(Dispatchers.IO) {
        requireHttps(url)
        val conn = openConnection(url, 120_000)
        try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun downloadToFile(url: String, dest: File, readTimeoutMs: Int = 120_000) = withContext(Dispatchers.IO) {
        requireHttps(url)
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        if (tmp.exists()) tmp.delete()
        val conn = openConnection(url, readTimeoutMs)
        try {
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                    }
                }
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun requireHttps(url: String) {
        val scheme = Uri.parse(url).scheme ?: throw IllegalArgumentException("Invalid URL.")
        require(scheme.equals("https", ignoreCase = true)) {
            "Only HTTPS URLs are supported."
        }
    }

    private fun openConnection(url: String, readTimeoutMs: Int): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = readTimeoutMs
        conn.setRequestProperty("Accept", "application/json, application/octet-stream, */*")
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            conn.disconnect()
            throw IllegalStateException("HTTP $code for $url ${err.take(200)}")
        }
        return conn
    }
}
