package com.sarathi.app.modeldownload

sealed class ModelDownloadUiState {
    data object Idle : ModelDownloadUiState()
    data object FetchingManifest : ModelDownloadUiState()
    data object Downloading : ModelDownloadUiState()
    data class Progress(val label: String, val downloadedBytes: Long, val totalBytes: Long) : ModelDownloadUiState()
    data object Verifying : ModelDownloadUiState()
    data object Installed : ModelDownloadUiState()
    data class Error(val message: String) : ModelDownloadUiState()
}
