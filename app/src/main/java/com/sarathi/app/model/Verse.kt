package com.sarathi.app.model

data class Verse(
    val chapter: Int,
    val verse: Int,
    val referenceLabel: String,
    val translation: String,
    val reflection: String,
    /** When sourced from RAG DB, short provenance line for the UI. */
    val sourceAttribution: String? = null,
)
