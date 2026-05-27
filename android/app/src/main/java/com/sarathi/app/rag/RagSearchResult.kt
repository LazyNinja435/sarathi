package com.sarathi.app.rag

/**
 * Single row from FTS retrieval or direct lookup, mapped for UI and prompt building.
 */
data class RagSearchResult(
    val id: String,
    val work: String,
    val collection: String,
    val title: String,
    val citation: String,
    val text: String,
    val translation: String,
    val sanskrit: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val themes: List<String>,
    val score: Double,
)
