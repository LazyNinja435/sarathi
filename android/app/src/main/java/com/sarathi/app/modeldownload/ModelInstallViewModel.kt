package com.sarathi.app.modeldownload

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.app.update.GithubReleaseClient
import com.sarathi.app.update.ManifestCache
import com.sarathi.app.update.ReleaseManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelInstallViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = ModelDownloadManager(application)

    private val _ui = MutableStateFlow<ModelDownloadUiState>(ModelDownloadUiState.Idle)
    val ui: StateFlow<ModelDownloadUiState> = _ui.asStateFlow()

    private val _lastManifestUrl = MutableStateFlow(GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL)
    val lastManifestUrl: StateFlow<String> = _lastManifestUrl.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun prefetchManifest(manifestUrl: String = GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL) {
        viewModelScope.launch {
            _lastError.value = null
            _lastManifestUrl.value = manifestUrl
            _ui.value = ModelDownloadUiState.FetchingManifest
            runCatching {
                val text = GithubReleaseClient.downloadText(manifestUrl)
                val parsed = ReleaseManifest.parse(text)
                if (parsed.app != null) {
                    ManifestCache.save(getApplication(), text)
                }
                _ui.value = ModelDownloadUiState.Idle
            }.onFailure { e ->
                _lastError.value = e.message ?: "Could not load the release manifest."
                _ui.value = ModelDownloadUiState.Error(_lastError.value!!)
            }
        }
    }

    fun startDownload(
        manifestUrl: String = GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL,
        action: ModelDownloadAction = ModelDownloadAction.INSTALL_MODEL,
    ) {
        viewModelScope.launch {
            if (_ui.value is ModelDownloadUiState.Error) {
                _ui.value = ModelDownloadUiState.Idle
            }
            _lastError.value = null
            _lastManifestUrl.value = manifestUrl
            _ui.value = ModelDownloadUiState.FetchingManifest
            runCatching {
                val manifest = manager.resolveManifestForModelDownload(manifestUrl)
                when (action) {
                    ModelDownloadAction.VERIFY_MODEL -> {
                        _ui.value = ModelDownloadUiState.Verifying
                        manager.verifyInstalledModel(manifest) { label ->
                            _ui.value = ModelDownloadUiState.Progress(label, 0L, 1L)
                        }
                        _ui.value = ModelDownloadUiState.Installed
                    }
                    ModelDownloadAction.KEEP_EXISTING_MODEL -> {
                        _ui.value = ModelDownloadUiState.Idle
                    }
                    ModelDownloadAction.INSTALL_MODEL,
                    ModelDownloadAction.UPDATE_MODEL,
                    -> {
                        _ui.value = ModelDownloadUiState.Downloading
                        manager.downloadAndInstall(manifest, action) { downloaded, total, label ->
                            _ui.value = ModelDownloadUiState.Progress(label, downloaded, total)
                        }
                        _ui.value = ModelDownloadUiState.Installed
                    }
                }
            }.onFailure { e ->
                _lastError.value = e.message ?: "Model download failed."
                _ui.value = ModelDownloadUiState.Error(_lastError.value!!)
            }
        }
    }

    fun acknowledgeInstalled() {
        if (_ui.value is ModelDownloadUiState.Installed) {
            _ui.value = ModelDownloadUiState.Idle
        }
    }

    fun resetAfterError() {
        if (_ui.value is ModelDownloadUiState.Error) {
            _ui.value = ModelDownloadUiState.Idle
        }
    }
}
