package com.sarathi.app.llm

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONException
import org.json.JSONObject

interface GeminiContentClient {
    fun generateContent(apiKey: String, request: JSONObject): String
}

class GoogleAiStudioGeminiClient(
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000,
) : GeminiContentClient {
    override fun generateContent(apiKey: String, request: JSONObject): String {
        if (apiKey.isBlank()) throw GeminiApiException.MissingApiKey
        val encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name())
        val url = URL("${GoogleAiStudioConfig.GENERATE_CONTENT_ENDPOINT}?key=$encodedKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return try {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
                it.write(request.toString())
            }
            val code = connection.responseCode
            val body = readBody(connection, code)
            when (code) {
                in 200..299 -> GeminiResponseParser.parseGenerateContentResponse(body)
                401, 403 -> throw GeminiApiException.Auth(code)
                429 -> throw GeminiApiException.Quota
                in 500..599 -> throw GeminiApiException.Server(code)
                else -> throw GeminiApiException.Server(code)
            }
        } catch (e: SocketTimeoutException) {
            throw GeminiApiException.Network(e)
        } catch (e: java.io.IOException) {
            throw GeminiApiException.Network(e)
        } catch (e: JSONException) {
            throw GeminiApiException.MalformedResponse
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(StandardCharsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
    }
}
