package com.sarathi.app.llm

object GoogleAiStudioConfig {
    const val PROVIDER_NAME = "Google AI Studio"
    const val MODEL_NAME = "gemini-flash-lite-latest"
    const val GENERATE_CONTENT_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    const val TEMPERATURE = 0.7
    const val MAX_OUTPUT_TOKENS = 512
}
