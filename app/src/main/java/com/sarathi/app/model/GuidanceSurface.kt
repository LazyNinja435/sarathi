package com.sarathi.app.model

/**
 * User-facing runtime label for the guidance surface (avoid prominent "Mock" wording).
 */
enum class GuidanceSurface {
    Practice,
    GoogleAiStudio,
    OnDeviceGemma,
    OnDeviceMediaPipe,
    OfflineGuidance,
    /** LiteRT file may exist but must be replaced before Gemma can run (manifest policy). */
    ModelUpdateRequired,
}
