package com.sarathi.app.llm

import android.content.Context
import android.os.Environment
import com.sarathi.app.BuildConfig
import com.sarathi.app.model.LlmModelFileKind
import com.sarathi.app.model.LlmRuntimeDiagnostics
import com.sarathi.app.model.LlmRuntimeKind
import com.sarathi.app.model.ModelStatus
import java.io.File

object ModelManager {

    /** MediaPipe LLM Inference `.task` bundles (legacy / alternate). */
    val EXPECTED_TASK_FILENAMES = listOf(
        "gemma.task",
        "gemma-3n.task",
        "gemma-3-1b-it.task",
        "gemma3-1b-it-int4.task",
    )

    @Deprecated("Use EXPECTED_TASK_FILENAMES", ReplaceWith("EXPECTED_TASK_FILENAMES"))
    val EXPECTED_FILENAMES: List<String> get() = EXPECTED_TASK_FILENAMES

    /** LiteRT-LM `.litertlm` search order (first existing file wins). */
    val LITERT_LM_FILENAMES = listOf(
        "gemma-4-E2B-it.litertlm",
        "gemma4-e2b.litertlm",
        "gemma.litertlm",
    )

    /** Display paths for Settings (no guarantee file exists). */
    fun expectedPathHints(context: Context): List<String> {
        val filesModels = File(context.filesDir, "models").absolutePath
        val download = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sarathi",
        ).absolutePath
        val hints = mutableListOf<String>()
        LITERT_LM_FILENAMES.forEach { name ->
            hints += File(filesModels, name).absolutePath
            hints += File(download, name).absolutePath
        }
        EXPECTED_TASK_FILENAMES.forEach { name ->
            hints += File(filesModels, name).absolutePath
            hints += File(download, name).absolutePath
        }
        if (BuildConfig.DEBUG) {
            hints.addAll(DEBUG_TMP_LITERT_PATHS)
            hints += DEBUG_TMP_TASK_PATH
        }
        return hints.distinct()
    }

    /**
     * Prefer LiteRT-LM, then MediaPipe `.task` (for status display and single “installed path”).
     */
    fun resolvePreferredModelPath(context: Context, customPath: String = ""): String? =
        resolveLiteRtLmPath(context, customPath) ?: resolveMediaPipeTaskPath(context, customPath)

    /**
     * Resolves first existing `.litertlm` file:
     * 1. [customPath] if set, exists, ends with `.litertlm`
     * 2. [context.filesDir]/models/ with [LITERT_LM_FILENAMES]
     * 3. Download/sarathi/ with same names
     * 4. Debug: /data/local/tmp/llm/ candidates
     */
    fun resolveLiteRtLmPath(context: Context, customPath: String = ""): String? {
        if (customPath.isNotBlank()) {
            val f = File(customPath)
            if (f.isFile && f.length() > 0L && f.name.endsWith(".litertlm", ignoreCase = true)) {
                return f.absolutePath
            }
        }
        val inApp = File(context.filesDir, "models")
        LITERT_LM_FILENAMES.forEach { name ->
            val f = File(inApp, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sarathi",
        )
        LITERT_LM_FILENAMES.forEach { name ->
            val f = File(downloadDir, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        if (BuildConfig.DEBUG) {
            DEBUG_TMP_LITERT_PATHS.forEach { p ->
                val f = File(p)
                if (f.isFile && f.length() > 0L) return f.absolutePath
            }
        }
        return null
    }

    /**
     * Resolves first existing MediaPipe `.task` file (unchanged semantics from earlier app versions).
     */
    fun resolveMediaPipeTaskPath(context: Context, customPath: String = ""): String? {
        if (customPath.isNotBlank()) {
            val f = File(customPath)
            if (f.isFile && f.length() > 0L && f.name.endsWith(".task", ignoreCase = true)) {
                return f.absolutePath
            }
        }
        val inApp = File(context.filesDir, "models")
        EXPECTED_TASK_FILENAMES.forEach { name ->
            val f = File(inApp, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sarathi",
        )
        EXPECTED_TASK_FILENAMES.forEach { name ->
            val f = File(downloadDir, name)
            if (f.isFile && f.length() > 0L) return f.absolutePath
        }
        if (BuildConfig.DEBUG) {
            val debugTmp = File(DEBUG_TMP_TASK_PATH)
            if (debugTmp.isFile && debugTmp.length() > 0L) return debugTmp.absolutePath
        }
        return null
    }

    /** @deprecated Prefer [resolveLiteRtLmPath] / [resolveMediaPipeTaskPath] / [resolvePreferredModelPath]. */
    @Deprecated("Use resolvePreferredModelPath or specific resolvers", ReplaceWith("resolvePreferredModelPath(context, customPath)"))
    fun resolveModelPath(context: Context, customPath: String = ""): String? =
        resolvePreferredModelPath(context, customPath)

    fun statusForPath(path: String?): ModelStatus =
        when {
            path.isNullOrBlank() -> ModelStatus.Missing
            else -> {
                val f = File(path)
                if (f.isFile && f.length() > 0L) ModelStatus.Installed(path)
                else ModelStatus.Missing
            }
        }

    fun diagnostics(
        context: Context,
        useMockMode: Boolean,
        customModelPath: String,
    ): LlmRuntimeDiagnostics {
        val lite = resolveLiteRtLmPath(context, customModelPath)
        val task = resolveMediaPipeTaskPath(context, customModelPath)
        val selected = lite ?: task
        val fileKind = when {
            lite != null -> LlmModelFileKind.LiteRtLm
            task != null -> LlmModelFileKind.MediaPipeTask
            else -> LlmModelFileKind.Missing
        }
        val active = when {
            lite != null -> LlmRuntimeKind.LiteRtLm
            task != null -> LlmRuntimeKind.MediaPipe
            else -> LlmRuntimeKind.Mock
        }
        return LlmRuntimeDiagnostics(
            activeRuntime = active,
            modelFileKind = fileKind,
            liteRtLmPath = lite,
            mediaPipeTaskPath = task,
            selectedPath = selected,
        )
    }

    private val DEBUG_TMP_LITERT_PATHS = listOf(
        "/data/local/tmp/llm/gemma-4-E2B-it.litertlm",
        "/data/local/tmp/llm/model.litertlm",
    )

    /** Emulator/dev path matching MediaPipe LLM sample adb workflows; debug builds only. */
    private const val DEBUG_TMP_TASK_PATH = "/data/local/tmp/llm/model_version.task"
}
