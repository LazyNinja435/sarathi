package com.sarathi.app.llm

import android.content.Context
import android.os.Environment
import com.sarathi.app.BuildConfig
import com.sarathi.app.model.ModelStatus
import java.io.File

object ModelManager {

    val EXPECTED_FILENAMES = listOf(
        "gemma.task",
        "gemma-3n.task",
        "gemma-3-1b-it.task",
        "gemma3-1b-it-int4.task",
    )

    /** Display paths for Settings (no guarantee file exists). */
    fun expectedPathHints(context: Context): List<String> {
        val filesModels = File(context.filesDir, "models").absolutePath
        val download = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sarathi",
        ).absolutePath
        val hints = EXPECTED_FILENAMES.flatMap { name ->
            listOf(
                File(filesModels, name).absolutePath,
                File(download, name).absolutePath,
            )
        }.toMutableList()
        if (BuildConfig.DEBUG) {
            hints += DEBUG_TMP_MODEL_PATH
        }
        return hints.distinct()
    }

    /**
     * Resolves first existing `.task` file:
     * 1. [customPath] if non-empty and file exists
     * 2. [context.filesDir]/models/ with expected names
     * 3. Public Download/sarathi/ (may be inaccessible on some Android versions without SAF)
     */
    fun resolveModelPath(context: Context, customPath: String = ""): String? {
        if (customPath.isNotBlank()) {
            val f = File(customPath)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        val inApp = File(context.filesDir, "models")
        EXPECTED_FILENAMES.forEach { name ->
            val f = File(inApp, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sarathi",
        )
        EXPECTED_FILENAMES.forEach { name ->
            val f = File(downloadDir, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        if (BuildConfig.DEBUG) {
            val debugTmp = File(DEBUG_TMP_MODEL_PATH)
            if (debugTmp.isFile && debugTmp.length() > 0L) return debugTmp.absolutePath
        }
        return null
    }

    /** Emulator/dev path matching MediaPipe LLM sample adb workflows; debug builds only. */
    private const val DEBUG_TMP_MODEL_PATH = "/data/local/tmp/llm/model_version.task"

    fun statusForPath(path: String?): ModelStatus =
        when {
            path.isNullOrBlank() -> ModelStatus.Missing
            else -> {
                val f = File(path)
                if (f.isFile && f.length() > 0L) ModelStatus.Installed(path)
                else ModelStatus.Missing
            }
        }
}
