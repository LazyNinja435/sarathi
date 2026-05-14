package com.sarathi.app.update

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class UpToDate(val versionName: String) : UpdateUiState()
    data class UpdateAvailable(
        val versionName: String,
        val apkSizeBytes: Long,
        val manifest: ReleaseManifest,
    ) : UpdateUiState()
    data object Downloading : UpdateUiState()
    data object ReadyToInstall : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}
