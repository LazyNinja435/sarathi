package com.sarathi.app.model

/** Which on-device backend Settings / runtime selection reflects. */
enum class LlmRuntimeKind {
    Mock,
    LiteRtLm,
    MediaPipe,
}

/** Coarse model file detection for Settings (filesystem, not JNI load proof). */
enum class LlmModelFileKind {
    Missing,
    LiteRtLm,
    MediaPipeTask,
}

/**
 * Settings-visible snapshot of LLM wiring.
 * [activeRuntime] is what Chat uses when mock mode is off; [modelFileKind] is what was found on disk.
 */
data class LlmRuntimeDiagnostics(
    val activeRuntime: LlmRuntimeKind,
    val modelFileKind: LlmModelFileKind,
    val liteRtLmPath: String?,
    val mediaPipeTaskPath: String?,
    val selectedPath: String?,
)
