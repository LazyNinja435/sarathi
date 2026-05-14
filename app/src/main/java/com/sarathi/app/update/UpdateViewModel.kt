package com.sarathi.app.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = UpdateDownloadManager(application)

    private val _ui = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val ui: StateFlow<UpdateUiState> = _ui.asStateFlow()

    private val _lastManifestUrl = MutableStateFlow(GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL)
    val lastManifestUrl: StateFlow<String> = _lastManifestUrl.asStateFlow()

    private val _lastDownloadedApkSha = MutableStateFlow<String?>(null)
    val lastDownloadedApkSha: StateFlow<String?> = _lastDownloadedApkSha.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var cachedManifest: ReleaseManifest? = null

    fun checkForUpdates(manifestUrl: String = GithubReleaseClient.DEFAULT_LATEST_MANIFEST_URL) {
        viewModelScope.launch {
            if (_ui.value is UpdateUiState.Error) {
                _ui.value = UpdateUiState.Idle
            }
            _lastError.value = null
            _lastManifestUrl.value = manifestUrl
            _ui.value = UpdateUiState.Checking
            runCatching {
                val text = GithubReleaseClient.downloadText(manifestUrl)
                val manifest = ReleaseManifest.parse(text)
                when (val cmp = AppUpdateManager.compareWithManifest(manifest)) {
                    is AppUpdateManager.UpdateComparison.UpToDate -> {
                        cachedManifest = manifest
                        _ui.value = UpdateUiState.UpToDate(cmp.versionName)
                    }
                    is AppUpdateManager.UpdateComparison.UpdateAvailable -> {
                        cachedManifest = cmp.manifest
                        _ui.value = UpdateUiState.UpdateAvailable(
                            versionName = cmp.versionName,
                            apkSizeBytes = cmp.apkSizeBytes,
                            manifest = cmp.manifest,
                        )
                    }
                }
            }.onFailure { e ->
                _lastError.value = e.message ?: "Update check failed."
                _ui.value = UpdateUiState.Error(_lastError.value!!)
            }
        }
    }

    fun downloadUpdate() {
        val manifest = cachedManifest ?: return
        viewModelScope.launch {
            _lastError.value = null
            _ui.value = UpdateUiState.Downloading
            runCatching {
                downloadManager.downloadApk(manifest)
                val apk = downloadManager.updateApkFile()
                _lastDownloadedApkSha.value = Sha256Util.sha256Hex(apk)
                _ui.value = UpdateUiState.ReadyToInstall
            }.onFailure { e ->
                _lastDownloadedApkSha.value = null
                _lastError.value = e.message ?: "Download failed."
                _ui.value = UpdateUiState.Error(_lastError.value!!)
            }
        }
    }

    fun resetAfterError() {
        if (_ui.value is UpdateUiState.Error) {
            _ui.value = UpdateUiState.Idle
        }
    }

    fun pendingUpdateApkFile(): File = downloadManager.updateApkFile()
}
